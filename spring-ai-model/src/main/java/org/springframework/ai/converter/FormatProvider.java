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

/**
 * Implementations of this interface provide instructions for how the output of a language
 * generative should be formatted.
 *
 * @author Mark Pollack
 */
/**
 * 【中文说明】格式说明提供者：给出「模型输出应当长什么样」的自然语言指令。
 *
 * <p>
 * 这是一个单方法接口，作用是产出一段格式约束文本，调用方会把它拼接到用户 Prompt 的末尾，
 * 从而引导大模型按照可被程序解析的固定格式（JSON、逗号分隔列表等）作答。
 *
 * <p>
 * 它与 {@link StructuredOutputConverter} 的关系：FormatProvider 负责「发出去之前的约束」，
 * Converter 负责「收回来之后的解析」，两者一进一出配套使用。
 *
 * <p>
 * 例如 {@code ListOutputConverter#getFormat()} 会返回类似
 * “Respond with only a list of comma-separated values...” 的指令文本。
 */
public interface FormatProvider {

	/**
	 * Get the format of the output of a language generative.
	 * @return Returns a string containing instructions for how the output of a language
	 * generative should be formatted.
	 */
	/**
	 * 【中文说明】获取追加到 Prompt 中的输出格式说明。
	 *
	 * <p>
	 * 返回值是给「模型」看的自然语言指令（通常还内嵌 JSON Schema 或示例），而不是给程序解析的数据。
	 * 实现类应保证该说明与自身的解析逻辑严格对应，否则会出现「模型按 A 格式输出、程序按 B 格式解析」的错位。
	 * @return 描述期望输出格式的指令字符串
	 */
	String getFormat();

}
