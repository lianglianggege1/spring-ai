/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.aot;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Utility methods for creating native runtime hints. See other modules for their
 * respective native runtime hints.
 *
 * @author Josh Long
 * @author Christian Tzolov
 * @author Mark Pollack
 * @author Fu Jian
 */
/**
 * 【中文说明】GraalVM 原生镜像（Native Image）运行时提示的通用工具类。
 *
 * <p>
 * 背景：原生镜像在编译期会做静态分析并裁剪掉“看起来没被用到”的类，反射、资源加载等动态行为无法被
 * 自动发现，必须通过 RuntimeHints 显式登记。本类提供一组扫描工具，批量找出需要登记的类型，
 * 避免逐个手写。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #findJsonAnnotatedClassesInPackage(String)} /
 * {@link #findJsonAnnotatedClassesInPackage(Class)}：扫描包下所有带 Jackson 注解的类
 * （这些类需要反射序列化/反序列化）。</li>
 * <li>{@link #findClassesInPackage(String, TypeFilter)}：按自定义过滤器扫描包。</li>
 * <li>{@link #findInnerClassesFor(Class)}：递归查找某个类的所有嵌套类。</li>
 * </ul>
 *
 * <p>
 * 设计要点：声明为 {@code abstract class} 而非 final 工具类，是 Spring 生态中常见的“不可实例化的
 * 静态方法容器”写法（无公开构造器可用）。各厂商模块的 RuntimeHintsRegistrar 会调用本类方法。
 *
 * <p>
 * 典型用法：{@code hints.reflection().registerType(...)} 配合
 * {@code AiRuntimeHints.findJsonAnnotatedClassesInPackage(SomeApi.class)}。
 *
 * @author Josh Long
 * @author Christian Tzolov
 * @author Mark Pollack
 * @author Fu Jian
 */
public abstract class AiRuntimeHints {

	// 日志：使用 commons-logging 抽象，便于在扫描时输出被登记的类型（debug 级别）
	private static final Log log = LogFactory.getLog(AiRuntimeHints.class);

	/**
	 * Finds classes in a package that are annotated with JsonInclude or have Jackson
	 * annotations.
	 * @param packageName The name of the package to search for annotated classes.
	 * @return A set of TypeReference objects representing the annotated classes found.
	 */
	// 扫描指定包，找出所有需要为 Jackson 反射登记的类型
	public static Set<TypeReference> findJsonAnnotatedClassesInPackage(String packageName) {
		// 第一重判定：类上直接标注了 @JsonInclude（基于字节码元数据，无需加载类）
		var annotationTypeFilter = new AnnotationTypeFilter(JsonInclude.class);
		// 自定义过滤器：两个条件满足其一即入选
		TypeFilter typeFilter = (metadataReader, metadataReaderFactory) -> {
			try {
				// 注意：这里会真正加载类，因为第二重判定需要用反射检查方法/构造器/字段上的注解
				var clazz = Class.forName(metadataReader.getClassMetadata().getClassName());
				// 条件一：类级 @JsonInclude；条件二：自身或其嵌套类中存在 Jackson 注解
				return annotationTypeFilter.match(metadataReader, metadataReaderFactory)
						|| !discoverJacksonAnnotatedTypesFromRootType(clazz).isEmpty();
			}
			catch (ClassNotFoundException e) {
				// 扫描到但加载不到的类视为环境异常，直接包装成非受检异常向上抛出（快速失败）
				throw new RuntimeException(e);
			}
		};

		return findClassesInPackage(packageName, typeFilter);
	}

	/**
	 * Finds classes in a package that are annotated with JsonInclude or have Jackson
	 * annotations.
	 * @param packageClass The class in the package to search for annotated classes.
	 * @return A set of TypeReference objects representing the annotated classes found.
	 */
	// 重载便捷版：传入包内任意一个类作为“定位锚点”，自动取其所在包名，避免硬编码包名字符串
	public static Set<TypeReference> findJsonAnnotatedClassesInPackage(Class<?> packageClass) {
		return findJsonAnnotatedClassesInPackage(packageClass.getPackageName());
	}

	/**
	 * Finds all classes in the specified package that match the given type filter.
	 * @param packageName The name of the package to scan for classes.
	 * @param typeFilter The type filter used to filter the scanned classes.
	 * @return A set of TypeReference objects representing the found classes.
	 */
	// 通用包扫描：复用 Spring 的类路径扫描器，按过滤器收集类型引用
	public static Set<TypeReference> findClassesInPackage(String packageName, TypeFilter typeFilter) {
		// 构造参数 false 表示“不使用默认过滤器”，即不自动包含 @Component 等注解，
		// 完全由下面 addIncludeFilter 传入的自定义规则决定
		var classPathScanningCandidateComponentProvider = new ClassPathScanningCandidateComponentProvider(false);
		classPathScanningCandidateComponentProvider.addIncludeFilter(typeFilter);
		return classPathScanningCandidateComponentProvider//
			.findCandidateComponents(packageName)//
			.stream()//
			// BeanDefinition -> TypeReference；requireNonNull 保证类名一定存在，否则抛 NPE
			.map(bd -> TypeReference.of(Objects.requireNonNull(bd.getBeanClassName())))//
			// peek 仅用于调试日志，不改变流内容；先判 isDebugEnabled 可避免无谓的字符串拼接开销
			.peek(tr -> {
				if (log.isDebugEnabled()) {
					log.debug("registering [" + tr.getName() + "]");
				}
			})
			// 收集为不可变 Set，天然去重且防止调用方误改
			.collect(Collectors.toUnmodifiableSet());
	}

