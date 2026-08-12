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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * 【中文说明】系统提示词模板：继承 {@link PromptTemplate}，但渲染结果被包装成
 * {@link SystemMessage}（系统消息）而非默认的用户消息。
 *
 * <p>
 * 用途：系统消息用于设定模型的角色、语气和行为准则（俗称"人设"），
 * 通常放在对话最前面且只有一条。把它做成模板，就能动态注入角色名、领域知识等变量。
 *
 * <p>
 * 实现方式：仅重写 {@code createMessage()} / {@code create()} 四个方法，
 * 把父类中的 {@code UserMessage} 换成 {@code SystemMessage}；
 * 模板渲染逻辑（{@code render()}）完全复用父类，是典型的"模板方法 + 局部重写"。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * var t = new SystemPromptTemplate("你是一位{role}，请用{style}的语气回答。");
 * Message sys = t.createMessage(Map.of("role", "医生", "style", "严谨"));
 * }</pre>
 */
public class SystemPromptTemplate extends PromptTemplate {

	// 【中文】由模板字符串构造，直接委托父类。
	public SystemPromptTemplate(String template) {
		super(template);
	}

	// 【中文】由资源文件构造，直接委托父类。
	public SystemPromptTemplate(Resource resource) {
		super(resource);
	}

	// 【中文】完整构造器（字符串版），private：仅供本类的 Builder 使用。
	private SystemPromptTemplate(String template, Map<String, Object> variables, TemplateRenderer renderer) {
		super(template, variables, renderer);
	}

	// 【中文】完整构造器（资源版），同样仅供 Builder 使用。
	private SystemPromptTemplate(Resource resource, Map<String, Object> variables, TemplateRenderer renderer) {
		super(resource, variables, renderer);
	}

	// 【中文】重写：渲染结果包装成 SystemMessage（父类返回的是 UserMessage）。
	@Override
	public Message createMessage() {
		return new SystemMessage(render());
	}

	// 【中文】重写：用传入变量渲染后包装成 SystemMessage。
	// 参数名为 model，此处指"模板数据模型"（变量表），与"AI 模型"无关，勿混淆。
	@Override
	public Message createMessage(Map<String, Object> model) {
		return new SystemMessage(render(model));
	}

	// 【中文】重写：产出只含一条系统消息的 Prompt。
	@Override
	public Prompt create() {
		return new Prompt(new SystemMessage(render()));
	}

	// 【中文】重写：用变量渲染后产出只含一条系统消息的 Prompt。
	@Override
	public Prompt create(Map<String, Object> model) {
		return new Prompt(new SystemMessage(render(model)));
	}

	// 【中文】静态工厂方法。注意它**隐藏**（hide）了父类的同名静态方法——
	// 静态方法不存在多态，因此必须写全 SystemPromptTemplate.builder() 才能拿到本类的 Builder。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@link SystemPromptTemplate} 的建造者，继承自 {@link PromptTemplate.Builder}。
	 *
	 * <p>
	 * 之所以要把父类的四个配置方法**重新声明一遍**，是为了把返回类型从
	 * {@code PromptTemplate.Builder} 收窄为本类的 {@code Builder}（Java 的协变返回类型），
	 * 否则链式调用后就无法再调用本类特有的方法、也拿不到正确类型的 build() 结果。
	 * 这与 ChatOptions.Builder 用递归泛型解决的是同一类问题，只是手段更朴素。
	 */
	public static class Builder extends PromptTemplate.Builder {

		// 【中文】设置模板字符串。逻辑与父类完全一致，唯一差别是返回类型收窄为本类 Builder。
		// 之所以能直接访问 this.template，是因为父类字段声明为 protected。
		public Builder template(String template) {
			Assert.hasText(template, "template cannot be null or empty");
			this.template = template;
			return this;
		}

		// 【中文】设置模板资源文件（同样只为收窄返回类型）。
		public Builder resource(Resource resource) {
			Assert.notNull(resource, "resource cannot be null");
			this.resource = resource;
			return this;
		}

		// 【中文】设置预置变量。
		public Builder variables(Map<String, Object> variables) {
			Assert.notNull(variables, "variables cannot be null");
			Assert.noNullElements(variables.keySet(), "variables keys cannot be null");
			this.variables = variables;
			return this;
		}

		// 【中文】设置渲染器。
		public Builder renderer(TemplateRenderer renderer) {
			Assert.notNull(renderer, "renderer cannot be null");
			this.renderer = renderer;
			return this;
		}

		// 【中文】完成构建，返回类型是 SystemPromptTemplate（协变返回类型，父类声明的是 PromptTemplate）。
		@Override
		public SystemPromptTemplate build() {
			// 【中文】互斥约束与父类一致：template 与 resource 只能二选一，且必须选其一。
			if (this.template != null && this.resource != null) {
				throw new IllegalArgumentException("Only one of template or resource can be set");
			}
			else if (this.resource != null) {
				return new SystemPromptTemplate(this.resource, this.variables, this.renderer);
			}
			else if (this.template != null) {
				return new SystemPromptTemplate(this.template, this.variables, this.renderer);
			}
			else {
				// 【中文】两者都未设置：构建器状态不完整。
				throw new IllegalStateException("Neither template nor resource is set");
			}
		}

	}

}
