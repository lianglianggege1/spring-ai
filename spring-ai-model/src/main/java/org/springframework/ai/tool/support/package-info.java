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
 * 【中文说明】工具「支撑（Support）」包 —— 框架内部使用的静态工具类集合。
 *
 * <ul>
 * <li>{@link org.springframework.ai.tool.support.ToolUtils} —— 从 {@code Method} 上解析
 * 工具名、描述、returnDirect、结果转换器；以及检测工具重名。所有解析都遵循
 * 「读 {@code @Tool} 注解 → 取不到走兜底」的统一套路。</li>
 * <li>{@link org.springframework.ai.tool.support.ToolDefinitions} —— 把 Java 方法一键翻译成
 * {@code ToolDefinition}，其中入参 JSON Schema 由反射方法签名自动生成。</li>
 * </ul>
 *
 * <p>
 * 两个类都是 {@code final} + 私有构造器的纯静态工具类，不应被继承或实例化。
 * </p>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.support;

import org.jspecify.annotations.NullMarked;
