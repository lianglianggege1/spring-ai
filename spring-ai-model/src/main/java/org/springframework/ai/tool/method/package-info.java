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
 * 【中文说明】工具「方法（Method）」包 —— {@code @Tool} 声明式方案的执行核心。
 *
 * <ul>
 * <li>{@link org.springframework.ai.tool.method.MethodToolCallback} —— 通过反射调用
 * 「对象 + 方法」来执行工具，负责 JSON 参数绑定、{@code ToolContext} 注入、
 * 方法可见性处理与反射异常包装。</li>
 * <li>{@link org.springframework.ai.tool.method.MethodToolCallbackProvider} —— 扫描一批业务对象，
 * 把其中所有 {@code @Tool} 方法批量装配成 {@code ToolCallback}，并做重名等校验。
 * 支持 Spring AOP 代理解包。</li>
 * </ul>
 *
 * <p>
 * <b>使用前提：</b>编译时需开启 {@code -parameters} 选项以保留方法参数名，
 * 否则参数绑定会因拿不到真实参数名（变成 arg0/arg1）而失败。
 * </p>
 *
 * <p>
 * 包级 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 * </p>
 */
@NullMarked
package org.springframework.ai.tool.method;

import org.jspecify.annotations.NullMarked;
