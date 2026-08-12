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

/**
 * 提示模板的「动作」接口——负责把渲染后的内容组装成一个完整的 {@link Prompt}。
 *
 * <p>它继承自 {@link PromptTemplateStringActions}（只负责渲染字符串），在此基础上补齐了
 * 把字符串进一步包装成 {@link Prompt} 的能力。四个重载的 {@code create} 方法通过不同的
 * 参数组合，覆盖了「是否传入模板变量」「是否附带模型参数（{@link ChatOptions}）」两种维度：
 * <ul>
 *   <li>无参：仅用模板渲染（变量取模板自身绑定的默认值），构造纯消息的 Prompt；</li>
 *   <li>带 {@link ChatOptions}：在消息之外附加模型级参数（如温度、模型名）；</li>
 *   <li>带 {@code model} 变量表：覆盖模板中的占位符；</li>
 *   <li>两者皆带：既填变量又附模型参数。</li>
 * </ul>
 */
public interface PromptTemplateActions extends PromptTemplateStringActions {

	// 仅渲染模板（使用模板自身绑定的变量），并把结果包装成 Prompt。
	Prompt create();

	// 渲染后包装成 Prompt，并附带模型级参数 modelOptions（如温度、模型名）。
	Prompt create(ChatOptions modelOptions);

	// 用传入的 model 变量表覆盖模板占位符，再包装成 Prompt。
	Prompt create(Map<String, Object> model);

	// 同时传入变量表与模型参数：既填占位符又附 ChatOptions。
	Prompt create(Map<String, Object> model, ChatOptions modelOptions);

}
