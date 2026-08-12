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

package org.springframework.ai.chat.model;

import java.util.Arrays;
import java.util.Optional;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.StreamingModel;

/**
 * 【中文说明】StreamingChatModel 是"流式对话模型"的抽象接口，用于以增量（token/chunk）方式获取模型输出。
 *
 * <p>
 * 设计要点：
 * <ul>
 * <li>标注了 {@code @FunctionalInterface}，说明它只有一个抽象方法 {@link #stream(Prompt)}，
 * 因此可以用 Lambda 表达式直接实现，便于测试与快速适配。</li>
 * <li>另外两个 {@code stream(String)} / {@code stream(Message...)} 都是 default 便捷方法，
 * 内部把入参包装成 Prompt 后委托给核心方法，并把 {@link ChatResponse} 映射为纯文本片段。</li>
 * <li>{@link ChatModel} 继承了本接口，所以所有对话模型在类型上都"具备"流式能力
 * （不支持时由 ChatModel 的默认实现抛出 UnsupportedOperationException）。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * Flux<String> textFlux = streamingChatModel.stream("讲个笑话");
 * textFlux.subscribe(System.out::print);
 * }</pre>
 *
 * <p>
 * 注意：流式返回的每个 ChatResponse 通常只包含"增量片段"，若需要完整消息，可配合
 * {@link MessageAggregator} 做聚合。
 */
@FunctionalInterface
public interface StreamingChatModel extends StreamingModel<Prompt, ChatResponse> {

	// 中文说明：便捷方法——传入一段文本，返回纯文本的增量流。
	// 这里用 Optional 链式调用做空值处理：getResult() 可能为 null，
	// 任意一环为空时最终 orElse("") 返回空串，保证下游订阅者不会收到 null。
	default Flux<String> stream(String message) {
		Prompt prompt = new Prompt(message);
		return stream(prompt).map(response -> Optional.ofNullable(response.getResult())
			.map(Generation::getOutput)
			.map(AssistantMessage::getText)
			.orElse(""));
	}

	// 中文说明：便捷方法——传入多条 Message（含系统提示/历史对话），返回纯文本增量流。
	// 空值处理逻辑同上：任一环节为 null 则该片段映射为空串。
	default Flux<String> stream(Message... messages) {
		Prompt prompt = new Prompt(Arrays.asList(messages));
		return stream(prompt).map(response -> Optional.ofNullable(response.getResult())
			.map(Generation::getOutput)
			.map(AssistantMessage::getText)
			.orElse(""));
	}

	// 中文说明：唯一的抽象方法（函数式接口的 SAM），由具体厂商实现完成流式请求与响应切分。
	@Override
	Flux<ChatResponse> stream(Prompt prompt);

}
