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
 * 【中文说明】工具「定义（Definition）」包。
 *
 * <p>
 * 本包定义了工具的对外描述契约 —— 即最终发送给大模型的那份「工具说明书」：
 * </p>
 * <ul>
 * <li>{@link org.springframework.ai.tool.definition.ToolDefinition} —— 接口，包含
 * name（唯一名称）、description（用途描述）、inputSchema（入参 JSON Schema）三要素。</li>
 * <li>{@link org.springframework.ai.tool.definition.DefaultToolDefinition} —— 基于 record 的
 * 不可变默认实现，附带 Builder，支持由工具名自动推导描述。</li>
 * </ul>
 *
 * <p>
 * 注意区分：本包只管「描述」，不管「执行」；执行由 {@code ToolCallback} 及其实现负责。
 * </p>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.definition;

import org.jspecify.annotations.NullMarked;
