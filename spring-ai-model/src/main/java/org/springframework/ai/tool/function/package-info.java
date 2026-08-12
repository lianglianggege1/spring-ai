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
 * 【中文说明】工具「函数式（Function）」包。
 *
 * <p>
 * 本包提供把 Java 函数式接口包装成 LLM 可调用工具的能力：
 * </p>
 * <ul>
 * <li>{@link org.springframework.ai.tool.function.FunctionToolCallback} —— 支持
 * {@code Function}、{@code BiFunction}、{@code Supplier}、{@code Consumer} 四种形态
 * （内部统一适配为 {@code BiFunction<I, ToolContext, O>}），通过 Builder 创建。</li>
 * </ul>
 *
 * <p>
 * <b>与 {@code tool.method} 包的对比：</b>
 * </p>
 * <ul>
 * <li>本包 = <b>编程式</b>注册：适合 Lambda、动态构造工具的场景。</li>
 * <li>{@code tool.method} 包 = <b>声明式</b>注册：适合用 {@code @Tool} 注解标注已有业务方法。</li>
 * </ul>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.function;

import org.jspecify.annotations.NullMarked;
