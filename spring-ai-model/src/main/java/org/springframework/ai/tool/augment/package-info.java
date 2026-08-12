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
 * 【中文说明】工具「增强（Augment）」包。
 *
 * <p>
 * <b>解决的问题：</b>在不修改原工具代码的前提下，往工具入参 schema 里额外插入若干字段，
 * 让模型调用工具时顺带提供旁路信息（如「调用理由」「置信度」「意图分类」），
 * 由业务侧统一消费用于审计、监控或路由。
 * </p>
 *
 * <p>
 * <b>四个组成部分：</b>
 * </p>
 * <ul>
 * <li>{@link org.springframework.ai.tool.augment.ToolInputSchemaAugmenter} —— schema 增强器，
 * 把 record 字段合并进已有 JSON Schema；内含 {@code AugmentedArgumentType} 元信息 record。</li>
 * <li>{@link org.springframework.ai.tool.augment.AugmentedToolCallback} —— 单个工具的<b>装饰器</b>：
 * 改写 schema、提取增强参数并回调消费者、可选剔除后转发给原工具。</li>
 * <li>{@link org.springframework.ai.tool.augment.AugmentedToolCallbackProvider} —— provider 层装饰器，
 * 批量为一组工具统一施加同样的增强。</li>
 * <li>{@link org.springframework.ai.tool.augment.AugmentedArgumentEvent} —— 回调给消费者的事件载体，
 * 携带工具定义、原始 JSON 输入与解析后的增强参数。</li>
 * </ul>
 *
 * <p>
 * <b>约束：</b>增强参数类型必须是 Java {@code record}，因为实现依赖
 * {@code getRecordComponents()} 反射字段名与类型，并从对应私有字段上读取 {@code @ToolParam} 注解。
 * </p>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.augment;

import org.jspecify.annotations.NullMarked;
