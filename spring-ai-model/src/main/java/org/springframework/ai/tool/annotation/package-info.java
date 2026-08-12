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
 * 【中文说明】工具「注解」包。
 *
 * <p>
 * 本包提供把普通 Java 方法声明为 LLM 可调用工具所需的注解：
 * </p>
 * <ul>
 * <li>{@link org.springframework.ai.tool.annotation.Tool} —— 标记方法为工具，定义名称、描述、
 * 是否直接返回、结果转换器。</li>
 * <li>{@link org.springframework.ai.tool.annotation.ToolParam} —— 标记工具方法的参数或字段，
 * 定义是否必填与参数描述。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示：本包内所有类型默认<b>不可为 null</b>，
 * 只有显式标注 {@code @Nullable} 的地方才允许 null。这为静态分析工具提供空安全约束。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.annotation;

import org.jspecify.annotations.NullMarked;
