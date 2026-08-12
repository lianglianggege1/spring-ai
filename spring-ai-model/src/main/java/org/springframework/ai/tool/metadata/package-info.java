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
 * 【中文说明】工具「元数据（Metadata）」包。
 *
 * <p>
 * 本包描述框架该<b>如何处理</b>某个工具的调用行为（不会发送给大模型）：
 * </p>
 * <ul>
 * <li>{@link org.springframework.ai.tool.metadata.ToolMetadata} —— 接口，核心属性
 * {@code returnDirect}（结果是否直接返回而不回灌给模型），并提供 {@code from(Method)}
 * 从 {@code @Tool} 注解方法解析元数据的便捷工厂。</li>
 * <li>{@link org.springframework.ai.tool.metadata.DefaultToolMetadata} —— 基于 record 的
 * 不可变默认实现，附带 Builder。</li>
 * </ul>
 *
 * <p>
 * 对比记忆：{@code definition} 包 = 给模型看的「说明书」；{@code metadata} 包 = 给框架看的「开关」。
 * </p>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.metadata;

import org.jspecify.annotations.NullMarked;
