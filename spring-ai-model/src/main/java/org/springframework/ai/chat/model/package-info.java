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
 * 【中文说明】本包定义了 Spring AI 对话（Chat）模型的核心抽象与数据载体。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@code ChatModel}：对话模型主接口，提供同步 {@code call} 能力。</li>
 * <li>{@code StreamingChatModel}：流式对话接口，提供 {@code stream} 能力。</li>
 * <li>{@code ChatResponse}：一次对话调用的完整响应（含候选结果与元数据）。</li>
 * <li>{@code Generation}：单条生成结果，承载 AssistantMessage 与生成元数据。</li>
 * <li>{@code MessageAggregator}：把流式的增量响应聚合成一条完整消息。</li>
 * <li>{@code ToolContext}：工具调用时旁路传递的只读上下文。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示：本包内所有类型默认"非空"，
 * 只有显式标注 {@code @Nullable} 的位置才允许为 null。这为 IDE 与静态分析工具提供了空安全依据。
 */
@NullMarked
package org.springframework.ai.chat.model;

import org.jspecify.annotations.NullMarked;
