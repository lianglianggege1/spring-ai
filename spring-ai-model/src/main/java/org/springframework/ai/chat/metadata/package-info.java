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
 * 【中文说明】聊天元数据（Chat Metadata）包：承载模型响应中除正文之外的各类附加信息。
 *
 * <p>
 * 按粒度可分为两层：
 * <ul>
 * <li>{@link org.springframework.ai.chat.metadata.ChatResponseMetadata}——整次响应级别
 * （请求 id、模型名、token 用量 {@link org.springframework.ai.chat.metadata.Usage}、
 * 限流 {@link org.springframework.ai.chat.metadata.RateLimit}）；</li>
 * <li>{@link org.springframework.ai.chat.metadata.ChatGenerationMetadata}——单条候选结果级别
 * （结束原因、内容过滤标记）。</li>
 * </ul>
 *
 * <p>
 * 本包大量使用"空对象模式"（{@code EmptyUsage}、{@code EmptyRateLimit}、
 * {@code ChatGenerationMetadata.NULL}），用零值实例代替 null，让调用方免于层层判空。
 *
 * <p>
 * 包上的 {@code @NullMarked}（JSpecify）表示包内类型默认不可为 null，仅显式标注
 * {@code @Nullable} 处允许为 null。
 */
@NullMarked
package org.springframework.ai.chat.metadata;

import org.jspecify.annotations.NullMarked;
