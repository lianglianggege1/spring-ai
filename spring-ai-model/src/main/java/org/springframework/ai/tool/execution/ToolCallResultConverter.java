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

package org.springframework.ai.tool.execution;

import java.lang.reflect.Type;

import org.jspecify.annotations.Nullable;

/**
 * A functional interface to convert tool call results to a String that can be sent back
 * to the AI model.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用结果转换器：把工具方法返回的任意 Java 对象，转换成可以回传给 AI 模型的<b>字符串</b>。
 *
 * <p>
 * 存在的必要性：模型只能理解文本，而工具方法可能返回 POJO、List、Map、图片甚至 void。 该接口负责统一这层「Java 对象 →
 * 字符串（通常是 JSON）」的序列化。
 *
 * <p>
 * 这是一个 {@code @FunctionalInterface}，因此可用 Lambda 直接实现，例如： <pre>{@code
 * ToolCallResultConverter converter = (result, type) -> String.valueOf(result);
 * }</pre>
 *
 * <p>
 * 配置方式：可在 {@code @Tool(resultConverter = XxxConverter.class)} 上指定， 或在构建
 * {@code FunctionToolCallback} 时通过 {@code .toolCallResultConverter(...)} 传入。
 * 未指定时使用 {@link DefaultToolCallResultConverter}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see DefaultToolCallResultConverter
 */
@FunctionalInterface
public interface ToolCallResultConverter {

	/**
	 * Given an Object returned by a tool, convert it to a String compatible with the
	 * given class type.
	 */
	/**
	 * 【中文说明】执行转换。
	 * <p>
	 * 两个参数都标注了 {@code @Nullable}：
	 * <ul>
	 * <li>{@code result} 为 null，可能是工具本身返回了 null；</li>
	 * <li>{@code returnType} 为 null，表示调用方未提供返回类型信息（无法做类型相关的特殊处理）。</li>
	 * </ul>
	 * 使用 {@link Type} 而非 {@link Class} 是为了保留泛型信息（如 {@code List<Foo>}），
	 * 便于序列化器做更精确的处理。
	 * @param result 工具执行返回的原始对象，可为 null
	 * @param returnType 工具方法的返回类型（含泛型信息），可为 null
	 * @return 可回传给模型的字符串，不可为 null
	 */
	String convert(@Nullable Object result, @Nullable Type returnType);

}
