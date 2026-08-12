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
 * 【中文说明】JSON 解析相关的工具包。
 *
 * <p>
 * 本包目前仅包含 {@code JsonParser}——一个已标记 {@code @Deprecated(forRemoval = true)} 的过渡适配层，
 * 其所有方法都转调 {@code JacksonUtils} 与 {@code JsonHelper}。阅读源码时若在其他模块看到
 * {@code JsonParser.xxx()} 调用，可直接对照新类理解其行为。
 *
 * <p>
 * 子包 {@code schema} 承载 JSON Schema 生成能力，是工具调用（Function Calling）声明参数结构的基础。
 *
 * <p>
 * 包上的 {@link org.jspecify.annotations.NullMarked @NullMarked} 表示默认非空语义。
 */
@NullMarked
package org.springframework.ai.util.json;

import org.jspecify.annotations.NullMarked;
