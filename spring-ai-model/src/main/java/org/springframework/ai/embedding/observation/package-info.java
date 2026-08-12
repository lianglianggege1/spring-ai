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
 * Provides the API for embedding observations.
 */
/**
 * 嵌入模型的<b>可观测性（Observability）</b>支持包，基于 Micrometer Observation 构建。
 *
 * <p>
 * 四个角色分工明确：
 * <ul>
 * <li>{@link org.springframework.ai.embedding.observation.EmbeddingModelObservationContext}：
 * 上下文，承载请求与响应数据；</li>
 * <li>{@link org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention}
 * 及其默认实现：约定，决定观测名与标签；</li>
 * <li>{@link org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation}：
 * 文档化定义，集中声明所有标签键；</li>
 * <li>{@link org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler}：
 * 处理器，额外产出 token 用量指标。</li>
 * </ul>
 *
 * <p>
 * 整体遵循 OpenTelemetry 的 GenAI 语义约定，可直接对接 Prometheus、Zipkin 等后端。
 *
 * <p>
 * {@code @NullMarked} 表示本包内类型默认不可为 null。
 */
@NullMarked
package org.springframework.ai.embedding.observation;

import org.jspecify.annotations.NullMarked;
