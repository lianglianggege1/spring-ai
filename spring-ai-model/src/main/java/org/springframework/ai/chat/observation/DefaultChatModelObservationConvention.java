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

package org.springframework.ai.chat.observation;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default conventions to populate observations for chat model operations.
 *
 * @author Thomas Vitale
 * @author Soby Chacko
 * @since 1.0.0
 */
/**
 * 【中文说明】DefaultChatModelObservationConvention 是对话模型观测的<b>默认约定实现</b>，
 * 负责把 {@link ChatModelObservationContext} 中的请求/响应信息翻译成 Micrometer 的标签（KeyValues）。
 *
 * <p>
 * 命名规范遵循 OpenTelemetry GenAI 语义约定，观测名固定为 {@code gen_ai.client.operation}。
 *
 * <p>
 * 三类输出：
 * <ul>
 * <li>{@link #getName()}：指标名。</li>
 * <li>{@link #getContextualName(ChatModelObservationContext)}：链路追踪的 span 名，
 * 格式为 "操作类型 模型名"（如 {@code chat gpt-4o}）。</li>
 * <li>{@code getLowCardinalityKeyValues} / {@code getHighCardinalityKeyValues}：
 * 分别产出低基数（进指标维度）与高基数（仅进追踪）标签。</li>
 * </ul>
 *
 * <p>
 * 扩展设计（重点）：所有具体标签的提取逻辑都被拆成独立的 {@code protected} 方法
 * （如 {@code requestTemperature}、{@code usageTotalTokens}），这是典型的<b>模板方法模式</b>——
 * 使用者只需继承本类并覆写个别方法，就能定制或新增标签，无需重写整个约定。
 *
 * <p>
 * 通用编码模式：高基数标签方法统一采用 {@code (KeyValues, context) -> KeyValues} 的签名，
 * 内部判断"值是否存在"，存在则 {@code keyValues.and(...)} 追加后返回新对象，不存在则原样返回。
 * KeyValues 是不可变对象，因此必须接收返回值（链式覆盖赋值）。
 */
public class DefaultChatModelObservationConvention implements ChatModelObservationConvention {

	// 中文说明：默认观测名称，符合 OpenTelemetry GenAI 语义约定
	public static final String DEFAULT_NAME = "gen_ai.client.operation";

