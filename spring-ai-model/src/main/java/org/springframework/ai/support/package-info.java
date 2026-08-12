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
 * 【中文说明】Spring AI 的通用支撑工具包。
 *
 * <p>
 * 本包收纳跨模块复用的静态工具类，均为 {@code final class} + 私有构造器的无状态工具：
 * <ul>
 * <li>{@code ToolCallbacks}：把带 {@code @Tool} 注解的普通对象转换为 {@code ToolCallback} 数组。</li>
 * <li>{@code UsageCalculator}：在工具调用循环、重试循环、流式聚合等多轮场景中累加 token 用量。</li>
 * </ul>
 *
 * <p>
 * 包上的 {@link org.jspecify.annotations.NullMarked @NullMarked} 表示本包默认非空，
 * 只有显式标注 {@code @Nullable} 之处才允许 null。
 */
@NullMarked
package org.springframework.ai.support;

import org.jspecify.annotations.NullMarked;
