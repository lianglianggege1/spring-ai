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
import org.springframework.ai.content.Media;

/**
 * 提示模板的「单条消息」接口——负责把渲染结果包装成一条 {@link Message}。
 *
 * <p>与 {@code PromptTemplateStringActions}（只产出字符串）和 {@code PromptTemplateChatActions}
 * （产出多条消息）不同，本接口聚焦「单条消息」的创建。三个重载的 {@code createMessage}
 * 区别在于附件与变量的组合：
 * <ul>
 *   <li>无参：渲染模板并构造单条消息（无附件）；</li>
 *   <li>带 {@link Media} 列表：在消息中携带多媒体附件（如图片）；</li>
 *   <li>带变量表：用传入的 model 覆盖占位符后再构造消息。</li>
 * </ul>
 */
public interface PromptTemplateMessageActions {

	// 渲染模板并构造一条消息（不含附件）。
	Message createMessage();

	// 构造消息并附带媒体附件列表（用于多模态输入）。
	Message createMessage(List<Media> mediaList);

	// 用传入的 model 变量表覆盖占位符后，构造一条消息。
	Message createMessage(Map<String, Object> model);

}
