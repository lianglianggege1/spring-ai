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
 * 提示模板的「字符串渲染」接口——定义模板最基本的能力：把带占位符的模板文本
 * 连同变量表，渲染为一段最终的字符串。
 *
 * <p>这是所有提示模板动作接口的最底层契约，只关心「文本→文本」的渲染，不涉及任何
 * 消息或 {@link Prompt} 的组装。上层接口（如 {@code PromptTemplateActions}）在其之上
 * 扩展出组装能力。两个重载的 {@code render} 区别在于变量的来源：无参版本使用模板
 * 自身已绑定的变量，带参版本用传入的 {@code model} 覆盖。
 */
public interface PromptTemplateStringActions {

	// 使用模板自身已绑定的变量渲染，返回最终字符串。
	String render();

	// 用传入的 model 变量表覆盖模板中的占位符后渲染，返回最终字符串。
	String render(Map<String, Object> model);

}
