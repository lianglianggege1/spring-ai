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

/**
 * A repository for storing and retrieving chat messages.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】聊天消息的**存储层**抽象（仓储接口），负责消息的实际持久化与读取。
 *
 * <p>
 * 与 {@link ChatMemory} 的分工（典型的策略 + 存储分离设计）：
 * <ul>
 * <li>{@link ChatMemory} 关注"记忆策略"，例如只保留最近 N 条、如何裁剪、系统消息是否保留；</li>
 * <li>{@code ChatMemoryRepository} 只关注"存到哪里、怎么取"，不关心业务策略。</li>
 * </ul>
 * 这样替换存储介质（内存 / JDBC / Redis / Cassandra 等）时无需改动记忆策略代码。
 *
 * <p>
 * 方法风格上刻意贴近 Spring Data 的命名习惯（findBy... / saveAll / deleteBy...），便于理解。
 * 内置的默认实现是 {@link InMemoryChatMemoryRepository}（基于 ConcurrentHashMap，进程重启即丢失）。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Thomas Vitale；@since 1.0.0。
 */
public interface ChatMemoryRepository {

	// 【中文】返回当前仓储中所有已存在的会话 id 列表，可用于会话列表展示或批量清理。
	List<String> findConversationIds();

	// 【中文】按会话 id 查询其全部消息，按写入顺序返回；会话不存在时应返回空列表而非 null。
	List<Message> findByConversationId(String conversationId);

	/**
	 * Replaces all the existing messages for the given conversation ID with the provided
	 * messages.
	 */
	// 【中文】注意语义是"整体替换"（replace）而不是"追加"：
	// 会用传入的 messages 覆盖该会话原有的全部消息。
	// 这样设计是为了配合 ChatMemory 的裁剪策略——上层先算好"裁剪后的完整列表"，再一次性落盘，
	// 存储层因此无需理解任何窗口/裁剪逻辑，也天然保证了存储结果与策略计算结果一致。
	void saveAll(String conversationId, List<Message> messages);

	// 【中文】按会话 id 删除该会话的全部消息，对应 ChatMemory#clear 的底层操作。
	void deleteByConversationId(String conversationId);

}
