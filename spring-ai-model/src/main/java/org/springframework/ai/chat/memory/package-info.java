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

/**
 * 【中文说明】聊天记忆（Chat Memory）包：为多轮对话提供历史消息的存取与裁剪能力。
 *
 * <p>
 * 核心类型：
 * <ul>
 * <li>{@link org.springframework.ai.chat.memory.ChatMemory}——记忆策略接口（存什么、留多少）；</li>
 * <li>{@link org.springframework.ai.chat.memory.ChatMemoryRepository}——存储接口（存到哪里）；</li>
 * <li>{@link org.springframework.ai.chat.memory.MessageWindowChatMemory}——滑动窗口策略实现；</li>
 * <li>{@link org.springframework.ai.chat.memory.InMemoryChatMemoryRepository}——默认的内存存储实现。</li>
 * </ul>
 *
 * <p>
 * 类上的 {@code @NullMarked}（JSpecify 规范）作用于整个包：表示包内所有类型默认**不可为 null**，
 * 只有显式标注 {@code @Nullable} 的地方才允许为 null，便于 IDE 和静态分析工具做空指针检查。
 */
@NullMarked
package org.springframework.ai.chat.memory;

import org.jspecify.annotations.NullMarked;
