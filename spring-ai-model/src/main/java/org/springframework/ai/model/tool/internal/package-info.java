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
 * 【中文说明】本包是工具调用（Tool Calling）相关的<b>内部实现</b>包。
 *
 * <p>
 * 包名中的 {@code internal} 表明：这里的类属于框架内部基础设施，
 * <b>不属于公开 API</b>，可能在小版本间发生不兼容变更，业务代码不应直接依赖。
 *
 * <p>
 * 当前成员：{@code ToolCallReactiveContextHolder} —— 借助 ThreadLocal 在阻塞式工具调用
 * 与 Reactor 响应式上下文之间传递上下文信息。
 *
 * <p>
 * 包级 {@code @NullMarked}：本包内类型默认非空，仅显式标注 {@code @Nullable} 处可为 null。
 */

@NullMarked
package org.springframework.ai.model.tool.internal;

import org.jspecify.annotations.NullMarked;
