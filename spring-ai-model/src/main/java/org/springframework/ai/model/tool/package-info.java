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
 * 【中文说明】本包是 Spring AI「工具调用（Tool Calling / Function Calling）」在<b>模型层</b>的核心抽象。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@link org.springframework.ai.model.tool.ToolCallingChatOptions} 及其默认实现
 * {@link org.springframework.ai.model.tool.DefaultToolCallingChatOptions}：带工具配置的聊天选项。</li>
 * <li>{@link org.springframework.ai.model.tool.ToolCallingManager} 及其默认实现
 * {@link org.springframework.ai.model.tool.DefaultToolCallingManager}：负责解析工具定义、执行工具调用。</li>
 * <li>{@link org.springframework.ai.model.tool.ToolExecutionResult} 及其默认实现
 * {@link org.springframework.ai.model.tool.DefaultToolExecutionResult}：工具执行结果与会话历史。</li>
 * <li>{@link org.springframework.ai.model.tool.ToolExecutionEligibilityChecker}：判定是否需要触发工具执行。</li>
 * <li>{@link org.springframework.ai.model.tool.StructuredOutputChatOptions}：结构化输出（JSON Schema）选项抽象。</li>
 * </ul>
 *
 * <p>
 * 包上的 {@code @NullMarked}（JSpecify）表示：本包内所有类型默认<b>不可为 null</b>，
 * 只有显式标注 {@code @Nullable} 的位置才允许为 null，便于静态分析工具做空指针检查。
 */
@NullMarked
package org.springframework.ai.model.tool;

import org.jspecify.annotations.NullMarked;
