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

import org.jspecify.annotations.Nullable;

/**
 * Interface representing a request to an AI model. This interface encapsulates the
 * necessary information required to interact with an AI model, including instructions or
 * inputs (of generic type T) and additional model options. It provides a standardized way
 * to send requests to AI models, ensuring that all necessary details are included and can
 * be easily managed.
 *
 * @param <T> the type of instructions or input required by the AI model
 * @author Mark Pollack
 * @since 0.8.0
 */
/**
 * 【中文说明】ModelRequest 是所有"发送给 AI 模型的请求"的统一抽象接口。
 *
 * <p>
 * 设计思想：任何一次模型调用都可以拆解为两部分——
 * <ol>
 * <li><b>指令/输入（instructions）</b>：真正要让模型处理的数据，是必填项。</li>
 * <li><b>选项（options）</b>：调节模型行为的参数，如 temperature、maxTokens、model 名称等，可选。</li>
 * </ol>
 * 把这两者固化在接口契约中，使得框架层（如可观测性、重试、日志）能以统一方式读取任意请求。
 *
 * <p>
 * 关键泛型参数 {@code T}：指令的具体类型，由各模态自行决定。例如聊天场景下 Prompt 实现
 * {@code ModelRequest<List<Message>>}；嵌入场景下 EmbeddingRequest 实现
 * {@code ModelRequest<List<String>>}。
 *
 * <p>
 * 典型用法：Prompt、EmbeddingRequest、ImagePrompt 等均实现本接口，并作为 {@link Model#call} 的入参。
 *
 * @param <T> AI 模型所需的指令或输入的类型
 * @author Mark Pollack
 * @since 0.8.0
 */
public interface ModelRequest<T> {

	/**
	 * Retrieves the instructions or input required by the AI model.
	 * @return the instructions or input required by the AI model
	 */
	// 【中文】获取本次调用的核心输入内容（必填）。
	// 行尾原注释 "required input" 即强调：该值不应为 null，是请求的必要组成部分。
	T getInstructions(); // required input

	/**
	 * Retrieves the customizable options for AI model interactions.
	 * @return the customizable options for AI model interactions
	 */
	// 【中文】获取本次调用的可调参数（选项）。
	// 标注了 @Nullable，说明选项是可选的：返回 null 表示"未在请求级别指定"，
	// 此时具体实现通常会回退使用模型客户端构造时配置的默认选项（defaultOptions）。
	@Nullable ModelOptions getOptions();

}
