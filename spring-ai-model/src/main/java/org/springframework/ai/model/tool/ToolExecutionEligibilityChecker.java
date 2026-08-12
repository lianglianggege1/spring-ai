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

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ChatResponse;

/**
 * Interface for determining when tool execution should be performed based on model
 * responses.
 *
 * @author Christian Tzolov
 */
/**
 * 【中文说明】判定「本次模型响应是否需要执行工具调用」的策略接口。
 *
 * <p>
 * 它继承自 {@code Function<ChatResponse, Boolean>}，因此本身是一个<b>函数式接口</b>，
 * 可以直接用 Lambda 实现，例如：
 *
 * <pre>{@code
 * ToolExecutionEligibilityChecker checker =
 *         response -> response.hasToolCalls(); // 仅示意
 * }</pre>
 *
 * <p>
 * 设计目的：把「是否触发工具执行」的判断从 ChatModel 中抽离出来，
 * 让不同厂商模型（返回结构、finishReason 语义各不相同）可以定制自己的判定逻辑。
 * 待实现的抽象方法即为 {@code Function#apply(ChatResponse)}。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Christian Tzolov。
 */
public interface ToolExecutionEligibilityChecker extends Function<ChatResponse, Boolean> {

	/**
	 * Determines if the response is a tool call message response.
	 * @param chatResponse The response from the chat model call
	 * @return true if the response is a tool call message response, false otherwise
	 */
	/**
	 * 【中文说明】判断给定响应是否为「工具调用类型」的响应。
	 *
	 * <p>
	 * 这是对 {@code apply()} 的一层空值安全包装：先判空再委托给具体实现，
	 * 从而把 {@code Boolean} 自动拆箱可能引发的 NPE 挡在外面。
	 * @param chatResponse 聊天模型返回的响应，允许为 {@code null}
	 * @return 需要执行工具时返回 {@code true}；响应为 null 或无需执行工具时返回 {@code false}
	 */
	default boolean isToolCallResponse(@Nullable ChatResponse chatResponse) {
		// 空值处理：响应为 null 直接判定为「非工具调用」，同时避免 Boolean 拆箱 NPE
		return chatResponse != null && apply(chatResponse);
	}

}
