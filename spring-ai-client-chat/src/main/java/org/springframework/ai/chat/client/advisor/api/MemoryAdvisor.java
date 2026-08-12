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

package org.springframework.ai.chat.client.advisor.api;

/**
 * Marker interface for advisors that own the memory lifecycle. An advisor implementing
 * this interface takes responsibility for managing the chat memory, including storing,
 * retrieving, and updating conversation context.
 *
 * <p>
 * {@link org.springframework.ai.chat.client.DefaultChatClient} uses this marker to detect
 * whether a memory advisor is already present in the chain, and to avoid auto-registering
 * a duplicate when memory management is configured on the {@code ChatClient}.
 *
 * @author Christian Tzolov
 * @since 2.0.0
 * @see org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor
 */
/**
 * 用于标记管理记忆生命周期的顾问接口。实现该接口的顾问负责管理聊天记忆，
 * 包含会话上下文的存储、读取与更新。
 *
 * <p>
 * {@link org.springframework.ai.chat.client.DefaultChatClient} 通过该标记检测顾问链中是否已存在记忆顾问，
 * 避免在 {@code ChatClient} 配置记忆管理时自动注册重复的记忆顾问。
 *
 * @author Christian Tzolov
 * @since 2.0.0
 * @see org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor
 */
public interface MemoryAdvisor extends Advisor {

}
