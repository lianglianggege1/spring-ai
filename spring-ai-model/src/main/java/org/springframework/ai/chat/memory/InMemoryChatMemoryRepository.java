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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

/**
 * An in-memory implementation of {@link ChatMemoryRepository}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ChatMemoryRepository} 的**内存实现**，是框架的默认存储实现。
 *
 * <p>
 * 关键字段：{@code chatMemoryStore}——以 {@code conversationId} 为 key、消息列表为 value 的
 * {@link ConcurrentHashMap}，天然支持多线程并发读写。
 *
 * <p>
 * 典型用法：不需要额外配置时由 {@link MessageWindowChatMemory.Builder} 自动创建；
 * 也可显式 {@code new InMemoryChatMemoryRepository()} 传入。
 *
 * <p>
 * 使用注意：数据仅存在于 JVM 堆内存，**应用重启即全部丢失**，且多实例部署时各节点内存互相独立、
 * 数据不共享；同时会话只增不减容易造成内存泄漏。因此仅适合本地开发、单元测试或演示场景，
 * 生产环境请换用 JDBC / Redis / Cassandra 等持久化实现。
 *
 * <p>
 * 类被声明为 final，不可被继承扩展。对应英文 javadoc 中的标签：@author Thomas Vitale；@since 1.0.0。
 */
public final class InMemoryChatMemoryRepository implements ChatMemoryRepository {

	// 【中文】实际的存储容器：key = 会话 id，value = 该会话的消息列表。
	// 选用 ConcurrentHashMap 而非普通 HashMap，是为了保证多线程（多个并发请求）读写时的线程安全。
	// 注意此字段是包级私有（无修饰符）而非 private，便于同包下的测试代码直接检查内部状态。
	Map<String, List<Message>> chatMemoryStore = new ConcurrentHashMap<>();

	@Override
	public List<String> findConversationIds() {
		// 【中文】用 new ArrayList<>(...) 包一层做防御性拷贝：
		// keySet() 返回的是与原 Map 关联的视图，若直接返回，调用方对其做删除操作会影响到内部存储。
		return new ArrayList<>(this.chatMemoryStore.keySet());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		List<Message> messages = this.chatMemoryStore.get(conversationId);
		// 【中文】空值处理：会话不存在时返回不可变空列表 List.of() 而不是 null，遵循接口约定，
		// 让调用方无需判空即可直接遍历。存在时同样做防御性拷贝，避免外部修改污染内部数据。
		return messages != null ? new ArrayList<>(messages) : List.of();
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		// 【中文】noNullElements 校验列表内部不含 null 元素——列表本身非 null 不代表元素都非 null，
		// 若放任 null 消息入库，后续拼装 Prompt 时才会抛 NPE，届时排查成本更高，故在入口处快速失败。
		Assert.noNullElements(messages, "messages cannot contain null elements");
		// 【中文】put 是整体覆盖语义，与接口 saveAll 的"替换全部消息"约定一致。
		this.chatMemoryStore.put(conversationId, messages);
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		// 【中文】直接移除整个 key；会话不存在时 remove 不会报错，因此该操作是幂等的。
		this.chatMemoryStore.remove(conversationId);
	}

}
