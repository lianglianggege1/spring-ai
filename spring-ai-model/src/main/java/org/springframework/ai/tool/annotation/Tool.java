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

package org.springframework.ai.tool.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

/**
 * Marks a method as a tool in Spring AI.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code @Tool} 注解：把一个普通的 Java 方法标记为可供大模型（LLM）调用的「工具」。
 *
 * <p>
 * <b>用途：</b>Spring AI 通过扫描带此注解的方法，自动生成工具的元信息（名称、描述、JSON Schema
 * 入参格式），并在对话过程中把这些信息发给模型；当模型决定调用某个工具时，框架再反射执行对应方法。
 * </p>
 *
 * <p>
 * <b>关键属性：</b>
 * </p>
 * <ul>
 * <li>{@link #name()} —— 工具名，缺省用方法名。建议只用字母/数字/下划线/连字符/点，避免 OpenAI 等模型解析报错。</li>
 * <li>{@link #description()} —— 工具用途描述，这是模型判断「何时该调用这个工具」的<b>核心依据</b>，应尽量写清楚。</li>
 * <li>{@link #returnDirect()} —— 为 true 时工具结果直接返回给调用方，不再回灌给模型做二次总结。</li>
 * <li>{@link #resultConverter()} —— 指定把方法返回值转换成字符串的转换器，默认 {@link DefaultToolCallResultConverter}。</li>
 * </ul>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * class WeatherTools {
 *     @Tool(description = "根据城市名查询当前天气")
 *     String getWeather(@ToolParam(description = "城市名称") String city) {
 *         return ...;
 *     }
 * }
 * // 注册：ChatClient.builder(model).build().prompt().tools(new WeatherTools())...
 * }</pre>
 *
 * <p>
 * <b>元注解含义：</b>{@code @Target} 允许标注在方法上，也允许标注在其他注解上（组合注解场景）；
 * {@code @Retention(RUNTIME)} 保证运行期可通过反射读取；{@code @Documented} 使其出现在 javadoc 中。
 * </p>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see ToolParam
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

	/**
	 * The name of the tool. If not provided, the method name will be used.
	 * <p>
	 * For maximum compatibility across different LLMs, it is recommended to use only
	 * alphanumeric characters, underscores, hyphens, and dots in tool names. Using spaces
	 * or special characters may cause issues with some LLMs (e.g., OpenAI).
	 * </p>
	 * <p>
	 * Examples of recommended names: "get_weather", "search-docs", "tool.v1"
	 * </p>
	 * <p>
	 * Examples of names that may cause compatibility issues: "get weather" (contains
	 * space), "tool()" (contains parentheses)
	 * </p>
	 */
	// 中文：工具名称。留空则回退使用方法名（见 ToolUtils#getToolName）。
	// 为兼容各家 LLM，建议仅使用「字母、数字、下划线、连字符、点」，
	// 例如 get_weather / search-docs / tool.v1；含空格或括号可能导致部分模型（如 OpenAI）报错。
	String name() default "";

	/**
	 * The description of the tool. If not provided, the method name will be used.
	 */
	// 中文：工具描述，供模型理解"这个工具能做什么、什么时候该调用"。
	// 留空则回退使用方法名，但强烈建议显式填写，描述质量直接影响模型选择工具的准确率。
	String description() default "";

	/**
	 * Whether the tool result should be returned directly or passed back to the model.
	 */
	// 中文：是否"直接返回"。
	// false（默认）：工具执行结果作为 ToolResponseMessage 回灌给模型，由模型继续生成自然语言回答；
	// true：结果直接返回给调用方，跳过再次请求模型，适合结果本身就是最终答案的场景（可省一次调用）。
	boolean returnDirect() default false;

	/**
	 * The class to use to convert the tool call result to a String.
	 */
	// 中文：结果转换器类型。工具方法的返回值需要序列化成字符串才能交给模型，
	// 默认 DefaultToolCallResultConverter 使用 JSON 序列化；可自定义实现以适配特殊格式。
	// 注意这里声明的是 Class 而非实例，因为注解属性只能是编译期常量，框架会在运行期反射实例化。
	Class<? extends ToolCallResultConverter> resultConverter() default DefaultToolCallResultConverter.class;

}
