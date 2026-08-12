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

package org.springframework.ai.model.tool;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Mixin interface for ChatModels that support structured output. Provides a unified way
 * to set and get the output JSON schema.
 *
 * @author Christian Tzolov
 */
/**
 * 【中文说明】为支持「结构化输出」的 ChatModel 提供的混入（Mixin）接口。
 *
 * <p>
 * 核心只有一个字段：{@code outputSchema}，即期望模型输出遵循的 <b>JSON Schema 字符串</b>。
 * 各家模型对结构化输出的参数名不一致（如 OpenAI 的 {@code response_format.json_schema}），
 * 本接口把它统一抽象为 getter/setter，使上层通用代码（如 ChatClient 的实体映射）
 * 无需关心具体厂商实现。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * StructuredOutputChatOptions options = StructuredOutputChatOptions.builder()
 *         .model("gpt-4o")
 *         .outputSchema(jsonSchemaString)
 *         .build();
 * }</pre>
 *
 * <p>
 * 默认实现为 {@link DefaultStructuredOutputChatOptions}。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Christian Tzolov。
 */
public interface StructuredOutputChatOptions extends ChatOptions {

	// 【中文说明】获取期望模型遵循的输出 JSON Schema；未设置结构化输出时为 null
	@Nullable String getOutputSchema();

	/**
	 * Returns a new {@link StructuredOutputChatOptions.Builder} initialized with the
	 * values of this {@link StructuredOutputChatOptions}.
	 *
	 * Narrows the return type of {@link ChatOptions#mutate()} so generic structured
	 * output code can chain
	 * {@code structuredOptions.mutate().outputSchema(schema).build()} without casting.
	 */
	/**
	 * 【中文说明】返回一个以当前对象各字段值预填充的 Builder，用于基于现有选项做局部修改。
	 *
	 * <p>
	 * 同样使用<b>协变返回类型</b>收窄了父接口 {@link ChatOptions#mutate()} 的返回值，
	 * 使得 {@code structuredOptions.mutate().outputSchema(schema).build()} 可以无强转链式书写。
	 */
	@Override
	StructuredOutputChatOptions.Builder<?> mutate();

	/**
	 * A builder to create a new {@link StructuredOutputChatOptions} instance.
	 */
	/**
	 * 【中文说明】创建默认实现（{@link DefaultStructuredOutputChatOptions}）的 Builder 入口。
	 */
	static StructuredOutputChatOptions.Builder<?> builder() {
		return new DefaultStructuredOutputChatOptions.Builder<>();
	}

	/**
	 * 【中文说明】构建 {@link StructuredOutputChatOptions} 的 Builder 接口。
	 *
	 * <p>
	 * 泛型 {@code B extends Builder<B>} 为<b>递归泛型（自限定类型）</b>，
	 * 保证继承体系中的链式调用始终返回子类型，避免类型丢失。
	 */
	interface Builder<B extends StructuredOutputChatOptions.Builder<B>> extends ChatOptions.Builder<B> {

		// 【中文说明】设置输出 JSON Schema；传 null 表示不启用结构化输出
		B outputSchema(@Nullable String outputSchema);

	}

}
