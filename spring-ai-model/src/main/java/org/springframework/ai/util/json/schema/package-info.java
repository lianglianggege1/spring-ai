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
 * 【中文说明】JSON Schema 生成与加工工具包。
 *
 * <p>
 * 本包是「工具调用」与「结构化输出」的底层支撑：把 Java 类型、方法签名及其注解，翻译成模型可理解的
 * JSON Schema，用于声明工具参数结构或约束模型输出格式。底层基于 victools jsonschema-generator。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@code JsonSchemaGenerator}：核心入口，提供 generateForMethodInput / generateForType 两个方法。</li>
 * <li>{@code JsonSchemaUtils}：Schema 加工工具，负责 $defs 上提、引用重写、外部 Schema 规范化。</li>
 * <li>{@code AbstractSpringAiSchemaModule}：模板方法基类，统一“描述解析”与“必填判定”的流程。</li>
 * <li>{@code SpringAiSchemaModule}：基于 {@code @ToolParam} 注解的默认实现。</li>
 * <li>{@code SchemaType}：标准 JSON Schema 与 OpenAPI Schema 的方言选择枚举。</li>
 * </ul>
 *
 * <p>
 * 必填判定的注解优先级统一为：{@code @ToolParam} &gt; {@code @JsonProperty} &gt; {@code @Schema} &gt;
 * 空安全标记 &gt; 全局默认（必填）。
 *
 * <p>
 * 包上的 {@link org.jspecify.annotations.NullMarked @NullMarked} 表示默认非空语义。
 */
@NullMarked
package org.springframework.ai.util.json.schema;

import org.jspecify.annotations.NullMarked;