	// 中文说明：请求模型名缺失时的占位标签。预先创建为静态常量，避免每次调用都重复构造对象。
	// 之所以要打占位值 "none" 而不是省略标签，是因为指标要求同一指标名下的标签集合必须保持一致。
	private static final KeyValue REQUEST_MODEL_NONE = KeyValue
		.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL, KeyValue.NONE_VALUE);

	// 中文说明：响应模型名缺失时的占位标签，原因同上
	private static final KeyValue RESPONSE_MODEL_NONE = KeyValue
		.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL, KeyValue.NONE_VALUE);

	// 中文说明：返回观测名称，即最终指标的名字
	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	// 中文说明：返回链路追踪中 span 的显示名。
	// 有模型名时拼成 "chat gpt-4o"，否则退化为只有操作类型 "chat"。
	@Override
	public String getContextualName(ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && StringUtils.hasText(options.getModel())) {
			return "%s %s".formatted(context.getOperationMetadata().operationType(), options.getModel());
		}
		return context.getOperationMetadata().operationType();
	}

	// 中文说明：组装低基数标签集合。这 4 个标签是固定的、必定存在的（缺失时用 "none" 占位），
	// 从而保证指标维度稳定，不会出现维度数量忽多忽少的问题。
	@Override
	public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
		return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context),
				responseModel(context));
	}

	// 中文说明：操作类型标签，取自上下文的操作元数据（对话场景恒为 "chat"）
	protected KeyValue aiOperationType(ChatModelObservationContext context) {
		return KeyValue.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
				context.getOperationMetadata().operationType());
	}

	// 中文说明：服务提供商标签，如 openai、ollama
	protected KeyValue aiProvider(ChatModelObservationContext context) {
		return KeyValue.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
				context.getOperationMetadata().provider());
	}

	// 中文说明：请求模型名标签。空值处理：options 为 null 或模型名为空白时，回退到 "none" 占位常量
	protected KeyValue requestModel(ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && StringUtils.hasText(options.getModel())) {
			return KeyValue.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
					options.getModel());
		}
		return REQUEST_MODEL_NONE;
	}

	// 中文说明：响应模型名标签。空值处理：响应尚未回填（如观测刚开始或调用失败）时回退到 "none"
	protected KeyValue responseModel(ChatModelObservationContext context) {
		if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
			return KeyValue.of(ChatModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL,
					context.getResponse().getMetadata().getModel());
		}
		return RESPONSE_MODEL_NONE;
	}

	// 中文说明：组装高基数标签集合。
	// 与低基数不同，这些标签是"有才加、没有就跳过"的，因此数量会随请求内容变化。
	// 实现上从空集合出发，依次把每个提取方法的结果链式覆盖赋值给 keyValues
	// （KeyValues 不可变，and(...) 返回的是新实例，必须重新赋值）。
	@Override
	public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
		var keyValues = KeyValues.empty();
		// Request
		keyValues = requestFrequencyPenalty(keyValues, context);
		keyValues = requestMaxTokens(keyValues, context);
		keyValues = requestPresencePenalty(keyValues, context);
		keyValues = requestStopSequences(keyValues, context);
		keyValues = requestStream(keyValues, context);
		keyValues = requestTemperature(keyValues, context);
		keyValues = requestTools(keyValues, context);
		keyValues = requestTopK(keyValues, context);
		keyValues = requestTopP(keyValues, context);
		// Response
		keyValues = responseFinishReasons(keyValues, context);
		keyValues = responseId(keyValues, context);
		keyValues = usageCacheWriteInputTokens(keyValues, context);
		keyValues = usageCacheReadInputTokens(keyValues, context);
		keyValues = usageInputTokens(keyValues, context);
		keyValues = usageOutputTokens(keyValues, context);
		keyValues = usageTotalTokens(keyValues, context);
		return keyValues;
	}

	// Request

	// 中文说明：频率惩罚标签。空值处理：options 或该参数为 null 时原样返回 keyValues（即不加此标签）
	protected KeyValues requestFrequencyPenalty(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getFrequencyPenalty() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_FREQUENCY_PENALTY.asString(),
					String.valueOf(options.getFrequencyPenalty()));
		}
		return keyValues;
	}

	// 中文说明：最大生成 token 数标签，未设置则不添加
	protected KeyValues requestMaxTokens(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getMaxTokens() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_MAX_TOKENS.asString(),
					String.valueOf(options.getMaxTokens()));
		}
		return keyValues;
	}

	// 中文说明：存在惩罚标签，未设置则不添加
	protected KeyValues requestPresencePenalty(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getPresencePenalty() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_PRESENCE_PENALTY.asString(),
					String.valueOf(options.getPresencePenalty()));
		}
		return keyValues;
	}

	// 中文说明：是否流式标签。注意这里是"单向"的——只有流式调用才打标签（值恒为 true），
	// 同步调用不会打 false 标签，因此在监控中"无此标签"即代表同步调用。
	protected KeyValues requestStream(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.isStreaming()) {
			return keyValues.and(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STREAM.asString(),
					String.valueOf(true));
		}
		return keyValues;
	}

	// 中文说明：停止词列表标签。用 StringJoiner 手工拼成 JSON 数组风格的字符串，
	// 形如 ["\n", "END"]（每个元素加双引号，用 ", " 分隔，整体用方括号包裹）。
	// 这样做是为了让标签值符合 OpenTelemetry 对数组型属性的字符串表示约定。
	protected KeyValues requestStopSequences(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && !CollectionUtils.isEmpty(options.getStopSequences())) {
			StringJoiner stopSequencesJoiner = new StringJoiner(", ", "[", "]");
			options.getStopSequences().forEach(value -> stopSequencesJoiner.add("\"" + value + "\""));
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STOP_SEQUENCES.asString(),
					stopSequencesJoiner.toString());
		}
		return keyValues;
	}

	// 中文说明：采样温度标签，未设置则不添加
	protected KeyValues requestTemperature(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getTemperature() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TEMPERATURE.asString(),
					String.valueOf(options.getTemperature()));
		}
		return keyValues;
	}

	// 中文说明：工具名列表标签。三个要点：
	// 1) 用 instanceof 模式匹配做"提前返回"——只有 ToolCallingChatOptions 才带工具信息，
	//    普通 ChatOptions 直接原样返回，避免无谓处理；
	// 2) 用 HashSet 收集工具名，天然去重（同名工具只记一次），但也意味着输出顺序不固定；
	// 3) 同样拼成 ["toolA", "toolB"] 的数组字符串格式。
	protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
		if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
			return keyValues;
		}

		Set<String> toolNames = new HashSet<>();
		if (!CollectionUtils.isEmpty(options.getToolCallbacks())) {
			toolNames.addAll(options.getToolCallbacks().stream().map(tc -> tc.getToolDefinition().name()).toList());
		}

		if (!CollectionUtils.isEmpty(toolNames)) {
			StringJoiner toolNamesJoiner = new StringJoiner(", ", "[", "]");
			toolNames.forEach(value -> toolNamesJoiner.add("\"" + value + "\""));
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOOL_NAMES.asString(),
					toolNamesJoiner.toString());
		}
		return keyValues;
	}

	// 中文说明：top-k 采样参数标签，未设置则不添加
	protected KeyValues requestTopK(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getTopK() != null) {
			return keyValues.and(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_K.asString(),
					String.valueOf(options.getTopK()));
		}
		return keyValues;
	}

	// 中文说明：top-p（核采样）参数标签，未设置则不添加
	protected KeyValues requestTopP(KeyValues keyValues, ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options != null && options.getTopP() != null) {
			return keyValues.and(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_P.asString(),
					String.valueOf(options.getTopP()));
		}
		return keyValues;
	}

	// Response

	// 中文说明：结束原因列表标签。处理流程：
	// 先过滤掉没有结束原因的生成结果，若过滤后为空则直接返回（不打空数组标签），
	// 否则拼成 ["STOP"] 这样的数组字符串。
	// 注意这里有两层空判断：外层判 response/results，内层判过滤后的 finishReasons。
	protected KeyValues responseFinishReasons(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
			var finishReasons = context.getResponse()
				.getResults()
				.stream()
				.filter(generation -> StringUtils.hasText(generation.getMetadata().getFinishReason()))
				.map(generation -> generation.getMetadata().getFinishReason())
				.toList();
			if (CollectionUtils.isEmpty(finishReasons)) {
				return keyValues;
			}
			StringJoiner finishReasonsJoiner = new StringJoiner(", ", "[", "]");
			finishReasons.forEach(finishReason -> finishReasonsJoiner.add("\"" + finishReason + "\""));
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_FINISH_REASONS.asString(),
					finishReasonsJoiner.toString());
		}
		return keyValues;
	}

	// 中文说明：响应 id 标签，响应为空或 id 为空白时不添加
	protected KeyValues responseId(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getId())) {
			return keyValues.and(ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_ID.asString(),
					context.getResponse().getMetadata().getId());
		}
		return keyValues;
	}

	// 中文说明：缓存写入 token 数标签。三重判空：response -> usage -> 该字段本身，
	// 因为并非所有厂商都支持 Prompt Caching，不支持时该值为 null，此时不打标签。
	protected KeyValues usageCacheWriteInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null
				&& context.getResponse().getMetadata().getUsage().getCacheWriteInputTokens() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_CACHE_WRITE_INPUT_TOKENS.asString(),
					String.valueOf(context.getResponse().getMetadata().getUsage().getCacheWriteInputTokens()));
		}
		return keyValues;
	}

	// 中文说明：缓存命中 token 数标签，判空逻辑同上
	protected KeyValues usageCacheReadInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null
				&& context.getResponse().getMetadata().getUsage().getCacheReadInputTokens() != null) {
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_CACHE_READ_INPUT_TOKENS.asString(),
					String.valueOf(context.getResponse().getMetadata().getUsage().getCacheReadInputTokens()));
		}
		return keyValues;
	}

	// 中文说明：输入 token 数标签。与缓存类字段不同，这里只做两重判空（response、usage），
	// 因为 promptTokens 被视为必有值。
	protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null) {
			// 中文说明：注意——这一行调用的返回值没有被使用，属于无副作用的空语句（疑似历史遗留代码）。
			// 真正取值发生在下面 String.valueOf(...) 里，删掉这行也不影响行为。
			context.getResponse().getMetadata().getUsage().getPromptTokens();
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
					String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
		}
		return keyValues;
	}

	// 中文说明：输出 token 数标签，逻辑与 usageInputTokens 完全对称
	protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null) {
			// 中文说明：同样是返回值未被使用的空语句，实际取值在下方 String.valueOf(...) 中
			context.getResponse().getMetadata().getUsage().getCompletionTokens();
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_OUTPUT_TOKENS.asString(),
					String.valueOf(context.getResponse().getMetadata().getUsage().getCompletionTokens()));
		}
		return keyValues;
	}

	// 中文说明：总 token 数标签，是成本核算最常用的观测项
	protected KeyValues usageTotalTokens(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata().getUsage() != null) {
			// 中文说明：同上，此行返回值未被使用
			context.getResponse().getMetadata().getUsage().getTotalTokens();
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
					String.valueOf(context.getResponse().getMetadata().getUsage().getTotalTokens()));
		}
		return keyValues;
	}

}
