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

/**
 * The Model interface provides a generic API for invoking AI models. It is designed to
 * handle the interaction with various types of AI models by abstracting the process of
 * sending requests and receiving responses. The interface uses Java generics to
 * accommodate different types of requests and responses, enhancing flexibility and
 * adaptability across different AI model implementations.
 *
 * @param <TReq> the generic type of the request to the AI model
 * @param <TRes> the generic type of the response from the AI model
 * @author Mark Pollack
 * @since 0.8.0
 */
/**
 * 【中文说明】Model 是 Spring AI 中所有"同步调用型"AI 模型的最顶层抽象接口。
 *
 * <p>
 * 用途：把"发请求 -> 拿响应"这一通用交互过程抽象出来，屏蔽不同厂商（OpenAI、Ollama、
 * Anthropic 等）模型 API 的差异，使上层代码只依赖统一契约而非具体实现。
 *
 * <p>
 * 关键泛型参数：
 * <ul>
 * <li>{@code TReq} —— 请求类型，必须是 {@link ModelRequest} 的子类型，例如 ChatModel 对应的
 * Prompt、EmbeddingModel 对应的 EmbeddingRequest。</li>
 * <li>{@code TRes} —— 响应类型，必须是 {@link ModelResponse} 的子类型，例如 ChatResponse、
 * EmbeddingResponse。</li>
 * </ul>
 *
 * <p>
 * 注意这里使用了"有界泛型 + 通配符"（{@code ModelRequest<?>}）的写法：只约束请求/响应必须遵循
 * Spring AI 的统一结构，但不限制其内部承载的具体数据类型，从而兼顾类型安全与灵活性。
 *
 * <p>
 * 典型用法：ChatModel、EmbeddingModel、ImageModel 等都直接或间接继承本接口；对应的流式版本
 * 请参见 {@link StreamingModel}。业务代码通常注入具体的子接口（如 ChatModel）而非本接口。
 *
 * @param <TReq> 发送给 AI 模型的请求泛型类型
 * @param <TRes> AI 模型返回的响应泛型类型
 * @author Mark Pollack
 * @since 0.8.0
 */
public interface Model<TReq extends ModelRequest<?>, TRes extends ModelResponse<?>> {

	/**
	 * Executes a method call to the AI model.
	 * @param request the request object to be sent to the AI model
	 * @return the response from the AI model
	 */
	// 【中文】以阻塞（同步）方式调用 AI 模型：传入请求对象，等待模型处理完毕后一次性返回完整响应。
	// 若需要边生成边接收的流式效果，请改用 StreamingModel#stream。
	TRes call(TReq request);

}
