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

package org.springframework.ai.chat.memory;

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

/**
 * The contract for storing and managing the memory of chat conversations.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】聊天记忆（对话历史）的顶层抽象接口。
 *
 * <p>
 * 用途：为多轮对话提供"记忆"能力。大模型本身是无状态的，每次请求都必须把历史消息一起发送过去，
 * ChatMemory 就负责按会话（conversation）维度存取这些历史 {@link Message}。
 *
 * <p>
 * 关键概念：
 * <ul>
 * <li>{@code conversationId}——会话标识，所有读写操作都以它为维度做隔离，不同用户/不同会话互不干扰；</li>
 * <li>{@link #CONVERSATION_ID}——放入 Advisor 上下文（context）中传递会话 id 所用的约定 key。</li>
 * </ul>
 *
 * <p>
 * 典型用法：一般不直接调用，而是配合 MessageChatMemoryAdvisor 之类的 Advisor 使用；
 * Advisor 会在请求前调用 {@link #get(String)} 取出历史并拼进 Prompt，在响应后调用
 * {@link #add(String, List)} 把新一轮的问答写回。
 *
 * <p>
 * 实现关系：本接口只定义"记忆策略"（如窗口大小、裁剪规则），真正的持久化交给
 * {@link ChatMemoryRepository}；内置实现见 {@link MessageWindowChatMemory}。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Christian Tzolov、Thomas Vitale；@since 1.0.0。
 */
public interface ChatMemory {

	/**
	 * The key to retrieve the chat memory conversation id from the context.
	 */
	// 【中文】从上下文（Advisor context / ToolContext）中取出会话 id 时使用的约定 key，
	// 值为字符串常量 "chat_memory_conversation_id"，各处硬编码 key 容易写错，故统一在此定义。
	String CONVERSATION_ID = "chat_memory_conversation_id";

	/**
	 * Save the specified message in the chat memory for the specified conversation.
	 */
	/**
	 * 【中文说明】向指定会话追加**单条**消息的便捷默认方法。
	 *
	 * <p>
	 * 这是一个 default 方法：内部先做参数校验，再委托给批量版本
	 * {@link #add(String, List)}，因此实现类只需实现批量方法即可，无需重复写单条逻辑。
	 * @param conversationId 会话 id，不能为 null 或空白字符串
	 * @param message 要保存的消息，不能为 null
	 */
	default void add(String conversationId, Message message) {
		// 【中文】参数校验：hasText 比 notNull 更严格，要求非 null 且去掉空白后长度大于 0，
		// 防止用空字符串当作会话 id 导致所有会话的历史串到一起。
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(message, "message cannot be null");
		// 【中文】统一收敛到批量接口：单条消息包装成不可变的单元素 List 再委托，避免逻辑分叉。
		this.add(conversationId, List.of(message));
	}

	/**
	 * Save the specified messages in the chat memory for the specified conversation.
	 */
	// 【中文】批量追加消息到指定会话（核心写入方法，由实现类提供）。
	// 注意语义是"追加"而非"覆盖"；实现类可在此处施加记忆策略，例如超出窗口上限时裁剪旧消息。
	void add(String conversationId, List<Message> messages);

	/**
	 * Get the messages in the chat memory for the specified conversation.
	 */
	// 【中文】读取指定会话当前的全部历史消息，按时间先后顺序返回。
	// 会话不存在时约定返回空列表而不是 null，调用方可直接遍历。
	List<Message> get(String conversationId);

	/**
	 * Clear the chat memory for the specified conversation.
	 */
	// 【中文】清空指定会话的全部历史消息，常用于用户点击"新建对话/清除上下文"的场景。
	void clear(String conversationId);

}
