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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * A {@link ToolCallback} implementation to invoke methods as tools.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code MethodToolCallback}：通过<b>反射调用 Java 方法</b>来执行工具的
 * {@link ToolCallback} 实现，是 {@code @Tool} 注解方案背后的执行引擎。
 *
 * <p>
 * <b>与 {@code FunctionToolCallback} 的区别：</b>后者包装的是 Lambda / 函数式接口；
 * 本类包装的是「对象 + 方法」组合，通过 {@code Method.invoke} 执行，因此需要额外处理
 * 参数绑定、可见性、反射异常等问题。
 * </p>
 *
 * <p>
 * <b>关键字段：</b>
 * </p>
 * <ul>
 * <li>{@code toolMethod} —— 要反射调用的目标方法</li>
 * <li>{@code toolObject} —— 方法所属实例。<b>静态方法时可为 null</b>，实例方法时必填</li>
 * <li>{@code toolDefinition} / {@code toolMetadata} / {@code toolCallResultConverter} —— 同函数式实现</li>
 * </ul>
 *
 * <p>
 * <b>执行流程（{@link #call(String, ToolContext)}）：</b>
 * </p>
 * <ol>
 * <li>校验方法是否需要 {@code ToolContext} 而调用方未提供</li>
 * <li>把模型给的 JSON 解析成 {@code Map<参数名, 值>}</li>
 * <li>按方法签名逐个参数做类型转换，拼成 {@code Object[]}（{@code ToolContext} 参数特殊注入）</li>
 * <li>反射 {@code invoke} 执行</li>
 * <li>用转换器把返回值转成字符串</li>
 * </ol>
 *
 * <p>
 * <b>注意：</b>第 3 步依赖 {@code parameter.getName()} 获取参数名，
 * 因此编译时需开启 {@code -parameters} 选项，否则参数名会变成 arg0/arg1 导致绑定失败。
 * </p>
 *
 * <p>
 * 类声明为 {@code final}，不允许继承，保证反射执行逻辑的行为一致性。
 * </p>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see MethodToolCallbackProvider
 * @see org.springframework.ai.tool.function.FunctionToolCallback
 */
public final class MethodToolCallback implements ToolCallback {

	// 中文：JSON 工具，static final 复用单例。
	private static final JsonHelper jsonHelper = new JsonHelper();

	private static final Log logger = LogFactory.getLog(MethodToolCallback.class);

	// 中文：默认结果转换器（JSON 序列化）。
	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	// 中文：默认元数据（returnDirect = false），不可变故可安全共享。
	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	// 中文：工具说明书，发送给模型。
	private final ToolDefinition toolDefinition;

	// 中文：执行期元数据，仅框架内部使用。
	private final ToolMetadata toolMetadata;

	// 中文：目标方法的反射对象。
	private final Method toolMethod;

	// 中文：方法所属的对象实例。静态方法时为 null（反射 invoke 静态方法时首参传 null 即可）。
	private final @Nullable Object toolObject;

	// 中文：返回值 -> 字符串的转换器。
	private final ToolCallResultConverter toolCallResultConverter;

	// 中文：构造器。toolMetadata 与 toolCallResultConverter 可为 null，内部会兜底成默认值。
	public MethodToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod,
			@Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolMethod, "toolMethod cannot be null");
		// 中文：关键的"互斥式"校验 —— 静态方法不需要实例，非静态方法则必须提供实例，
		// 否则后续 Method.invoke 会抛 NullPointerException。这里提前拦截给出清晰错误信息。
		Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
				"toolObject cannot be null for non-static methods");
		this.toolDefinition = toolDefinition;
		// 中文：空值兜底 —— 未提供元数据时使用共享默认实例。
		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
		this.toolMethod = toolMethod;
		this.toolObject = toolObject;
		// 中文：空值兜底 —— 未提供转换器时使用默认 JSON 转换器。
		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
				: DEFAULT_RESULT_CONVERTER;
	}

	// 中文：返回工具说明书。
	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	// 中文：返回工具执行期元数据。
	@Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	// 中文：不带上下文的重载，委托给带上下文版本并传 null。
	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	// 中文：工具执行主流程。toolInput 为模型生成的 JSON 参数字符串。
	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		if (logger.isDebugEnabled()) {
			logger.debug("Starting execution of tool: " + this.toolDefinition.name());
		}

		// 中文：步骤 1 —— 校验：若方法签名声明了 ToolContext 参数，则调用方必须提供非空上下文。
		this.validateToolContextSupport(toolContext);

		// 中文：步骤 2 —— 把 JSON 解析成 Map<参数名, 参数值>。
		Map<String, Object> toolArguments = this.extractToolArguments(toolInput);
		Assert.state(toolArguments != null, "toolArguments must not be null");

		// 中文：步骤 3 —— 按方法签名顺序拼装实参数组，并逐个做类型转换。
		Object[] methodArguments = this.buildMethodArguments(toolArguments, toolContext);

		// 中文：步骤 4 —— 反射调用目标方法。
		Object result = this.callMethod(methodArguments);

		if (logger.isDebugEnabled()) {
			logger.debug("Successful execution of tool: " + this.toolDefinition.name());
		}

		// 中文：步骤 5 —— 取"泛型返回类型"（而非 getReturnType()），
		// 以便转换器能感知 List<Foo> 这类泛型信息，做更精确的序列化。
		Type returnType = this.toolMethod.getGenericReturnType();

		return this.toolCallResultConverter.convert(result, returnType);
	}

	// 中文：校验工具上下文的可用性。
	// 规则：如果方法签名里声明了 ToolContext 类型的参数，那么调用时就必须提供"非空且内容非空"的上下文，
	// 否则方法拿到 null 会在业务代码里报错，不如提前抛出清晰异常。
	// 反之，方法不需要上下文时，即便调用方传了上下文也没关系（会被忽略）。
	private void validateToolContextSupport(@Nullable ToolContext toolContext) {
		// 中文：注意"非空"的判定不仅要求对象非 null，还要求其内部 Map 非空。
		var isNonEmptyToolContextProvided = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
		// 中文：扫描方法参数类型，判断是否存在可接收 ToolContext 的参数。
		var isToolContextAcceptedByMethod = Stream.of(this.toolMethod.getParameterTypes())
			.anyMatch(type -> ClassUtils.isAssignable(ToolContext.class, type));
		if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
			throw new IllegalArgumentException("ToolContext is required by the method as an argument");
		}
	}

	// 中文：把模型返回的 JSON 字符串解析成 Map<参数名, 参数值>。
	private @Nullable Map<String, Object> extractToolArguments(String toolInput) {
		try {
			// 中文：这里用匿名内部类形式的 ParameterizedTypeReference<>，
			// 借助"匿名子类保留泛型签名"的技巧绕过类型擦除，目标类型由左侧返回值 Map<String, Object> 推断得出。
			return jsonHelper.fromJson(toolInput, new ParameterizedTypeReference<>() {
			});
		}
		catch (Exception ex) {
			logger.warn("Conversion from JSON failed", ex);
			// 中文：如果根因是 Jackson 的解析异常，就用它作为 cause（错误信息更贴近真实问题，
			// 例如"JSON 格式非法"），否则用原异常。这样反馈给模型的报错更有指导性，便于其自我修正。
			Throwable cause = (ex.getCause() instanceof JacksonException) ? ex.getCause() : ex;
			throw new ToolExecutionException(this.getToolDefinition(), cause);
		}
	}

	// Based on the implementation in MethodToolCallback.
	// 中文：按方法签名顺序构建实参数组。
	// 关键点：依赖 parameter.getName() 按"参数名"从 Map 中取值，
	// 因此编译时必须开启 -parameters 选项保留参数名，否则会变成 arg0/arg1 而匹配不上。
	@SuppressWarnings("null")
	private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
		return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
			// 中文：特殊分支 —— ToolContext 类型的参数不从 JSON 取值，而是由框架直接注入。
			if (parameter.getType().isAssignableFrom(ToolContext.class)) {
				return toolContext;
			}
			// 中文：普通参数按名字取原始值（可能为 null，表示模型没传这个可选参数）。
			Object rawArgument = toolInputArguments.get(parameter.getName());
			// 中文：用"泛型化参数类型"做转换，以支持 List<Foo> 这类签名。
			return buildTypedArgument(rawArgument, parameter.getParameterizedType());
		}).toArray();
	}

	// 中文：把 JSON 解析出的松散值（Map/List/String 等）转换成方法参数所需的强类型对象。
	private @Nullable Object buildTypedArgument(@Nullable Object value, Type type) {
		// 中文：空值处理 —— 模型没提供该可选参数时直接返回 null，交由业务方法自行处理。
		if (value == null) {
			return null;
		}
		try {
			// 中文：分支一 —— 普通非泛型类型，直接做对象转换，性能更好。
			if (type instanceof Class<?>) {
				return jsonHelper.convertToTypedObject(value, (Class<?>) type);
			}

			// For generic types, use the fromJson method that accepts Type
			// 中文：分支二 —— 泛型类型（如 List<Foo>）。
			// 采用"先序列化回 JSON 字符串、再按目标 Type 反序列化"的迂回方式，
			// 这样才能让 Jackson 正确还原泛型元素类型。
			String json = jsonHelper.toJson(value, true);
			return jsonHelper.fromJson(json, type);
		}
		catch (Exception ex) {
			logger.warn("Conversion from JSON failed", ex);
			// 中文：同上，优先取 Jackson 异常作为根因，让错误信息更精确。
			Throwable cause = (ex.getCause() instanceof JacksonException) ? ex.getCause() : ex;
			throw new ToolExecutionException(this.getToolDefinition(), cause);
		}
	}

	// 中文：真正执行反射调用。
	@SuppressWarnings("NullAway") // ex.getCause() is guaranteed to be non-null
	private @Nullable Object callMethod(Object[] methodArguments) {
		// 中文：可见性处理 —— 若"所属类非 public"或"方法非 public"，
		// 需要先 setAccessible(true) 突破访问限制，否则 invoke 会抛 IllegalAccessException。
		// 这让用户可以把工具方法写成包级私有或定义在非公开类中。
		if (isObjectNotPublic() || isMethodNotPublic()) {
			this.toolMethod.setAccessible(true);
		}

		Object result;
		try {
			// 中文：静态方法时 this.toolObject 为 null，这正是 Method.invoke 对静态方法的约定用法。
			result = this.toolMethod.invoke(this.toolObject, methodArguments);
		}
		catch (IllegalAccessException ex) {
			// 中文：访问权限问题属于"框架/配置层面的错误"，不该反馈给模型让它重试，
			// 因此抛 IllegalStateException 而非 ToolExecutionException。
			throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
		}
		catch (InvocationTargetException ex) {
			// 中文：关键区分点 —— InvocationTargetException 是反射对"业务方法内部抛出的异常"的包装。
			// 这里取 ex.getCause() 拿到真实业务异常，避免异常信息被反射层遮蔽。
			throw new ToolExecutionException(this.toolDefinition, ex.getCause());
		}
		return result;
	}

	// 中文：判断工具对象所属的类是否为非 public（静态方法时 toolObject 为 null，直接返回 false）。
	private boolean isObjectNotPublic() {
		return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
	}

	// 中文：判断工具方法本身是否为非 public。
	private boolean isMethodNotPublic() {
		return !Modifier.isPublic(this.toolMethod.getModifiers());
	}

	// 中文：调试用的字符串表示，只包含定义与元数据。
	@Override
	public String toString() {
		return "MethodToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
				+ '}';
	}

	// 中文：获取 Builder 实例的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@code MethodToolCallback} 的构建器。
	 *
	 * <p>
	 * <b>必填项：</b>{@code toolDefinition}、{@code toolMethod}。
	 * </p>
	 *
	 * <p>
	 * <b>可选项：</b>{@code toolMetadata}、{@code toolCallResultConverter}（缺省用默认实例）；
	 * {@code toolObject} —— <b>静态方法可不设置，实例方法必须设置</b>，
	 * 该约束由 {@code MethodToolCallback} 构造器负责校验，而非在 build() 中。
	 * </p>
	 *
	 * <p>
	 * <b>典型用法：</b>
	 * </p>
	 *
	 * <pre>{@code
	 * ToolCallback callback = MethodToolCallback.builder()
	 *         .toolDefinition(ToolDefinitions.from(method))
	 *         .toolMetadata(ToolMetadata.from(method))
	 *         .toolMethod(method)
	 *         .toolObject(bean)
	 *         .build();
	 * }</pre>
	 */
	public static final class Builder {

		// 中文：工具说明书，必填。
		private @Nullable ToolDefinition toolDefinition;

		// 中文：执行期元数据，可选。
		private @Nullable ToolMetadata toolMetadata;

		// 中文：目标方法，必填。
		private @Nullable Method toolMethod;

		// 中文：方法所属实例。静态方法可为 null；实例方法必填（由构造器校验）。
		private @Nullable Object toolObject;

		// 中文：结果转换器，可选。
		private @Nullable ToolCallResultConverter toolCallResultConverter;

		// 中文：私有构造器，只能通过 MethodToolCallback.builder() 创建。
		private Builder() {
		}

		// 中文：设置工具说明书（通常由 ToolDefinitions.from(method) 生成）。
		public Builder toolDefinition(ToolDefinition toolDefinition) {
			this.toolDefinition = toolDefinition;
			return this;
		}

		// 中文：设置执行期元数据（通常由 ToolMetadata.from(method) 生成）。
		public Builder toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		// 中文：设置要反射调用的目标方法。
		public Builder toolMethod(Method toolMethod) {
			this.toolMethod = toolMethod;
			return this;
		}

		// 中文：设置方法所属的对象实例（静态方法可省略）。
		public Builder toolObject(Object toolObject) {
			this.toolObject = toolObject;
			return this;
		}

		// 中文：设置自定义结果转换器。
		public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
			this.toolCallResultConverter = toolCallResultConverter;
			return this;
		}

		// 中文：完成构建。这里只校验两个必填项，
		// "非静态方法必须提供 toolObject" 的约束交由 MethodToolCallback 构造器统一校验，避免重复。
		@SuppressWarnings("null")
		public MethodToolCallback build() {
			Assert.state(this.toolDefinition != null, "ToolDefinition is required");
			Assert.state(this.toolMethod != null, "ToolMethod is required");
			return new MethodToolCallback(this.toolDefinition, this.toolMetadata, this.toolMethod, this.toolObject,
					this.toolCallResultConverter);
		}

	}

}
