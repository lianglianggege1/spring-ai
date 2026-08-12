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

package org.springframework.ai.tool.augment;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * An event that encapsulates the augmented arguments extracted from a tool input, along
 * with the associated tool definition and raw input data.
 *
 * @param <T> The type of the augmented arguments record.
 * @param toolDefinition The tool definition associated with the event.
 * @param rawInput The raw input data as a string.
 * @param arguments The augmented arguments extracted from the input.
 * @author Christian Tzolov
 */
/**
 * 【中文说明】{@code AugmentedArgumentEvent}：「增强参数事件」，是一个不可变的事件载体（record）。
 *
 * <p>
 * <b>用途：</b>当 {@link AugmentedToolCallback} 从模型传入的 JSON 中提取出「额外增强参数」后，
 * 会把这些参数打包成本事件，回调给用户注册的 {@code Consumer} 处理。
 * 常见于让模型在调用工具时额外附带「调用理由」「置信度」「用户意图」等旁路信息，
 * 这些信息由业务侧消费（如记录审计日志），但不传给真正的工具方法。
 * </p>
 *
 * <p>
 * <b>三个组件：</b>
 * </p>
 * <ul>
 * <li>{@code toolDefinition} —— 触发本事件的工具定义（即增强后的定义），用于知道是"哪个工具"。</li>
 * <li>{@code rawInput} —— 模型给出的<b>原始</b> JSON 输入字符串（未做任何删减），便于排查与审计。</li>
 * <li>{@code arguments} —— 反序列化后的增强参数对象，类型为泛型 {@code T}；
 * 标注 {@code @Nullable} 是因为模型可能并未提供这些字段，导致解析结果为 null。</li>
 * </ul>
 *
 * <p>
 * <b>泛型 {@code <T>}：</b>增强参数的 record 类型。注意此处未加 {@code extends Record} 约束，
 * 而在 {@link AugmentedToolCallback} 中才做该约束。
 * </p>
 *
 * @param <T> 增强参数的记录类型
 * @author Christian Tzolov
 * @see AugmentedToolCallback
 */
public record AugmentedArgumentEvent<T>(ToolDefinition toolDefinition, String rawInput, @Nullable T arguments) {
}
