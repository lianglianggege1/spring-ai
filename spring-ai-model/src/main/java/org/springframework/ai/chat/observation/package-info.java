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
 * Provides the API for chat observations.
 */
/**
 * 【中文说明】本包提供对话模型的"可观测性（Observability）"支持，基于 Micrometer Observation 构建。
 *
 * <p>
 * 核心概念对应关系：
 * <ul>
 * <li>{@code ChatModelObservationContext}：一次对话调用的观测上下文，承载请求 Prompt 与响应
 * ChatResponse。</li>
 * <li>{@code ChatModelObservationConvention}：约定接口，决定观测名称与标签（KeyValues）。</li>
 * <li>{@code DefaultChatModelObservationConvention}：默认约定实现，按 OpenTelemetry GenAI
 * 语义规范生成标签。</li>
 * <li>{@code ChatModelObservationDocumentation}：以枚举形式声明所有标签名，供文档生成与测试校验。</li>
 * <li>{@code ChatModelMeterObservationHandler}：把 token 用量转换为 Micrometer 指标。</li>
 * <li>{@code ChatModelPromptContentObservationHandler} /
 * {@code ChatModelCompletionObservationHandler}：把提示词与生成内容输出到日志（默认关闭，
 * 因为可能包含敏感数据）。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示本包内类型默认非空，只有显式标注
 * {@code @Nullable} 的位置才允许为 null。
 */
@NullMarked
package org.springframework.ai.chat.observation;

import org.jspecify.annotations.NullMarked;
