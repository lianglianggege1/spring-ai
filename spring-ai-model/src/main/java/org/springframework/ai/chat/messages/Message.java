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

package org.springframework.ai.chat.messages;

import org.springframework.ai.content.Content;

/**
 * The Message interface represents a message that can be sent or received in a chat
 * application. Messages can have content, media attachments, properties, and message
 * types.
 *
 * @see Media
 * @see MessageType
 */
/**
 * 消息接口：聊天应用中收发消息的统一抽象。
 *
 * <p>所有具体消息类型（用户消息、助手消息、系统消息、工具消息）都实现本接口。它扩展了
 * {@link Content} 接口，因此消息可以携带文本内容（getText）和元数据（getMetadata），
 * 并在此基础上增加了消息类型 {@link MessageType} 这一维度。
 *
 * @see org.springframework.ai.content.Content
 * @see MessageType
 */
public interface Message extends Content {

	/**
	 * Get the message type.
	 * @return the message type
	 */
	/**
	 * 获取消息类型（如 USER / ASSISTANT / SYSTEM / TOOL）。
	 * @return 消息类型
	 */
	MessageType getMessageType();

}
