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
 * Provides converters for transforming AI model text outputs into structured Java types.
 *
 * <p>
 * The output of AI models traditionally arrives as a {@code String}, even if you ask for
 * the reply to be in JSON. This package provides specialized converters that employ
 * meticulously crafted prompts and parsing logic to convert text responses into usable
 * data structures for application integration.
 *
 * <p>
 * For detailed documentation and usage examples, see the <a href=
 * "https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html">Structured
 * Output Converter Reference Guide</a>.
 */
/**
 * 【中文说明】结构化输出转换包：把 AI 模型返回的文本转成结构化的 Java 类型。
 *
 * <p>
 * 背景：大模型的输出本质上永远是一段 {@code String}，即使你在提示词里要求「返回 JSON」，
 * 拿到的仍是字符串，且常伴随代码围栏、思维链标签等噪声。本包提供的转换器通过
 * 「精心设计的提示词 + 解析逻辑」这一组合拳，把文本可靠地还原为可直接使用的数据结构。
 *
 * <p>
 * 包内主要成员：
 * <ul>
 * <li>{@link org.springframework.ai.converter.StructuredOutputConverter} —— 核心接口，
 * 兼具「格式约束」与「文本解析」两种能力</li>
 * <li>{@link org.springframework.ai.converter.FormatProvider} —— 产出追加到 Prompt
 * 的格式说明</li>
 * <li>{@link org.springframework.ai.converter.BeanOutputConverter} —— 转为任意
 * JavaBean/record，内部生成 JSON Schema</li>
 * <li>{@link org.springframework.ai.converter.MapOutputConverter} /
 * {@link org.springframework.ai.converter.ListOutputConverter} —— 转为 Map / List</li>
 * <li>{@link org.springframework.ai.converter.ResponseTextCleaner} 及其实现 —— 解析前的
 * 文本清洗责任链</li>
 * </ul>
 *
 * <p>
 * {@code @NullMarked} 表示本包默认所有类型「非空」，只有显式标注 {@code @Nullable} 的位置才允许 null。
 */
@NullMarked
package org.springframework.ai.converter;

import org.jspecify.annotations.NullMarked;