	// 私有辅助：全方位检查一个类是否“沾染”了 Jackson 注解，
	// 检查范围覆盖 类 / 方法 / 构造器 / 方法参数 / record 组件 / 字段
	private static boolean hasJacksonAnnotations(Class<?> type) {
		var annotationsToFind = Set.of(JsonProperty.class, JsonInclude.class);
		for (var annotationToFind : annotationsToFind) {

			// 1) 类型声明上的注解
			if (type.isAnnotationPresent(annotationToFind)) {
				return true;
			}

			// Executable 是 Method 与 Constructor 的公共父类，便于统一遍历；
			// 用 HashSet 承载可自动去重（getConstructors 与 getDeclaredConstructors 会有重叠）
			var executables = new HashSet<Executable>();
			executables.addAll(Set.of(type.getMethods()));
			executables.addAll(Set.of(type.getConstructors()));
			executables.addAll(Set.of(type.getDeclaredConstructors()));

			for (var executable : executables) {
				// 2) 方法/构造器本身的注解
				if (executable.isAnnotationPresent(annotationToFind)) {
					return true;
				}

				// 3) 参数上的注解，常见于 record 或不可变类的 @JsonProperty 反序列化构造器
				for (var p : executable.getParameters()) {
					if (p.isAnnotationPresent(annotationToFind)) {
						return true;
					}
				}
			}

			// 4) record 组件上的注解；非 record 类型时 getRecordComponents() 返回 null，故先判空
			if (type.getRecordComponents() != null) {
				for (var r : type.getRecordComponents()) {
					if (r.isAnnotationPresent(annotationToFind)) {
						return true;
					}
				}
			}

			// 5) public 字段上的注解（getFields 只返回公开字段，含继承而来的）
			for (var f : type.getFields()) {
				if (f.isAnnotationPresent(annotationToFind)) {
					return true;
				}
			}
		}

		// 所有位置都没找到，判定为与 Jackson 无关
		return false;
	}

	// 私有辅助：以给定类为根，连同它的所有“嵌套成员”一起检查，返回其中带 Jackson 注解的类集合
	private static Set<Class<?>> discoverJacksonAnnotatedTypesFromRootType(Class<?> type) {
		var jsonTypes = new HashSet<Class<?>>();
		var classesToInspect = new HashSet<Class<?>>();
		classesToInspect.add(type);
		// getNestMembers 返回同一 nest（外部类及其所有嵌套类）的全部成员，
		// 这样内部 DTO/Builder 等也能被一并发现
		classesToInspect.addAll(Arrays.asList(type.getNestMembers()));
		for (var n : classesToInspect) {
			if (hasJacksonAnnotations(n)) {
				jsonTypes.add(n);
			}
		}
		return jsonTypes;
	}

	/**
	 * Discovers all inner classes of a given class.
	 * <p>
	 * This method recursively finds all nested classes (both declared and inherited) of
	 * the provided class and converts them to type references.
	 * @param clazz the class to find inner classes for
	 * @return a set of type references for all discovered inner classes
	 */
	// 查找某个类的全部内部类（含多层嵌套），转换为 TypeReference 供反射登记
	public static Set<TypeReference> findInnerClassesFor(Class<?> clazz) {
		// 用 Set<String> 收集类名可天然去重，避免同一内部类被重复登记
		var indent = new HashSet<String>();
		// 递归收集，结果通过参数（可变集合）带出，而非返回值
		findNestedClasses(clazz, indent);
		return indent.stream().map(TypeReference::of).collect(Collectors.toSet());
	}

	/**
	 * Recursively finds all nested classes of a given class.
	 * <p>
	 * This method:
	 * <ol>
	 * <li>Collects both declared and inherited nested classes</li>
	 * <li>Recursively processes each nested class</li>
	 * <li>Adds the class names to the provided set</li>
	 * </ol>
	 * @param clazz the class to find nested classes for
	 * @param indent the set to collect class names in
	 */
	// 私有递归方法：深度优先遍历，把每一层的嵌套类名累积到 indent 集合中
	private static void findNestedClasses(Class<?> clazz, Set<String> indent) {
		var classes = new ArrayList<Class<?>>();
		// getDeclaredClasses：本类声明的所有嵌套类（含 private）
		classes.addAll(Arrays.asList(clazz.getDeclaredClasses()));
		// getClasses：本类及父类中的 public 嵌套类；两者合并可覆盖“声明的 + 继承的”，
		// 用 ArrayList 会产生重复，但最终由外层 HashSet 去重
		classes.addAll(Arrays.asList(clazz.getClasses()));
		// 递归下钻：处理内部类的内部类。终止条件是某层不再有嵌套类，classes 为空则循环不执行
		for (var nestedClass : classes) {
			findNestedClasses(nestedClass, indent);
		}
		// 递归返回后再收集本层结果
		indent.addAll(classes.stream().map(Class::getName).toList());
	}

}
