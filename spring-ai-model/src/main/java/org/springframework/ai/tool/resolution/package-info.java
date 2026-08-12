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
 * 【中文说明】工具「解析」包：负责把模型返回的<b>工具名称</b>映射为可执行的
 * {@link org.springframework.ai.tool.ToolCallback}。
 *
 * <p>
 * 核心成员：
 * <ul>
 * <li>{@link org.springframework.ai.tool.resolution.ToolCallbackResolver}：解析器接口，
 * 约定「找不到时返回 null」；</li>
 * <li>{@link org.springframework.ai.tool.resolution.StaticToolCallbackResolver}：
 * 基于内存 Map 的静态注册表实现；</li>
 * <li>{@link org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver}：
 * 责任链实现，按顺序委派给多个解析器；</li>
 * <li>{@link org.springframework.ai.tool.resolution.TypeResolverHelper}：泛型类型解析工具，
 * 用于在类型擦除后反推函数式接口的入参/出参类型。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 */
@NullMarked
package org.springframework.ai.tool.resolution;

import org.jspecify.annotations.NullMarked;
