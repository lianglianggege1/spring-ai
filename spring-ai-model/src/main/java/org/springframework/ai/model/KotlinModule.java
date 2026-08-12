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

package org.springframework.ai.model;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import org.springframework.core.KotlinDetector;

/**
 * 【中文说明】KotlinModule 是 victools JSON Schema 生成器的<b>扩展模块</b>，
 * 用于让 Spring AI 在为 <b>Kotlin 类</b>生成工具（Tool/Function Calling）的 JSON Schema 时，
 * 能正确识别 Kotlin 特有的语言特性。
 *
 * <p>
 * 要解决的问题：Kotlin 编译成字节码后，很多语言级信息（可空性 {@code String?}、
 * 构造参数默认值、属性真实名称）在纯 Java 反射下会丢失或失真。若不做处理，
 * 生成的 Schema 会把可空字段标成必填、把有默认值的参数也标成 required，
 * 导致大模型调用工具时传参出错。
 *
 * <p>
 * 实现思路：通过 {@code kotlin-reflect} 读取 Kotlin 元数据，覆写 victools 的四类判定逻辑：
 * <ul>
 * <li>可空性判定 —— 依据 {@code KType.isMarkedNullable}；</li>
 * <li>属性名解析 —— 使用 Kotlin 属性名而非 Java 字段名；</li>
 * <li>必填判定 —— "非空 且 是主构造器中无默认值的参数"才算必填；</li>
 * <li>忽略判定 —— 跳过编译器生成的合成成员。</li>
 * </ul>
 *
 * <p>
 * 典型用法：由 Spring AI 的 JSON Schema 生成工具在检测到 Kotlin 环境时自动注册，
 * 业务代码一般无需直接使用本类。
 */
public class KotlinModule implements Module {

	// 【中文】模块入口：victools 在构建 Schema 生成配置时回调本方法，
	// 我们在此把自定义判定逻辑挂载到配置构建器上。
	@Override
	public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
		// 只针对"字段"应用自定义规则
		SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
		// SchemaGeneratorConfigPart<MethodScope> methodConfigPart = builder.forMethods();

