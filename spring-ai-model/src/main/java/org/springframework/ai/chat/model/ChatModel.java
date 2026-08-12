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

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Model;

/**
 * 【中文说明】ChatModel 是 Spring AI 中"对话模型"的核心抽象接口，代表一个可以进行多轮对话的大语言模型。
 *
 * <p>
 * 继承关系：
 * <ul>
 * <li>{@code Model<Prompt, ChatResponse>}：定义了同步调用语义，输入 {@link Prompt}，输出
 * {@link ChatResponse}。</li>
 * <li>{@link StreamingChatModel}：定义了流式调用语义，返回 {@code Flux<ChatResponse>}。</li>
 * </ul>
 * 因此一个 ChatModel 实现天然同时具备"同步"和"流式"两种能力。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #call(Prompt)}：唯一必须由子类实现的抽象方法，是所有对话能力的落脚点。</li>
 * <li>{@code call(String)} / {@code call(Message...)}：默认方法，为常见场景提供的便捷重载，内部都会包装成
 * Prompt 后委托给 {@code call(Prompt)}。</li>
 * <li>{@link #getOptions()}：返回该模型的默认参数（温度、模型名、maxTokens 等）。</li>
 * <li>{@link #stream(Prompt)}：默认抛出 UnsupportedOperationException，不支持流式的实现可以不覆写。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * String answer = chatModel.call("你好，介绍一下 Spring AI");
 * ChatResponse resp = chatModel.call(new Prompt(new UserMessage("你好")));
 * Flux<ChatResponse> flux = chatModel.stream(new Prompt("讲个故事"));
 * }</pre>
 */
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {

	// 中文说明：便捷方法——直接传入一段文本，内部会包装成 UserMessage 再构造 Prompt。
	// 返回值可能为 null（由 @Nullable 标注），但实现上：若无结果则返回空字符串 ""，
	// 注意这里做了空值保护，避免 getResult() 返回 null 时抛 NPE。
	default @Nullable String call(String message) {
		Prompt prompt = new Prompt(new UserMessage(message));
		Generation generation = call(prompt).getResult();
		// 空值处理：没有生成结果时返回空串而不是 null
		return (generation != null) ? generation.getOutput().getText() : "";
	}

	// 中文说明：便捷方法——传入若干条 Message（可混合 System/User/Assistant 消息），
	// 适合需要携带系统提示词或历史对话的场景。同样只返回首条生成结果的纯文本。
	default @Nullable String call(Message... messages) {
		Prompt prompt = new Prompt(Arrays.asList(messages));
		Generation generation = call(prompt).getResult();
		// 空值处理：没有生成结果时返回空串而不是 null
		return (generation != null) ? generation.getOutput().getText() : "";
	}

	// 中文说明：核心抽象方法（唯一必须实现的方法）。各厂商适配层（OpenAI、DashScope、Ollama 等）
	// 在此完成"请求组装 -> HTTP 调用 -> 响应解析 -> 工具调用循环"的完整流程。
	@Override
	ChatResponse call(Prompt prompt);

	/**
	 * Gets the chat options for this model.
	 * @return the chat options
	 * @since 2.0.0
	 */
	/**
	 * 【中文说明】获取该模型的默认对话参数（如 model、temperature、topP、maxTokens 等）。
	 * <p>
	 * 默认实现返回一个"空配置"对象；具体厂商实现通常会覆写此方法，返回构造时注入的默认 options。
	 * 运行时 Prompt 自带的 options 一般会与此处的默认值做合并（Prompt 优先级更高）。
	 * @return 该模型的默认 ChatOptions
	 * @since 2.0.0
	 */
	default ChatOptions getOptions() {
		return ChatOptions.builder().build();
	}

	/**
	 * @deprecated use {@link #getOptions()} instead.
	 */
	/**
	 * 【中文说明】旧版获取默认参数的方法，已废弃并计划移除（forRemoval = true）。
	 * <p>
	 * 内部直接委托给 {@link #getOptions()}，保留仅为向后兼容，新代码请改用 {@code getOptions()}。
	 * @deprecated 请改用 {@link #getOptions()}
	 */
	@Deprecated(forRemoval = true)
	default ChatOptions getDefaultOptions() {
		return getOptions();
	}

	// 中文说明：流式调用的默认实现。这里故意抛出 UnsupportedOperationException，
	// 表示"该模型默认不支持流式输出"；支持 SSE/流式的厂商实现需要覆写此方法。
	// 这样设计的好处：ChatModel 继承了 StreamingChatModel，但实现方可以只关心 call(Prompt)。
	default Flux<ChatResponse> stream(Prompt prompt) {
		throw new UnsupportedOperationException("streaming is not supported");
	}

}
