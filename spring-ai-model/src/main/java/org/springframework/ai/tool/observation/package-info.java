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
 * Provides the API for chat client advisors observations.
 */
/**
 * 【中文说明】工具调用的可观测性（Observability）支持包，基于 Micrometer Observation 构建。
 *
 * <p>
 * 四个角色协同工作：
 * <ul>
 * <li>{@link org.springframework.ai.tool.observation.ToolCallingObservationContext}：
 * 数据载体，承载工具定义、调用 ID、入参与结果；</li>
 * <li>{@link org.springframework.ai.tool.observation.ToolCallingObservationConvention}
 * 及其默认实现：把上下文翻译成指标名与标签；</li>
 * <li>{@link org.springframework.ai.tool.observation.ToolCallingObservationDocumentation}：
 * 集中声明所有标签名，作为「单一事实来源」并可自动生成文档；</li>
 * <li>{@link org.springframework.ai.tool.observation.ToolCallingContentObservationFilter}：
 * 可选过滤器，用于额外记录入参与结果内容（含敏感数据，默认关闭）。</li>
 * </ul>
 *
 * <p>
 * 注意：上方英文注释写的是「chat client advisors observations」，与本包实际内容不符，
 * 应为复制粘贴遗留的笔误——本包实际服务于<b>工具调用</b>观测。
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 */
@NullMarked
package org.springframework.ai.tool.observation;

import org.jspecify.annotations.NullMarked;
