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

package org.springframework.ai.tool.function;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link ToolCallback} implementation to invoke functions as tools.
 *
 * @author Thomas Vitale
 * @author YunKui Lu
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code FunctionToolCallback}：把一个<b>函数式接口</b>（Function / BiFunction /
 * Supplier / Consumer 或 Lambda）包装成大模型可调用工具的 {@link ToolCallback} 实现。
 *
 * <p>
 * <b>适用场景：</b>当你想快速注册一个工具、又不想为它专门写一个带 {@code @Tool} 注解的类时，
 * 用它可以直接把 Lambda 变成工具。这是与 {@code MethodToolCallback}（基于注解方法反射）
 * 并列的另一条注册路径，属于「编程式注册」。
 * </p>
 *
 * <p>
 * <b>泛型含义：</b>{@code <I>} = 工具入参类型，{@code <O>} = 工具返回值类型。
 * </p>
 *
 * <p>
 * <b>执行流程（{@link #call(String, ToolContext)}）：</b>
 * </p>
 * <ol>
 * <li>模型传回 JSON 字符串 → 按 {@code toolInputType} 反序列化成 {@code I} 对象</li>
 * <li>调用 {@code toolFunction} 执行业务逻辑，得到 {@code O} 结果</li>
 * <li>用 {@code toolCallResultConverter} 把结果转成字符串返回给模型</li>
 * </ol>
 *
 * <p>
 * <b>关键字段：</b>
 * </p>
 * <ul>
 * <li>{@code toolDefinition} —— 发给模型的工具说明书（名称/描述/入参 schema）</li>
 * <li>{@code toolMetadata} —— 执行期元数据（如 returnDirect）</li>
 * <li>{@code toolInputType} —— 入参的 {@link Type}，反序列化 JSON 时必须知道目标类型；
 * 用 {@code Type} 而非 {@code Class} 是为了支持 {@code List<Foo>} 这类泛型</li>
 * <li>{@code toolFunction} —— 真正的业务逻辑，统一收敛为 {@code BiFunction<I, ToolContext, O>}</li>
 * </ul>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * ToolCallback callback = FunctionToolCallback.builder("getWeather", (WeatherRequest req) -> queryWeather(req))
 *         .description("查询指定城市的天气")
 *         .inputType(WeatherRequest.class)
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author YunKui Lu
 * @since 1.0.0
 * @see org.springframework.ai.tool.method.MethodToolCallback
 */
public class FunctionToolCallback<I, O> implements ToolCallback {

	// 中文：JSON 序列化/反序列化助手，static final 复用同一实例（线程安全）。
	private static final JsonHelper jsonHelper = new JsonHelper();

	private static final Log logger = LogFactory.getLog(FunctionToolCallback.class);

	// 中文：默认结果转换器常量。提取为 static final 避免每次构造都新建对象。
	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	// 中文：默认元数据常量（returnDirect = false）。因为 ToolMetadata 实现是不可变的，所以可安全共享。
	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	// 中文：工具说明书 —— 会被发送给大模型。
	private final ToolDefinition toolDefinition;

	// 中文：工具执行期元数据 —— 仅框架内部使用，不发给模型。
	private final ToolMetadata toolMetadata;

	// 中文：入参类型。使用 java.lang.reflect.Type 而非 Class，
	// 这样才能表达 List<Foo>、Map<String, Bar> 等带泛型参数的类型（Class 会擦除泛型信息）。
	private final Type toolInputType;

	// 中文：实际执行的业务函数。所有形态（Function/Supplier/Consumer）在 builder 里
	// 都被统一适配成 BiFunction<入参, 工具上下文, 返回值>，使执行逻辑只需处理一种形态。
	// 泛型上的 @Nullable 表示：入参与上下文都可能为 null（例如 Supplier 场景入参就是 null）。
	private final BiFunction<@Nullable I, @Nullable ToolContext, O> toolFunction;

	// 中文：把返回值 O 转成字符串（供模型消费）的转换器。
	private final ToolCallResultConverter toolCallResultConverter;

	// 中文：全参构造器。注意 toolMetadata 与 toolCallResultConverter 允许传 null，
	// 内部会替换为默认值，这是典型的"可选依赖 + 空值兜底"写法。
	public FunctionToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Type toolInputType,
			BiFunction<@Nullable I, @Nullable ToolContext, O> toolFunction,
			@Nullable ToolCallResultConverter toolCallResultConverter) {
		// 中文：三个必填依赖做非空校验，快速失败。
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolInputType, "toolInputType cannot be null");
		Assert.notNull(toolFunction, "toolFunction cannot be null");
		this.toolDefinition = toolDefinition;
		// 中文：空值兜底 —— 未指定元数据时使用共享的默认实例。
		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
		this.toolFunction = toolFunction;
		this.toolInputType = toolInputType;
		// 中文：空值兜底 —— 未指定转换器时使用默认的 JSON 转换器。
		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
				: DEFAULT_RESULT_CONVERTER;
	}

	// 中文：返回工具说明书，框架据此把工具信息发送给模型。
	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	// 中文：返回工具执行期元数据（如 returnDirect）。
	@Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	// 中文：不带工具上下文的重载，直接委托给带上下文的版本并传 null，避免逻辑重复。
	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	// 中文：工具执行入口。入参 toolInput 是模型生成的 JSON 字符串，返回值也是字符串（回传给模型）。
	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		// 中文：先判断日志级别再拼接字符串，避免无谓的字符串拼接开销。
		if (logger.isDebugEnabled()) {
			logger.debug("Starting execution of tool: " + this.toolDefinition.name());
		}

		// 中文：第一步 —— 把模型给出的 JSON 反序列化成强类型入参对象 I。
		I request = jsonHelper.fromJson(toolInput, this.toolInputType);
		// 中文：第二步 —— 执行业务函数（异常在 callMethod 内统一包装）。
		O response = callMethod(request, toolContext);

		if (logger.isDebugEnabled()) {
			logger.debug("Successful execution of tool: " + this.toolDefinition.name());
		}

		// 中文：第三步 —— 把返回值转换成字符串。第二个参数是返回值的泛型 Type，
		// 这里传 null 表示由转换器自行推断（默认实现直接做 JSON 序列化，不需要该类型信息）。
		return this.toolCallResultConverter.convert(response, null);
	}

	// 中文：实际调用业务函数，并统一做异常包装。
	private O callMethod(@Nullable I request, @Nullable ToolContext toolContext) {
		try {
			return this.toolFunction.apply(request, toolContext);
		}
		catch (ToolExecutionException ex) {
			// 中文：已经是框架定义的工具执行异常，直接透传，避免重复包装导致异常链变长。
			throw ex;
		}
		catch (Exception ex) {
			// 中文：其他任意异常统一包装成 ToolExecutionException，
			// 并携带 toolDefinition，便于上层知道是"哪个工具"出错并决定是否把错误反馈给模型。
			throw new ToolExecutionException(this.toolDefinition, ex);
		}
	}

	// 中文：调试用的字符串表示。只输出定义与元数据，不输出 toolFunction（Lambda 的 toString 无可读性）。
	@Override
	public String toString() {
		return "FunctionToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
				+ '}';
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link BiFunction}.
	 */
	// 中文：入口一（最完整）—— 基于 BiFunction，函数可同时拿到「入参」和「ToolContext 工具上下文」。
	// 需要访问上下文（如当前用户、会话信息）时用这个重载。
	public static <I, O> Builder<I, O> builder(String name,
			BiFunction<@Nullable I, @Nullable ToolContext, O> function) {
		return new Builder<>(name, function);
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Function}.
	 */
	// 中文：入口二（最常用）—— 基于 Function，只关心入参、不需要上下文。
	// 内部用 Lambda 适配成 BiFunction，忽略 context 参数。
	public static <I, O> Builder<I, O> builder(String name, Function<I, O> function) {
		Assert.notNull(function, "function cannot be null");
		return new Builder<>(name, (request, context) -> function.apply(request));
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Supplier}.
	 */
	// 中文：入口三 —— 基于 Supplier，适用于「无入参、只有返回值」的工具（如"获取当前时间"）。
	// 注意两点：入参泛型固定为 Void；并且这里已自动调用 inputType(Void.class)，
	// 调用方无需再显式指定 inputType 即可 build()。
	public static <O> Builder<Void, O> builder(String name, Supplier<O> supplier) {
		Assert.notNull(supplier, "supplier cannot be null");
		Function<Void, O> function = input -> supplier.get();
		return builder(name, function).inputType(Void.class);
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Consumer}.
	 */
	// 中文：入口四 —— 基于 Consumer，适用于「有入参、无返回值」的工具（如"发送通知"）。
	// 返回值泛型固定为 Void，Lambda 内部执行完后返回 null。
	public static <I> Builder<I, Void> builder(String name, Consumer<I> consumer) {
		Assert.notNull(consumer, "consumer cannot be null");
		// 中文：@SuppressWarnings("NullAway") 用于抑制空安全检查告警 ——
		// 本包被 @NullMarked 标记（默认非空），但这里必须返回 null 来适配 Function<I, Void>，
		// 属于有意为之的例外。
		@SuppressWarnings("NullAway")
		Function<I, Void> function = (I input) -> {
			consumer.accept(input);
			return null;
		};
		return builder(name, function);
	}

	/**
	 * 【中文说明】{@code FunctionToolCallback} 的构建器。
	 *
	 * <p>
	 * <b>必填项：</b>{@code name}（构造时传入）、{@code toolFunction}（构造时传入）、
	 * {@code inputType}（必须显式设置，否则 build() 抛异常；Supplier 入口除外，它已自动设为 Void.class）。
	 * </p>
	 *
	 * <p>
	 * <b>可缺省并自动推导的项：</b>
	 * </p>
	 * <ul>
	 * <li>{@code description} —— 缺省时由工具名驼峰拆词生成</li>
	 * <li>{@code inputSchema} —— 缺省时由 {@code inputType} 反射自动生成 JSON Schema</li>
	 * <li>{@code toolMetadata} / {@code toolCallResultConverter} —— 缺省时用默认实例</li>
	 * </ul>
	 *
	 * <p>
	 * 泛型 {@code <I, O>} 与外部类保持一致，保证类型在链式调用中不丢失。
	 * </p>
	 */
	public static final class Builder<I, O> {

		// 中文：工具名，final 表示构造后不可变，必须在创建 Builder 时提供。
		private final String name;

		// 中文：工具描述，可缺省（build() 时由工具名自动推导）。
		private @Nullable String description;

		// 中文：入参 JSON Schema，可缺省（build() 时由 inputType 自动生成）。
		private @Nullable String inputSchema;

		// 中文：入参类型，build() 时的必填项（Supplier 入口已自动设为 Void.class）。
		private @Nullable Type inputType;

		// 中文：执行期元数据，可缺省。
		private @Nullable ToolMetadata toolMetadata;

		// 中文：业务函数，final 且必填，创建 Builder 时即确定。
		private final BiFunction<@Nullable I, @Nullable ToolContext, O> toolFunction;

		// 中文：结果转换器，可缺省。
		private @Nullable ToolCallResultConverter toolCallResultConverter;

		// 中文：私有构造器，只能通过外部类的 4 个静态 builder(...) 工厂方法创建。
		// 两个必填项在此就完成校验，实现"尽早失败"。
		private Builder(String name, BiFunction<@Nullable I, @Nullable ToolContext, O> toolFunction) {
			Assert.hasText(name, "name cannot be null or empty");
			Assert.notNull(toolFunction, "toolFunction cannot be null");
			this.name = name;
			this.toolFunction = toolFunction;
		}

		// 中文：设置工具描述（建议显式设置，直接影响模型选工具的准确率）。
		public Builder<I, O> description(String description) {
			this.description = description;
			return this;
		}

		// 中文：手动指定入参 JSON Schema。设置后将<b>覆盖</b>自动生成的结果，
		// 适合需要精细控制 schema（如添加枚举、格式约束）的场景。
		public Builder<I, O> inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		// 中文：设置入参类型（重载一）—— 传普通 Class 或 Type，适用于非泛型类型。
		public Builder<I, O> inputType(Type inputType) {
			this.inputType = inputType;
			return this;
		}

		// 中文：设置入参类型（重载二）—— 传 Spring 的 ParameterizedTypeReference，
		// 用于保留泛型信息，解决 Java 类型擦除问题。
		// 例如：new ParameterizedTypeReference<List<Foo>>() {}，
		// 这样 JSON 反序列化时才知道要还原成 List<Foo> 而不是 List<Object>。
		public Builder<I, O> inputType(ParameterizedTypeReference<?> inputType) {
			Assert.notNull(inputType, "inputType cannot be null");
			this.inputType = inputType.getType();
			return this;
		}

		// 中文：设置执行期元数据（如 returnDirect）。
		public Builder<I, O> toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		// 中文：设置自定义结果转换器。
		public Builder<I, O> toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
			this.toolCallResultConverter = toolCallResultConverter;
			return this;
		}

		// 中文：完成构建。
		public FunctionToolCallback<I, O> build() {
			// 中文：inputType 是唯一无法自动推导的必填项 —— 没有它就无法反序列化模型传回的 JSON，
			// 也无法生成 inputSchema，因此必须先校验。
			Assert.notNull(this.inputType, "inputType cannot be null");
			var toolDefinition = DefaultToolDefinition.builder()
				.name(this.name)
				// 中文：兜底一 —— description 为空时，由工具名驼峰拆词生成，
				// 例如 "getWeather" -> "get Weather"。
				.description(StringUtils.hasText(this.description) ? this.description
						: ToolUtils.getToolDescriptionFromName(this.name))
				// 中文：兜底二 —— inputSchema 为空时，反射 inputType 自动生成 JSON Schema。
				// 这正是"手动指定优先于自动生成"的取舍点。
				.inputSchema(StringUtils.hasText(this.inputSchema) ? this.inputSchema
						: JsonSchemaGenerator.generateForType(this.inputType))
				.build();
			// 中文：toolMetadata 与 toolCallResultConverter 允许为 null，
			// 由 FunctionToolCallback 构造器统一替换为默认实例。
			return new FunctionToolCallback<>(toolDefinition, this.toolMetadata, this.inputType, this.toolFunction,
					this.toolCallResultConverter);
		}

	}

}
