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
 * 聊天消息包：定义 Spring AI 对话系统中「消息」的核心抽象与具体类型。
 *
 * <p>包含：{@link Message} 接口、{@link AbstractMessage} 基类、{@link MessageType} 角色枚举，
 * 以及四种具体消息 {@link UserMessage}/{@link AssistantMessage}/{@link SystemMessage}/{@link ToolResponseMessage}，
 * 另有内部工具类 {@link MessageUtils}。
 */
@NullMarked
package org.springframework.ai.chat.messages;

import org.jspecify.annotations.NullMarked;
