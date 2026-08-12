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
 * Types of operations performed by AI systems. Inspired by the OpenTelemetry Semantic
 * Conventions for Generative AI.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * Semantic Conventions for Generative AI</a>.
 */
/**
 * AI系统执行的操作类型。参考OpenTelemetry生成式AI语义约定规范设计。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai">OpenTelemetry
 * 生成式AI语义约定</a>.
 */
public enum AiOperationType {

	// @formatter:off

	/**
	 * AI operation type for chat completion.
	 */
	/**
	 * 用于聊天补全的AI操作类型。
	 */
	CHAT("chat"),

	/**
	 * AI operation type for embedding.
	 */
	/**
	 * 用于向量嵌入的AI操作类型。
	 */
	EMBEDDING("embedding"),

	/**
	 * AI operation type for tool execution.
	 */
	/**
	 * 用于工具执行的AI操作类型。
	 */
	EXECUTE_TOOL("execute_tool"),

	/**
	 * AI operation type for framework.
	 */
	/**
	 * 框架层面的AI操作类型。
	 */
	FRAMEWORK("framework"),

	/**
	 * AI operation type for image.
	 */
	/**
	 * 图像生成相关的AI操作类型。
	 */
	IMAGE("image"),

	/**
	 * AI operation type for text completion.
	 */
	/**
	 * 文本补全的AI操作类型。
	 */
	TEXT_COMPLETION("text_completion");

	private final String value;

	AiOperationType(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the operation type.
	 * @return the value of the operation type
	 */
	/**
	 * 获取操作类型对应的值。
	 * @return 操作类型的值
	 */
	public String value() {
		return this.value;
	}

	// @formatter:on

}
