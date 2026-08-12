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
 * Provides classes for observing image data.
 */
/**
 * 图像模型调用的可观测性（Observability）支持包，基于 Micrometer Observation 构建。
 * <p>
 * 主要类型：
 * <ul>
 * <li>{@link org.springframework.ai.image.observation.ImageModelObservationContext}
 * —— 观测上下文，承载请求与响应。</li>
 * <li>{@link org.springframework.ai.image.observation.ImageModelObservationConvention}
 * 及其默认实现 —— 决定观测名称与标签。</li>
 * <li>{@link org.springframework.ai.image.observation.ImageModelObservationDocumentation}
 * —— 文档化声明全部标签名。</li>
 * <li>{@link org.springframework.ai.image.observation.ImageModelPromptContentObservationHandler}
 * —— 可选处理器，把提示词内容打到日志。</li>
 * </ul>
 * 协作关系：模型调用时创建 Context → Convention 从中提取 KeyValues → 生成 metrics 与 tracing span。
 * <p>
 * 包上的 {@code @NullMarked}（JSpecify）表示本包内类型默认非空，仅显式标注 {@code @Nullable} 处可为 null。
 */
@NullMarked
package org.springframework.ai.image.observation;

import org.jspecify.annotations.NullMarked;
