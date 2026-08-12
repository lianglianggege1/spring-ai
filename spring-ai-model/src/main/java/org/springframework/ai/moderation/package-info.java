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
 * 【中文说明】内容审核（Moderation）包：调用 AI 审核模型判断文本是否违规。
 *
 * <p>
 * 用途：在把用户输入送给大模型之前、或把模型输出返回给用户之前，先做一次合规检查，
 * 识别仇恨、暴力、自残、色情、骚扰等有害内容，是 AI 应用安全防护的常见一环。
 *
 * <p>
 * 本包遵循 Spring AI 统一的「Prompt → Model → Response」调用范式，核心类型如下：
 * <ul>
 * <li>{@link org.springframework.ai.moderation.ModerationModel} —— 模型接口，
 * 入参 ModerationPrompt，出参 ModerationResponse</li>
 * <li>{@link org.springframework.ai.moderation.ModerationPrompt} /
 * {@link org.springframework.ai.moderation.ModerationMessage} —— 请求侧：待审核文本 + 选项</li>
 * <li>{@link org.springframework.ai.moderation.ModerationResponse} /
 * {@link org.springframework.ai.moderation.Generation} —— 响应侧的两层包装</li>
 * <li>{@link org.springframework.ai.moderation.Moderation} —— 审核结果聚合，含多条
 * {@link org.springframework.ai.moderation.ModerationResult}</li>
 * <li>{@link org.springframework.ai.moderation.Categories}（是否命中各违规类别，布尔）与
 * {@link org.springframework.ai.moderation.CategoryScores}（各类别置信度分数，double）</li>
 * </ul>
 *
 * <p>
 * 典型调用链：
 * {@code ModerationPrompt -> ModerationModel.call() -> ModerationResponse -> Generation
 * -> Moderation -> ModerationResult -> Categories / CategoryScores}
 *
 * <p>
 * {@code @NullMarked} 表示本包默认所有类型「非空」，只有显式标注 {@code @Nullable} 处才允许 null。
 */
@NullMarked
package org.springframework.ai.moderation;

import org.jspecify.annotations.NullMarked;
