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
 * Collection of systems providing AI functionality. Inspired by the OpenTelemetry
 * Semantic Conventions for Generative AI.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * Semantic Conventions for Generative AI</a>.
 */
/**
 * 提供AI功能的系统集合。参考OpenTelemetry生成式AI语义约定。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * 生成式AI语义约定</a>.
 */
public enum AiProvider {

	// @formatter:off

	/**
	 * AI system provided by Anthropic.
	 */
	/**
	 * Anthropic提供的AI系统。
	 */
	ANTHROPIC("anthropic"),

	/**
	 * AI system provided by Bedrock Converse.
	 */
	/**
	 * Bedrock Converse 提供的AI系统。
	 */
	BEDROCK_CONVERSE("bedrock_converse"),

	/**
	 * AI system provided by DeepSeek.
	 */
	/**
	 * DeepSeek提供的AI系统。
	 */
	DEEPSEEK("deepseek"),

	/**
	 * AI system provided by Google Gen AI.
	 */
	/**
	 * Google Gen AI 提供的AI系统。
	 */
	GOOGLE_GENAI_AI("google_genai"),

	/**
	 * AI system provided by Mistral.
	 */
	/**
	 * Mistral提供的AI系统。
	 */
	MISTRAL_AI("mistral_ai"),

	/**
	 * AI system provided by Ollama.
	 */
	/**
	 * Ollama提供的AI系统。
	 */
	OLLAMA("ollama"),

	/**
	 * AI system provided by ONNX.
	 */
	/**
	 * ONNX提供的AI系统。
	 */
	ONNX("onnx"),

	/**
	 * AI system provided by OpenAI.
	 */
	/**
	 * OpenAI提供的AI系统。
	 */
	OPENAI("openai"),

	/**
	 * AI system provided by Spring AI.
	 */
	/**
	 * Spring AI提供的AI系统。
	 */
	SPRING_AI("spring_ai"),

	/**
	 * AI system provided by Vertex AI.
	 */
	/**
	 * Vertex AI提供的AI系统。
	 */
	VERTEX_AI("vertex_ai");

	private final String value;

	AiProvider(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the provider.
	 * @return the value of the provider
	 */
	/**
	 * 返回提供商对应的值。
	 * @return 提供商对应的值
	 */
	public String value() {
		return this.value;
	}

	// @formatter:on

}
