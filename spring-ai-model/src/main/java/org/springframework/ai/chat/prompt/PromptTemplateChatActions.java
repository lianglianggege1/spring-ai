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

package org.springframework.ai.chat.prompt;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;

/**
 * 提示模板的「多消息」接口——负责把（可能由多个子模板构成的）模板渲染结果，
 * 组装成一组 {@link Message}（即一次对话的完整消息列表）。
 *
 * <p>它适用于「一次 Prompt 由多条消息拼成」的场景（例如系统消息 + 用户消息 + 样例消息）。
 * 两个重载的 {@code createMessages} 区别在于是否传入变量表覆盖模板占位符；与
 * {@code PromptTemplateMessageActions}（产出单条）相比，这里产出的是整段对话消息列表。
 */
public interface PromptTemplateChatActions {

	// 使用模板自身绑定的变量渲染，返回一组消息（整段对话）。
	List<Message> createMessages();

	// 用传入的 model 变量表覆盖占位符后，返回一组消息。
	List<Message> createMessages(Map<String, Object> model);

}
