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

package org.springframework.ai.tool.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from
 * {@link Tool}-annotated methods.
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code MethodToolCallbackProvider}：扫描一批「工具对象」，把其中所有带
 * {@link Tool} 注解的方法批量转换成 {@link ToolCallback} 的提供者。
 *
 * <p>
 * <b>定位：</b>这是 {@code @Tool} 声明式方案的「入口/装配器」。
 * 你只需把普通业务 Bean 丢进来，它负责完成「反射扫描 → 过滤 → 逐个构建 MethodToolCallback → 重名校验」。
 * </p>
 *
 * <p>
 * <b>关键字段：</b>{@code toolObjects} —— 待扫描的对象列表（通常是 Spring Bean 实例）。
 * </p>
 *
 * <p>
 * <b>三层过滤规则（见 {@link #getToolCallbacks()}）：</b>
 * </p>
 * <ol>
 * <li>必须带 {@code @Tool} 注解</li>
 * <li>返回值不能是 {@code Function}/{@code Supplier}/{@code Consumer} 等函数式类型（会打 warn 并忽略）</li>
 * <li>必须是「用户声明的方法」，排除编译器合成方法与 {@code Object} 的方法</li>
 * </ol>
 *
 * <p>
 * <b>两处重要校验（fail-fast）：</b>构造时若某个对象里一个 {@code @Tool} 方法都没有，直接报错并提示
 * 是否该改用 {@code .toolCallbacks(...)}；工具名重复也会直接抛异常。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
 *         .toolObjects(new WeatherTools(), new DateTimeTools())
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 * @see MethodToolCallback
 */
public final class MethodToolCallbackProvider implements ToolCallbackProvider {

	private static final Log logger = LogFactory.getLog(MethodToolCallbackProvider.class);

	// 中文：待扫描的工具对象列表（通常是 Spring 容器中的 Bean）。
	private final List<Object> toolObjects;

	// 中文：私有构造器。注意它在构造阶段就执行了两轮校验，属于典型的 fail-fast 设计 ——
	// 配置错误在应用启动时暴露，而不是等到运行期真正调用工具时才发现。
	private MethodToolCallbackProvider(List<Object> toolObjects) {
		Assert.notNull(toolObjects, "toolObjects cannot be null");
		Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
		// 中文：校验一 —— 每个对象里至少要有一个 @Tool 方法。
		assertToolAnnotatedMethodsPresent(toolObjects);
		this.toolObjects = toolObjects;
		// 中文：校验二 —— 提前构建一次全部回调，检查是否存在工具重名。
		// 这里会有一次"额外构建"的开销，是用性能换取尽早发现配置错误。
		validateToolCallbacks(getToolCallbacks());
	}

	// 中文：确保每个传入对象都至少包含一个 @Tool 注解方法。
	private void assertToolAnnotatedMethodsPresent(List<Object> toolObjects) {

		for (Object toolObject : toolObjects) {
			List<Method> toolMethods = Stream
				// 中文：关键 —— 若对象是 Spring AOP 代理（如被 @Transactional 增强），
				// 必须取"目标类"来扫描注解，因为代理类上通常读不到原始方法的注解。
				.of(ReflectionUtils.getDeclaredMethods(
						AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
				.filter(this::isToolAnnotatedMethod)
				.filter(toolMethod -> !isFunctionalType(toolMethod))
				.toList();

			if (toolMethods.isEmpty()) {
				// 中文：这个错误信息很贴心 —— 用户最常见的误用就是把 ToolCallback 对象
				// 误传给了 .tools(Object...)，这里直接提示应改用 .toolCallbacks(...)。
				throw new IllegalArgumentException("No @Tool annotated methods found in " + toolObject + ". "
						+ "Did you mean to pass a ToolCallback or ToolCallbackProvider? If so, use"
						+ " .tools(toolCallback) or .toolCallbacks(toolCallback) instead.");
			}
		}
	}

	// 中文：核心方法 —— 扫描所有工具对象，生成 ToolCallback 数组。
	@Override
	public ToolCallback[] getToolCallbacks() {
		var toolCallbacks = this.toolObjects.stream()
			.map(toolObject -> Stream
				// 中文：同样做 AOP 代理解包，取真实目标类。
				.of(ReflectionUtils.getDeclaredMethods(
						AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
				// 中文：过滤一 —— 只保留带 @Tool 注解的方法。
				.filter(this::isToolAnnotatedMethod)
				// 中文：过滤二 —— 排除返回函数式类型的方法（不受支持，仅打 warn 忽略）。
				.filter(toolMethod -> !isFunctionalType(toolMethod))
				// 中文：过滤三 —— USER_DECLARED_METHODS 会排除桥接方法、编译器合成方法
				// 以及 Object 类声明的方法（如 equals/hashCode），避免把它们误当作工具。
				.filter(ReflectionUtils.USER_DECLARED_METHODS::matches)
				// 中文：逐个方法组装 MethodToolCallback —— 定义、元数据、方法、实例、结果转换器全部自动解析。
				.map(toolMethod -> MethodToolCallback.builder()
					.toolDefinition(ToolDefinitions.from(toolMethod))
					.toolMetadata(ToolMetadata.from(toolMethod))
					.toolMethod(toolMethod)
					.toolObject(toolObject)
					.toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod))
					.build())
				.toArray(ToolCallback[]::new))
			// 中文：上一步每个对象产出一个数组，这里用 flatMap 压平成单层流。
			.flatMap(Stream::of)
			.toArray(ToolCallback[]::new);

		// 中文：再次做重名校验（本方法可被外部单独调用，因此不能只依赖构造时的那次校验）。
		validateToolCallbacks(toolCallbacks);

		return toolCallbacks;
	}

	// 中文：判断方法返回值是否为函数式类型（Function/Supplier/Consumer）。
	// 这类方法通常是"返回一个函数"而非"本身就是工具"，语义容易混淆，故不支持并给出告警。
	private boolean isFunctionalType(Method toolMethod) {
		var isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType())
				|| ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
				|| ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());

		if (isFunction) {
			// 中文：只告警不抛异常 —— 该方法会被静默跳过，不影响同一个类里的其他工具方法。
			if (logger.isWarnEnabled()) {
				logger.warn("Method " + toolMethod.getName() + "is annotated with @Tool but returns a functional type. "
						+ "This is not supported and the method will be ignored.");
			}
		}

		return isFunction;
	}

	// 中文：判断方法上是否存在 @Tool 注解。
	// AnnotationUtils.findAnnotation 会沿着父类/接口/元注解向上查找，比 getAnnotation 更全面。
	private boolean isToolAnnotatedMethod(Method method) {
		Tool annotation = AnnotationUtils.findAnnotation(method, Tool.class);
		return Objects.nonNull(annotation);
	}

	// 中文：校验工具名是否重复。工具名必须全局唯一，否则模型回传名称时无法确定该调用哪个。
	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			// 中文：错误信息同时列出"重复的工具名"和"涉及的来源类名"，便于快速定位冲突位置。
			throw new IllegalArgumentException("Multiple tools with the same name (%s) found in sources: %s".formatted(
					String.join(", ", duplicateToolNames),
					this.toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
		}
	}

	// 中文：获取 Builder 实例的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@code MethodToolCallbackProvider} 的构建器。
	 *
	 * <p>
	 * 只有 {@code toolObjects} 一个可配置项。注意 {@code build()} 时会触发扫描与校验，
	 * 若某个对象没有 {@code @Tool} 方法或存在工具重名，会立即抛出异常。
	 * </p>
	 */
	public static final class Builder {

		// 中文：初始化为空列表而非 null，避免用户不调用 toolObjects(...) 时出现 NPE
		// （此时 build() 会得到一个空的 provider）。
		private List<Object> toolObjects = new ArrayList<>();

		// 中文：私有构造器，只能通过 MethodToolCallbackProvider.builder() 创建。
		private Builder() {
		}

		// 中文：设置待扫描的工具对象（可变参数）。
		// 注意语义是"替换"而非"追加"——重复调用会覆盖之前设置的值。
		public Builder toolObjects(Object... toolObjects) {
			Assert.notNull(toolObjects, "toolObjects cannot be null");
			this.toolObjects = Arrays.asList(toolObjects);
			return this;
		}

		// 中文：完成构建。真正的扫描与校验都发生在构造器里。
		public MethodToolCallbackProvider build() {
			return new MethodToolCallbackProvider(this.toolObjects);
		}

	}

}
