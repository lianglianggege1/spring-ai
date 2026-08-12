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

import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.Resource;

/**
 * 【中文说明】助手（Assistant）提示词模板：继承 {@link PromptTemplate}，
 * 渲染结果被包装成 {@link AssistantMessage}，即"模型的回复"这一角色的消息。
 *
 * <p>
 * 用途：主要用于**构造伪造的历史回复**，常见于两种场景：
 * <ul>
 * <li>Few-shot（少样本）提示：手工编造几轮"用户问—助手答"示例，引导模型模仿输出格式；</li>
 * <li>预填充回复开头：给出助手回复的前缀，诱导模型按指定格式续写。</li>
 * </ul>
 *
 * <p>
 * 与 {@link SystemPromptTemplate} 的结构完全对称，仅消息类型不同；
 * 但本类未提供自己的 Builder，只有两个便捷构造器。
 *
 * <p>
 * 典型用法：{@code new AssistantPromptTemplate("好的，我将以{format}格式回答。").createMessage(vars)}。
 */
public class AssistantPromptTemplate extends PromptTemplate {

	// 【中文】由模板字符串构造。
	public AssistantPromptTemplate(String template) {
		super(template);
	}

	// 【中文】由资源文件构造。
	public AssistantPromptTemplate(Resource resource) {
		super(resource);
	}

	// 【中文】重写：产出只含一条助手消息的 Prompt。
	@Override
	public Prompt create() {
		return new Prompt(new AssistantMessage(render()));
	}

	// 【中文】重写：用变量渲染后产出只含一条助手消息的 Prompt（参数 model 指变量表）。
	@Override
	public Prompt create(Map<String, Object> model) {
		return new Prompt(new AssistantMessage(render(model)));
	}

	// 【中文】重写：渲染结果包装成 AssistantMessage。
	@Override
	public Message createMessage() {
		return new AssistantMessage(render());
	}

	// 【中文】重写：用变量渲染后包装成 AssistantMessage。
	@Override
	public Message createMessage(Map<String, Object> model) {
		return new AssistantMessage(render(model));
	}

}
