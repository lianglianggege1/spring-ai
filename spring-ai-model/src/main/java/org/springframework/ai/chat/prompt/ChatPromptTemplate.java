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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;

/**
 * A PromptTemplate that lets you specify the role as a string should the current
 * implementations and their roles not suffice for your needs.
 */
/**
 * 由「多个子 {@link PromptTemplate}」组合而成的提示模板。
 *
 * <p>与单个 {@link PromptTemplate} 只能产出一条固定角色的消息不同，本类持有一组子模板，
 * 每个子模板都可以自行指定角色（例如一个 SystemPromptTemplate + 一个 UserPromptTemplate）。
 * 渲染或建消息时，会按顺序遍历所有子模板并拼接，从而灵活拼出整段对话。
 *
 * <p>它同时实现了 {@link PromptTemplateActions}（产出 {@link Prompt}）与
 * {@link PromptTemplateChatActions}（产出 {@code List<Message>}），因此既能拿到完整 Prompt，
 * 也能拿到中间的消息列表。注意：本类内部直接复用 {@link PromptTemplate} 的渲染/建消息能力，
 * 并未限定角色，角色由各个子模板自己决定。
 */
public class ChatPromptTemplate implements PromptTemplateActions, PromptTemplateChatActions {

	// 组成本次对话的全部子模板，按加入顺序渲染/拼接。
	private final List<PromptTemplate> promptTemplates;

	public ChatPromptTemplate(List<PromptTemplate> promptTemplates) {
		this.promptTemplates = promptTemplates;
	}

	@Override
	public String render() {
		// 逐个子模板渲染并拼接（使用子模板自身绑定的变量），得到合并后的完整文本。
		StringBuilder sb = new StringBuilder();
		for (PromptTemplate promptTemplate : this.promptTemplates) {
			sb.append(promptTemplate.render());
		}
		return sb.toString();
	}

	@Override
	public String render(Map<String, Object> model) {
		// 用统一的 model 变量表覆盖所有子模板的占位符后拼接渲染结果。
		StringBuilder sb = new StringBuilder();
		for (PromptTemplate promptTemplate : this.promptTemplates) {
			sb.append(promptTemplate.render(model));
		}
		return sb.toString();
	}

	@Override
	public List<Message> createMessages() {
		// 遍历子模板，各自生成一条消息，按顺序收集成整段对话的消息列表。
		List<Message> messages = new ArrayList<>();
		for (PromptTemplate promptTemplate : this.promptTemplates) {
			messages.add(promptTemplate.createMessage());
		}
		return messages;
	}

	@Override
	public List<Message> createMessages(Map<String, Object> model) {
		// 用统一变量表覆盖占位符后，逐个子模板生成消息并收集。
		List<Message> messages = new ArrayList<>();
		for (PromptTemplate promptTemplate : this.promptTemplates) {
			messages.add(promptTemplate.createMessage(model));
		}
		return messages;
	}

	@Override
	public Prompt create() {
		// 先拼出消息列表，再包成 Prompt（不带模型参数）。
		List<Message> messages = createMessages();
		return new Prompt(messages);
	}

	@Override
	public Prompt create(ChatOptions modelOptions) {
		// 拼出消息列表，并附带模型级参数 modelOptions。
		List<Message> messages = createMessages();
		return new Prompt(messages, modelOptions);
	}

	@Override
	public Prompt create(Map<String, Object> model) {
		// 用变量表覆盖占位符后拼消息，再包成 Prompt。
		List<Message> messages = createMessages(model);
		return new Prompt(messages);
	}

	@Override
	public Prompt create(Map<String, Object> model, ChatOptions modelOptions) {
		// 同时传入变量表与模型参数：既填占位符又附 ChatOptions。
		List<Message> messages = createMessages(model);
		return new Prompt(messages, modelOptions);
	}

}
