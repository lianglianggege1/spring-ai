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
 * 【中文说明】Spring AI 工具调用（Tool Calling）体系的根包。
 *
 * <p>
 * 本包定义了两个最核心的抽象：
 * <ul>
 * <li>{@link org.springframework.ai.tool.ToolCallback}：一个可被 AI 模型触发执行的工具；</li>
 * <li>{@link org.springframework.ai.tool.ToolCallbackProvider}：批量提供工具回调的来源。</li>
 * </ul>
 *
 * <p>
 * 子包职责划分：
 * <ul>
 * <li>{@code annotation}：{@code @Tool}/{@code @ToolParam} 声明式注解；</li>
 * <li>{@code definition}：工具定义（名称、描述、入参 Schema）；</li>
 * <li>{@code metadata}：工具元数据（如 returnDirect）；</li>
 * <li>{@code function} / {@code method}：基于函数式接口、基于反射方法的两类实现；</li>
 * <li>{@code execution}：执行期的结果转换与异常处理；</li>
 * <li>{@code resolution}：按名称解析出对应的 ToolCallback；</li>
 * <li>{@code observation}：基于 Micrometer 的可观测性支持。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示：本包内所有类型默认<b>不可为 null</b>， 只有显式标注
 * {@code @Nullable} 的位置才允许 null。
 */
@NullMarked
package org.springframework.ai.tool;

import org.jspecify.annotations.NullMarked;
