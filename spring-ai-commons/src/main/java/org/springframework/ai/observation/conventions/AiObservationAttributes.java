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

package org.springframework.ai.observation.conventions;

/**
 * Collection of attribute keys used in AI observations (spans, metrics, events). Inspired
 * by the OpenTelemetry Semantic Conventions for Generative AI.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * Semantic Conventions for Generative AI</a>.
 */
/**
 * AI观测（链路Span、指标、事件）所使用的属性键集合。
 * 参考OpenTelemetry生成式AI语义约定规范实现。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * Semantic Conventions for Generative AI</a>.
 */
public enum AiObservationAttributes {

// @formatter:off

	// GenAI General

	/**
	 * The name of the operation being performed.
	 */
	/**
	 * 当前正在执行的操作名称。
	 */
	AI_OPERATION_TYPE("gen_ai.operation.name"),
	/**
	 * The model provider as identified by the client instrumentation.
	 */
	/**
	 * 由客户端埋点识别出的模型提供商。
	 */
	AI_PROVIDER("gen_ai.system"),

	// GenAI Request

	/**
	 * The name of the model a request is being made to.
	 */
	/**
	 * 请求所调用的模型名称。
	 */
	REQUEST_MODEL("gen_ai.request.model"),
	/**
	 * The frequency penalty setting for the model request.
	 */
	/**
	 * 模型请求的频率惩罚参数。
	 */
	REQUEST_FREQUENCY_PENALTY("gen_ai.request.frequency_penalty"),
	/**
	 * The maximum number of tokens the model generates for a request.
	 */
	/**
	 * 模型针对单次请求可生成的最大Token数量。
	 */
	REQUEST_MAX_TOKENS("gen_ai.request.max_tokens"),
	/**
	 * The presence penalty setting for the model request.
	 */
	/**
	 * 模型请求的存在惩罚参数。
	 */
	REQUEST_PRESENCE_PENALTY("gen_ai.request.presence_penalty"),
	/**
	 * List of sequences that the model will use to stop generating further tokens.
	 */
	/**
	 * 用于使模型停止继续生成Token的序列列表。
	 */
	REQUEST_STOP_SEQUENCES("gen_ai.request.stop_sequences"),
	/**
	 * Indicates whether the GenAI request was made in streaming mode.
	 */
	/**
	 * 标识生成式AI请求是否以流式模式发起。
	 */
	REQUEST_STREAM("gen_ai.request.stream"),
	/**
	 * The temperature setting for the model request.
	 */
	/**
	 * 模型请求的温度参数。
	 */
	REQUEST_TEMPERATURE("gen_ai.request.temperature"),
	/**
	 * List of tool definitions provided to the model in the request.
	 */
	/**
	 * 请求中提供给模型的工具定义列表。
	 */
	REQUEST_TOOL_NAMES("spring.ai.model.request.tool.names"),
	/**
	 * The top_k sampling setting for the model request.
	 */
	/**
	 * 模型请求的top‑k采样参数。
	 */
	REQUEST_TOP_K("gen_ai.request.top_k"),
	/**
	 * The top_p sampling setting for the model request.
	 */
	/**
	 * 模型请求的top‑p采样参数。
	 */
	REQUEST_TOP_P("gen_ai.request.top_p"),

	/**
	 * The number of dimensions the resulting output embeddings have.
	 */
	/**
	 * 输出的嵌入向量的维度数量。
	 */
	/**
	 * The number of dimensions the resulting output embeddings have.
	 */
	REQUEST_EMBEDDING_DIMENSIONS("gen_ai.request.embedding.dimensions"),

	/**
	 * The format in which the generated image is returned.
	 */
	/**
	 * 生成图片返回时所使用的格式。
	 */
	REQUEST_IMAGE_RESPONSE_FORMAT("gen_ai.request.image.response_format"),
	/**
	 * The size of the image to generate.
	 */
	/**
	 * 生成图片时所使用的尺寸。
	 */
	REQUEST_IMAGE_SIZE("gen_ai.request.image.size"),
	/**
	 * The style of the image to generate.
	 */
	/**
	 * 待生成图片的风格。
	 */
	REQUEST_IMAGE_STYLE("gen_ai.request.image.style"),

	// GenAI Response

	/**
	 * Reasons the model stopped generating tokens, corresponding to each generation received.
	 */
	/**
	 * 模型停止生成Token的原因，与每一条生成结果一一对应。
	 */
	RESPONSE_FINISH_REASONS("gen_ai.response.finish_reasons"),
	/**
	 * The unique identifier for the AI response.
	 */
	/**
	 * AI响应的唯一标识符。
	 */
	RESPONSE_ID("gen_ai.response.id"),
	/**
	 * The name of the model that generated the response.
	 */
	/**
	 * 生成该响应的模型名称。
	 */
	RESPONSE_MODEL("gen_ai.response.model"),

	// GenAI Usage

	/**
	 * The number of input tokens written to a provider-managed cache.
	 */
	/**
	 * 缓存写入的输入Token数量。
	 */
	USAGE_CACHE_WRITE_INPUT_TOKENS("gen_ai.usage.cache_creation.input_tokens"),
	/**
	 * The number of input tokens served from a provider-managed cache.
	 * /
	/**
	 * 缓存读取的输入Token数量。
	 */
	USAGE_CACHE_READ_INPUT_TOKENS("gen_ai.usage.cache_read.input_tokens"),
	/**
	 * The number of tokens used in the model input.
	 */
	/**
	 * 模型输入所消耗的Token数量。
	 */
	USAGE_INPUT_TOKENS("gen_ai.usage.input_tokens"),
	/**
	 * The number of tokens used in the model output.
	 */
	/**
	 * 模型输出所消耗的Token数量。
	 */
	USAGE_OUTPUT_TOKENS("gen_ai.usage.output_tokens"),
	/**
	 * The total number of tokens used in the model exchange.
	 */
	/**
	 * 模型交互过程总共消耗的Token数量。
	 */
	USAGE_TOTAL_TOKENS("gen_ai.usage.total_tokens");

	private final String value;

	AiObservationAttributes(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the attribute key.
	 * @return the value of the attribute key
	 */
	/**
	 * 返回该属性键对应的值。
	 * @return 属性键对应的值
	 */
	public String value() {
		return this.value;
	}

// @formatter:on

}
