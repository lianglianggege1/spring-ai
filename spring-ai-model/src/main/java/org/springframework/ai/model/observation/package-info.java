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
 * Provides classes for observing model data.
 */

/**
 * 【中文说明】本包提供 AI 模型调用的<b>可观测性（Observability）</b>支持，
 * 基于 Micrometer Observation / Tracing / Metrics 体系构建。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@code ModelObservationContext} —— 观测上下文，承载请求、响应与操作元数据；</li>
 * <li>{@code ModelUsageMetricsGenerator} —— 将 token 用量转换为 Micrometer 指标；</li>
 * <li>{@code ErrorLoggingObservationHandler} —— 出错时输出带链路信息的错误日志。</li>
 * </ul>
 *
 * <p>
 * 借助这些组件，可以监控模型调用的耗时、成功率与 token 成本，并把日志、指标、链路三者打通。
 *
 * <p>
 * 包级 {@code @NullMarked}：表示本包内类型默认非空，仅显式标注 {@code @Nullable} 处可为 null。
 */

@NullMarked
package org.springframework.ai.model.observation;

import org.jspecify.annotations.NullMarked;
