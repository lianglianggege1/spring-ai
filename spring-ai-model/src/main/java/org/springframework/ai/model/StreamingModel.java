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

import reactor.core.publisher.Flux;

/**
 * The StreamingModel interface provides a generic API for invoking an AI models with
 * streaming response. It abstracts the process of sending requests and receiving a
 * streaming responses. The interface uses Java generics to accommodate different types of
 * requests and responses, enhancing flexibility and adaptability across different AI
 * model implementations.
 *
 * @param <TReq> the generic type of the request to the AI model
 * @param <TResChunk> the generic type of a single item in the streaming response from the
 * AI model
 * @author Christian Tzolov
 * @since 0.8.0
 */
/**
 * 【中文说明】StreamingModel 是 Spring AI 中所有"流式调用型"AI 模型的顶层抽象接口，
 * 是 {@link Model} 的流式对应版本。
 *
 * <p>
 * 用途：模型（尤其是大语言模型）生成内容通常是逐 token 产出的，本接口用 Reactor 的
 * {@link Flux} 把这些增量片段以响应式流的形式推送给调用方，从而实现"打字机"效果，
 * 显著降低用户感知延迟。
 *
 * <p>
 * 关键泛型参数：
 * <ul>
 * <li>{@code TReq} —— 请求类型，与同步版本完全一致（如 Prompt），说明流式与非流式共用同一套请求模型。</li>
 * <li>{@code TResChunk} —— 注意这里是"流中的单个数据块"的类型，而不是整体响应。它同样被约束为
 * {@link ModelResponse} 子类型（如 ChatResponse），即每个片段本身也是一个结构完整的响应对象，
 * 只不过其中承载的内容是增量的一小段。</li>
 * </ul>
 *
 * <p>
 * 典型用法：ChatModel 同时继承 {@code Model} 和 {@code StreamingModel}，调用方可按需在
 * {@code call()} 与 {@code stream()} 之间切换。
 *
 * @param <TReq> 发送给 AI 模型的请求泛型类型
 * @param <TResChunk> 流式响应中单个数据块的泛型类型
 * @author Christian Tzolov
 * @since 0.8.0
 */
public interface StreamingModel<TReq extends ModelRequest<?>, TResChunk extends ModelResponse<?>> {

	/**
	 * Executes a method call to the AI model.
	 * @param request the request object to be sent to the AI model
	 * @return the streaming response from the AI model
	 */
	// 【中文】以流式（非阻塞）方式调用 AI 模型，返回一个 Flux 响应式流。
	// 返回的 Flux 一般是"冷流"（cold stream）：只有被订阅时才真正发起对模型的请求；
	// 每订阅一次就会触发一次独立调用，因此不要重复订阅同一个 Flux 以免产生多次计费。
	Flux<TResChunk> stream(TReq request);

}
