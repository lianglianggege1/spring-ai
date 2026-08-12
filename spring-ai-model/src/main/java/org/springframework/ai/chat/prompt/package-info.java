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
 * Prompt（提示）相关类型所在的包。
 *
 * <p>本包提供「提示模板」与「提示对象」两大体系：
 * <ul>
 *   <li>提示对象：{@link org.springframework.ai.chat.prompt.Prompt} 作为一次模型调用的核心入参，
 *       封装了消息列表（{@code List<Message>}）与可选的模型参数（{@link org.springframework.ai.chat.prompt.ChatOptions}）；</li>
 *   <li>提示模板：{@code PromptTemplate} 及其子类（{@code SystemPromptTemplate}、
 *       {@code AssistantPromptTemplate}、{@code ChatPromptTemplate} 等）实现模板与变量的分离渲染，
 *       并定义了 {@code PromptTemplateStringActions} / {@code PromptTemplateMessageActions} /
 *       {@code PromptTemplateChatActions} / {@code PromptTemplateActions} 一族动作接口，分别描述
 *       「渲染字符串」「造单条消息」「造多条消息」「造完整 Prompt」的能力。</li>
 * </ul>
 *
 * <p>整个包通过 JSpecify 的 {@code @NullMarked} 标注，表示包内 API 默认按「非空」契约设计，
 * 即方法参数与返回值除非显式标注 {@code @Nullable}，否则不应为 {@code null}。
 */
@NullMarked
package org.springframework.ai.chat.prompt;

import org.jspecify.annotations.NullMarked;
