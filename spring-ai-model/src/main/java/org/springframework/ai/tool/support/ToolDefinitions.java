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

package org.springframework.ai.tool.support;

import java.lang.reflect.Method;

import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.Assert;

/**
 * Utility class for creating {@link ToolDefinition} builders and instances from Java
 * {@link Method} objects.
 * <p>
 * This class provides static methods to facilitate the construction of
 * {@link ToolDefinition} objects by extracting relevant metadata from Java reflection
 * {@link Method} instances.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code ToolDefinitions}：从 Java {@link Method} 反射对象「一键生成」
 * {@link ToolDefinition} 的静态工厂工具类。
 *
 * <p>
 * <b>它是把「Java 方法」翻译成「LLM 工具说明书」的入口。</b>一次调用即可自动完成三件事：
 * </p>
 * <ol>
 * <li>解析工具名（读 {@code @Tool(name)}，缺省用方法名）</li>
 * <li>解析工具描述（读 {@code @Tool(description)}，缺省用方法名）</li>
 * <li><b>反射方法签名自动生成入参 JSON Schema</b>（这是最有价值的一步，
 * 会读取参数类型、{@code @ToolParam} 的 required/description 等信息）</li>
 * </ol>
 *
 * <p>
 * <b>两个方法的关系：</b>{@link #builder(Method)} 返回已预填好的 Builder，
 * 允许调用方再覆盖某些字段；{@link #from(Method)} 是它的"一步到位"简化版。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * // 直接生成
 * ToolDefinition def = ToolDefinitions.from(method);
 *
 * // 生成后再定制描述
 * ToolDefinition def = ToolDefinitions.builder(method).description("自定义描述").build();
 * }</pre>
 *
 * @author Mark Pollack
 * @since 1.0.0
 * @see ToolUtils
 * @see org.springframework.ai.util.json.schema.JsonSchemaGenerator
 */
public final class ToolDefinitions {

	// 中文：私有构造器，防止实例化（纯静态工具类惯用写法）。
	private ToolDefinitions() {
		// prevents instantiation.
	}

	/**
	 * Create a default {@link ToolDefinition} builder from a {@link Method}.
	 */
	// 中文：由方法生成一个"已预填三要素"的 Builder。
	// 返回 Builder 而非成品的好处：调用方可在自动解析的基础上覆盖任意字段后再 build()。
	public static DefaultToolDefinition.Builder builder(Method method) {
		Assert.notNull(method, "method cannot be null");
		return DefaultToolDefinition.builder()
			// 中文：工具名 —— 来自 @Tool(name) 或方法名。
			.name(ToolUtils.getToolName(method))
			// 中文：工具描述 —— 来自 @Tool(description) 或方法名。
			.description(ToolUtils.getToolDescription(method))
			// 中文：入参 schema —— 反射方法参数列表自动生成 JSON Schema，
			// 会一并读取 @ToolParam 上的 required 与 description 信息。
			.inputSchema(JsonSchemaGenerator.generateForMethodInput(method));
	}

	/**
	 * Create a default {@link ToolDefinition} instance from a {@link Method}.
	 */
	// 中文：便捷方法 —— 直接构建出成品 ToolDefinition，等价于 builder(method).build()。
	// 无需重复做 null 校验，因为已委托给上面的 builder(method)。
	public static ToolDefinition from(Method method) {
		return builder(method).build();
	}

}
