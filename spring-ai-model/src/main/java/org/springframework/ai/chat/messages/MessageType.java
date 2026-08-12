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

/**
 * Enumeration representing types of {@link Message Messages} in a chat application. It
 * can be one of the following: USER, ASSISTANT, SYSTEM, FUNCTION.
 */
/**
 * 消息类型枚举：定义聊天应用中 {@link Message} 的角色类型。
 *
 * <p>可选值：USER（用户）、ASSISTANT（助手）、SYSTEM（系统）、TOOL（工具）。
 * {@link #getValue()} 返回的小写字符串会直接用于大模型 API 请求体的 role 字段。
 */
public enum MessageType {

	/**
	 * A {@link Message} of type {@literal user}, having the user role and originating
	 * from an end-user or developer.
	 * @see UserMessage
	 */
	/**
	 * 用户消息：归属用户角色，由终端用户或开发者发出。
	 * @see UserMessage
	 */
	USER("user"),

	/**
	 * A {@link Message} of type {@literal assistant} passed in subsequent input
	 * {@link Message Messages} as the {@link Message} generated in response to the user.
	 * @see AssistantMessage
	 */
	/**
	 * 助手消息：作为对用户的应答生成，可传入后续对话中。
	 * @see AssistantMessage
	 */
	ASSISTANT("assistant"),

	/**
	 * A {@link Message} of type {@literal system} passed as input {@link Message
	 * Messages} containing high-level instructions for the conversation, such as behave
	 * like a certain character or provide answers in a specific format.
	 * @see SystemMessage
	 */
	/**
	 * 系统消息：对话的顶层指令（如设定角色、规定回答格式）。
	 * @see SystemMessage
	 */
	SYSTEM("system"),

	/**
	 * A {@link Message} of type {@literal function} passed as input {@link Message
	 * Messages} with function content in a chat application.
	 * @see ToolResponseMessage
	 */
	/**
	 * 工具消息：承载函数/工具调用相关内容，作为对话输入回传。
	 * @see ToolResponseMessage
	 */
	TOOL("tool");

	private final String value;

	MessageType(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
