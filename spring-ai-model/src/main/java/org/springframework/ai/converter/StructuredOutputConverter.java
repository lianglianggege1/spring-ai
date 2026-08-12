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

package org.springframework.ai.converter;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts the (raw) LLM output into a structured responses of type. The
 * {@link FormatProvider#getFormat()} method should provide the LLM prompt description of
 * the desired format.
 *
 * @param <T> Specifies the desired response type.
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Filip Hrisafov
 */
/**
 * 【中文说明】结构化输出转换器：把大模型返回的「原始文本」转换成强类型的 Java 对象。
 *
 * <p>
 * 该接口是 Spring AI「结构化输出」机制的核心抽象，由两个能力组合而成：
 * <ul>
 * <li>{@link org.springframework.core.convert.converter.Converter Converter&lt;String,
 * T&gt;}：提供 {@code T convert(String source)}，负责「解析」——把模型吐出的字符串反序列化为 T。</li>
 * <li>{@link FormatProvider}：提供 {@code getFormat()}，负责「约束」——生成一段追加到 Prompt
 * 末尾的格式说明，告诉模型应当按什么格式输出。</li>
 * </ul>
 * 二者配合形成闭环：先用 getFormat() 引导模型产出规范文本，再用 convert() 把文本还原成对象。
 *
 * <p>
 * 典型用法（通常由 ChatClient 的 {@code .entity(...)} 内部调用，无需手写）：
 *
 * <pre>{@code
 * StructuredOutputConverter<ActorFilms> converter = new BeanOutputConverter<>(ActorFilms.class);
 * String prompt = "列出成龙的5部电影。" + converter.getFormat(); // 追加格式说明
 * String raw = chatModel.call(prompt);                        // 模型返回原始字符串
 * ActorFilms films = converter.convert(raw);                  // 解析为强类型对象
 * }</pre>
 *
 * <p>
 * 泛型参数 {@code <T>} 表示期望得到的目标响应类型。
 *
 * <p>
 * 主要实现类：{@code BeanOutputConverter}（任意 JavaBean/record）、{@code MapOutputConverter}
 * （Map）、{@code ListOutputConverter}（逗号分隔列表）。
 *
 * @param <T> 期望的响应类型
 */
public interface StructuredOutputConverter<T> extends Converter<String, T>, FormatProvider {

	/**
	 * Constant for when there is no JSON schema available.
	 */
	// 常量：表示「无可用 JSON Schema」，用空字符串而非 null，避免调用方做空值判断
	String NO_JSON_SCHEMA = "";

	/**
	 * Returns the JSON schema for the structured output of an LLM call.
	 * @return the JSON schema or {@link StructuredOutputConverter#NO_JSON_SCHEMA} if not
	 * available
	 * @since 2.0.0
	 */
	/**
	 * 【中文说明】返回本次结构化输出对应的 JSON Schema 文本。
	 *
	 * <p>
	 * 部分模型（如 OpenAI 的 structured outputs / response_format）支持直接传入 JSON Schema
	 * 做「服务端强约束」，比单纯在 Prompt 里写格式说明更可靠。该方法即为此类场景提供 Schema。
	 *
	 * <p>
	 * 这里给出的是 default 默认实现，返回 {@link #NO_JSON_SCHEMA}（空串），表示当前转换器
	 * 无法提供 Schema；需要该能力的实现类（如 {@code BeanOutputConverter}）会覆写它。
	 * 使用 default 方法可以在新增该能力时不破坏已有实现的二进制兼容性。
	 * @return JSON Schema 字符串；不可用时返回 {@link #NO_JSON_SCHEMA}
	 */
	default String getJsonSchema() {
		return NO_JSON_SCHEMA;
	}

}
