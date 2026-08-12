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

package org.springframework.ai.embedding.observation;

import java.util.Optional;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.util.StringUtils;

/**
 * Default conventions to populate observations for embedding model operations.
 *
 * @author Thomas Vitale
 * @author Soby Chacko
 * @author Mengqi Xu
 * @since 1.0.0
 */
/**
 * 嵌入模型可观测性的<b>默认约定（Convention）</b>实现。
 *
 * <p>
 * 所谓 Convention，是 Micrometer Observation 体系中的"命名与打标约定"：它决定一次嵌入调用
 * 被记录成指标/链路时，观测名叫什么、带哪些标签（KeyValue）。
 *
 * <p>
 * 标签分两类，这个区分对监控系统至关重要：
 * <ul>
 * <li><b>低基数（Low Cardinality）</b>：取值种类少，如操作类型、厂商、模型名。
 * 它们会成为指标的 tag，可安全用于聚合分组；</li>
 * <li><b>高基数（High Cardinality）</b>：取值种类多，如 token 数、维度。
 * 只进链路追踪（trace），若做成指标 tag 会引发时间序列爆炸。</li>
 * </ul>
 *
 * <p>
 * 命名遵循 OpenTelemetry 的 GenAI 语义约定，默认观测名为
 * {@code gen_ai.client.operation}。
 *
 * <p>
 * 扩展方式：各 {@code protected} 方法均可被子类重写，以定制单个标签的取值逻辑。
 *
 * @author Thomas Vitale
 * @author Soby Chacko
 * @author Mengqi Xu
 * @since 1.0.0
 */
public class DefaultEmbeddingModelObservationConvention implements EmbeddingModelObservationConvention {

	// 默认观测名，遵循 OpenTelemetry GenAI 语义约定
	public static final String DEFAULT_NAME = "gen_ai.client.operation";

	// 预建的"模型名缺失"占位标签，复用常量可避免每次请求都新建对象
	// 之所以要占位而不是省略该标签，是为了保证指标的标签集合稳定（监控系统要求维度一致）
	private static final KeyValue REQUEST_MODEL_NONE = KeyValue
		.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL, KeyValue.NONE_VALUE);

	// 同上，响应侧模型名缺失时的占位标签
	private static final KeyValue RESPONSE_MODEL_NONE = KeyValue
		.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL, KeyValue.NONE_VALUE);

	// 返回观测名称（对应指标名）
	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	// 返回上下文名称（对应 span 名）：优先用"操作类型 + 模型名"，取不到模型名则只用操作类型
	@Override
	public String getContextualName(EmbeddingModelObservationContext context) {
		// Optional 链式空值处理：options 可能为 null，模型名可能为 null 或空串
		return Optional.ofNullable(context.getRequest().getOptions())
			.map(EmbeddingOptions::getModel)
			.filter(StringUtils::hasText)
			.map(model -> "%s %s".formatted(context.getOperationMetadata().operationType(), model))
			.orElseGet(() -> context.getOperationMetadata().operationType());
	}

	// 组装低基数标签集合：可安全用作指标维度
	@Override
	public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
		return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context),
				responseModel(context));
	}

	// 标签：AI 操作类型，此处固定为 embedding
	protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
		return KeyValue.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
				context.getOperationMetadata().operationType());
	}

	// 标签：模型厂商，如 openai、ollama
	protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
		return KeyValue.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
				context.getOperationMetadata().provider());
	}

	// 标签：请求中指定的模型名；缺失时回退为 NONE 占位值，保证标签集合不缺项
	protected KeyValue requestModel(EmbeddingModelObservationContext context) {
		return Optional.ofNullable(context.getRequest().getOptions())
			.map(EmbeddingOptions::getModel)
			.filter(StringUtils::hasText)
			.map(model -> KeyValue.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
					model))
			.orElse(REQUEST_MODEL_NONE);
	}

	// 标签：响应中实际返回的模型名；注意响应本身可能为 null（调用失败时），故需层层判空
	protected KeyValue responseModel(EmbeddingModelObservationContext context) {
		return Optional.ofNullable(context.getResponse())
			.map(EmbeddingResponse::getMetadata)
			.map(EmbeddingResponseMetadata::getModel)
			.filter(StringUtils::hasText)
			.map(model -> KeyValue.of(EmbeddingModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL,
					model))
			.orElse(RESPONSE_MODEL_NONE);
	}

	// 组装高基数标签集合：仅用于链路追踪，逐项累加（KeyValues 不可变，每次 and 返回新实例）
	@Override
	public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
		var keyValues = KeyValues.empty();
		// Request
		// 请求侧：期望的向量维度
		keyValues = requestEmbeddingDimension(keyValues, context);
		// Response
		// 响应侧：token 用量
		keyValues = usageInputTokens(keyValues, context);
		keyValues = usageTotalTokens(keyValues, context);
		return keyValues;
	}

	// Request

	// 高基数标签：请求中指定的向量维度；未指定则原样返回 keyValues（即不添加该标签）
	protected KeyValues requestEmbeddingDimension(KeyValues keyValues, EmbeddingModelObservationContext context) {
		return Optional.ofNullable(context.getRequest().getOptions())
			.map(EmbeddingOptions::getDimensions)
			.map(dimensions -> keyValues
				.and(EmbeddingModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_EMBEDDING_DIMENSIONS
					.asString(), String.valueOf(dimensions)))
			.orElse(keyValues);
	}

	// Response

	// 高基数标签：输入 token 数（prompt tokens），用于成本核算
	protected KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
		return Optional.ofNullable(context.getResponse())
			.map(EmbeddingResponse::getMetadata)
			.map(EmbeddingResponseMetadata::getUsage)
			.map(Usage::getPromptTokens)
			.map(promptTokens -> keyValues.and(
					EmbeddingModelObservationDocumentation.HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
					String.valueOf(promptTokens)))
			.orElse(keyValues);
	}

	// 高基数标签：总 token 数；与上一方法结构相同，同样在任一环节为 null 时跳过打标
	protected KeyValues usageTotalTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
		return Optional.ofNullable(context.getResponse())
			.map(EmbeddingResponse::getMetadata)
			.map(EmbeddingResponseMetadata::getUsage)
			.map(Usage::getTotalTokens)
			.map(totalTokens -> keyValues.and(
					EmbeddingModelObservationDocumentation.HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
					String.valueOf(totalTokens)))
			.orElse(keyValues);
	}

}