		this.applyToConfigBuilderPart(fieldConfigPart);
		// 【中文】注意：针对"方法"（getter）的处理被刻意注释掉了。
		// 原因是 Kotlin 属性最终落到字段上即可完整表达，重复处理 getter 反而可能产生重复项。
		// this.applyToConfigBuilderPart(methodConfigPart);
	}

	// 【中文】把四类判定回调统一注册到配置分区上。
	// 参数用通配符 SchemaGeneratorConfigPart<?>，是为了让同一段逻辑既能作用于字段也能作用于方法
	// （虽然目前只启用了字段），体现了复用意图。
	private void applyToConfigBuilderPart(SchemaGeneratorConfigPart<?> configPart) {
		// 可空性判定：决定 Schema 中该属性是否允许 null
		configPart.withNullableCheck(this::isNullable);
		// 属性名覆盖：用 Kotlin 侧的属性名替换 Java 字段名
		configPart.withPropertyNameOverrideResolver(this::getPropertyName);
		// 必填判定：决定该属性是否进入 Schema 的 required 列表
		configPart.withRequiredCheck(this::isRequired);
		// 忽略判定：决定该成员是否完全不出现在 Schema 中
		configPart.withIgnoreCheck(this::shouldIgnore);
	}

	// 【中文】判断成员是否可空。
	// 返回值是包装类型 Boolean 且可为 null，这是 victools 的约定：
	// 返回 null 表示"本模块不表态"，交由默认逻辑或其他模块决定；
	// 只有确认这是 Kotlin 属性时才依据 isMarkedNullable() 给出 true/false。
	private @Nullable Boolean isNullable(MemberScope<?, ?> member) {
		KProperty<?> kotlinProperty = getKotlinProperty(member);
		if (kotlinProperty != null) {
			// Kotlin 的 String? 会被标记为 marked nullable，这是 Java 反射拿不到的信息
			return kotlinProperty.getReturnType().isMarkedNullable();
		}
		// 非 Kotlin 类型：不干预，返回 null 表示放弃判定
		return null;
	}

	// 【中文】解析属性名。同样地，返回 null 表示"不覆盖"，沿用 victools 默认的字段名。
	// 对 Kotlin 类则返回其属性名，确保生成的 Schema 字段名与 Kotlin 源码一致。
	private @Nullable String getPropertyName(MemberScope<?, ?> member) {
		KProperty<?> kotlinProperty = getKotlinProperty(member);
		if (kotlinProperty != null) {
			return kotlinProperty.getName();
		}
		return null;
	}

	// 【中文】判断属性是否为"必填"（进入 JSON Schema 的 required 列表）。
	// 这是本类最关键的逻辑：必须<b>同时</b>满足两个条件才算必填。
	private boolean isRequired(MemberScope<?, ?> member) {
		KProperty<?> kotlinProperty = getKotlinProperty(member);
		if (kotlinProperty != null) {
			KType returnType = kotlinProperty.getReturnType();
			// 条件一：类型非空（即不是 String? 这种可空类型）
			boolean isNonNullable = !returnType.isMarkedNullable();

			// 取得声明该属性的 Java 类，再映射为 Kotlin 类以读取 Kotlin 元数据
			Class<?> declaringClass = member.getDeclaringType().getErasedType();
			KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(declaringClass);

			// 收集主构造器中"没有默认值"的参数名集合
			Set<String> constructorParamsWithoutDefault = getConstructorParametersWithoutDefault(kotlinClass);

			// 条件二：该属性出现在主构造器且没有默认值。
			// 若参数有默认值（如 val n: Int = 3），调用方可以不传，因此不应标为 required
			boolean isInConstructor = constructorParamsWithoutDefault.contains(kotlinProperty.getName());

			// 两个条件同时成立才是真正的必填项
			return isNonNullable && isInConstructor;
		}

		// 非 Kotlin 属性：本模块一律返回 false（不主动标记为必填）
		return false;
	}

	// 【中文】判断是否忽略该成员。
	// isSynthetic() 为 true 表示这是编译器生成的合成成员（如 Kotlin 的 $$delegatedProperties、
	// lambda 捕获字段、companion 相关字段等），它们不属于业务数据，
	// 必须排除，否则会污染生成的 Schema。
	private boolean shouldIgnore(MemberScope<?, ?> member) {
		return member.getRawMember().isSynthetic(); // Ignore generated properties/methods
	}

	// 【中文】核心辅助方法：把 victools 的 Java 成员反向映射回对应的 Kotlin 属性；
	// 若该成员不属于 Kotlin 类或找不到对应属性，则返回 null。
	// @NullUnmarked 注解用于在包级 @NullMarked 环境下豁免本方法的空安全检查
	// （注释中的链接说明这是为绕开 JDK 的一个已知问题）。
	@NullUnmarked // https://github.com/openjdk/jdk/pull/28018
	private KProperty<?> getKotlinProperty(MemberScope<?, ?> member) {
		Class<?> declaringClass = member.getDeclaringType().getErasedType();
		// 先用 Spring 的 KotlinDetector 快速判断是否为 Kotlin 类，
		// 避免对普通 Java 类做无谓的 kotlin-reflect 调用（kotlin-reflect 开销较大）
		if (KotlinDetector.isKotlinType(declaringClass)) {
			KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(declaringClass);
			// 遍历所有 Kotlin 成员属性，通过"其底层 Java 字段是否与当前成员相同"来配对
			for (KProperty<?> prop : KClasses.getMemberProperties(kotlinClass)) {
				Field javaField = ReflectJvmMapping.getJavaField(prop);
				// 空值处理：并非所有 Kotlin 属性都有底层字段（例如只有 getter 的计算属性），
				// 因此必须先判空再比较
				if (javaField != null && javaField.equals(member.getRawMember())) {
					return prop;
				}
			}
		}
		// 非 Kotlin 类或未匹配到属性
		return null;
	}

	// 【中文】收集 Kotlin 主构造器中"没有默认值"的参数名集合，供 isRequired 使用。
	private Set<String> getConstructorParametersWithoutDefault(KClass<?> kotlinClass) {
		Set<String> paramsWithoutDefault = new HashSet<>();
		KFunction<?> primaryConstructor = KClasses.getPrimaryConstructor(kotlinClass);
		// 空值处理：Java 类或没有主构造器的 Kotlin 类会返回 null，此时直接返回空集合
		if (primaryConstructor != null) {
			primaryConstructor.getParameters().forEach(param -> {
				// 两重过滤：
				// 1) Kind.INSTANCE 表示接收者/this 参数，不是真正的业务入参，需排除；
				// 2) isOptional() 为 true 表示该参数有默认值，可省略，不算必填。
				if (param.getKind() != KParameter.Kind.INSTANCE && !param.isOptional()) {
					String name = param.getName();
					// 空值处理：某些情况下参数名可能不可用（未保留调试信息），跳过
					if (name != null) {
						paramsWithoutDefault.add(name);
					}
				}
			});
		}

		return paramsWithoutDefault;
	}

}
