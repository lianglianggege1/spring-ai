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

package org.springframework.ai.tool.definition;

/**
 * Definition used by the AI model to determine when and how to call the tool.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code ToolDefinition}：工具的「对外契约/描述信息」，也就是最终发送给大模型的那份工具说明。
 *
 * <p>
 * <b>定位：</b>它只描述「这个工具叫什么、能干什么、需要哪些参数」，<b>不包含</b>具体执行逻辑。
 * 执行逻辑由 {@code ToolCallback} 负责；二者的关系是「说明书」与「执行器」。
 * </p>
 *
 * <p>
 * <b>三个核心要素：</b>
 * </p>
 * <ul>
 * <li>{@link #name()} —— 工具名，在同一次请求提供给模型的工具集合中必须<b>唯一</b>，
 * 模型正是通过这个名字回传「我要调用哪个工具」。</li>
 * <li>{@link #description()} —— 工具用途描述，模型据此判断何时调用。</li>
 * <li>{@link #inputSchema()} —— 入参的 JSON Schema 字符串，模型据此生成结构化参数。</li>
 * </ul>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * ToolDefinition definition = ToolDefinition.builder()
 *         .name("getWeather")
 *         .description("查询指定城市的天气")
 *         .inputSchema("{\"type\":\"object\",\"properties\":{...}}")
 *         .build();
 * }</pre>
 *
 * <p>
 * 实际开发中更常见的是由 {@code ToolDefinitions#from(Method)} 从带 {@code @Tool} 注解的方法自动生成。
 * </p>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see DefaultToolDefinition
 * @see org.springframework.ai.tool.support.ToolDefinitions
 */
public interface ToolDefinition {

	/**
	 * The tool name. Unique within the tool set provided to a model.
	 */
	// 中文：工具名称。必须在提供给模型的工具集合内唯一，模型回传的 function name 需能匹配到它。
	String name();

	/**
	 * The tool description, used by the AI model to determine what the tool does.
	 */
	// 中文：工具描述。模型判断"该不该调用这个工具"的主要依据。
	String description();

	/**
	 * The schema of the parameters used to call the tool.
	 */
	// 中文：入参的 JSON Schema（字符串形式）。约束模型必须按此结构生成参数，
	// 从而保证框架能把模型输出反序列化成 Java 对象。
	String inputSchema();

	/**
	 * Create a default {@link ToolDefinition} builder.
	 */
	// 中文：静态工厂方法，返回默认实现的 Builder。
	// 这是接口"自带构造入口"的常见写法：调用方只依赖接口 ToolDefinition，无需感知 DefaultToolDefinition。
	static DefaultToolDefinition.Builder builder() {
		return DefaultToolDefinition.builder();
	}

}
