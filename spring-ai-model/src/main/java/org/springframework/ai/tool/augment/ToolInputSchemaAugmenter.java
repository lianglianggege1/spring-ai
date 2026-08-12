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

package org.springframework.ai.tool.augment;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.util.json.schema.JsonSchemaUtils;

/**
 * This utility provides functionality to augment a JSON Schema with additional fields
 * based on a provided Record type. It uses a JSON Schema Generator to generate the schema
 * for the Record's fields and integrates them into an existing JSON Schema. The augmented
 * schema can then be used to re-define the tool inputs for tool calling.
 *
 * @author Christian Tzolov
 */
/**
 * 【中文说明】{@code ToolInputSchemaAugmenter}：JSON Schema「增强器」静态工具类。
 *
 * <p>
 * <b>核心能力：</b>把一个 record 类型的字段，动态<b>合并</b>进已有的工具入参 JSON Schema，
 * 从而在不修改原工具代码的前提下，让模型多填几个额外参数。
 * </p>
 *
 * <p>
 * <b>两个主要方法：</b>
 * </p>
 * <ul>
 * <li>{@link #toAugmentedArgumentTypes(Class)} —— 反射 record 组件，
 * 结合 {@link ToolParam} 注解提取出「字段名 / 类型 / 描述 / 是否必填」元信息列表。</li>
 * <li>{@link #augmentToolInputSchema(String, List)} —— 用 Jackson 解析原 schema 的 JSON 树，
 * 往 {@code properties} 节点里插入新字段，必填字段再追加到 {@code required} 数组。</li>
 * </ul>
 *
 * <p>
 * <b>实现要点：</b>基于 Jackson 的 {@code ObjectNode} 做树形操作，
 * 对 {@code properties} 与 {@code required} 节点均采用「存在则复用、不存在则新建」的策略，
 * 因此能兼容各种形态的原始 schema（包括没有任何参数的空 schema）。
 * </p>
 *
 * @author Christian Tzolov
 * @see AugmentedToolCallback
 */
public final class ToolInputSchemaAugmenter {

	// 中文：私有构造器，防止实例化（纯静态工具类）。
	private ToolInputSchemaAugmenter() {
	}

	/**
	 * Extracts the tool argument types from a record class annotated with
	 * {@link ToolParam}. It retrieves the field names, types, descriptions, and required
	 * status from the record components.
	 * @param recordClass The record class to extract argument types from.
	 * @return A list of {@link AugmentedArgumentType} representing the tool input
	 * argument types.
	 */
	// 中文：反射 record 类，把它的每个组件转换成一条"增强参数元信息"。
	public static <T extends Record> List<AugmentedArgumentType> toAugmentedArgumentTypes(Class<T> recordClass) {
		try {

			return Arrays.stream(recordClass.getRecordComponents()).map(c -> {
				// Get the annotation from the corresponding field, not the record
				// component
				// 中文：重要细节 —— 注解要从"对应的私有字段"上读，而不是从 record 组件上读。
				// 因为 @ToolParam 的 @Target 包含 FIELD，编译器会把它放到 record 生成的私有字段上，
				// 直接调用 c.getAnnotation(...) 往往取不到。
				ToolParam toolParam = null;
				try {
					var field = recordClass.getDeclaredField(c.getName());
					toolParam = field.getAnnotation(ToolParam.class);
				}
				catch (NoSuchFieldException e) {
					// Field not found, toolParam remains null
					// 中文：空值处理 —— 找不到字段时静默忽略，toolParam 保持 null，
					// 下面会走默认值分支，保证流程不中断。
				}

				// 中文：组装元信息。注意两个兜底默认值：
				// 描述缺省为 "no description"；required 缺省为 false
				// （与 @ToolParam 注解本身默认 true 相反 —— 增强字段默认是可选的，更保守）。
				return new AugmentedArgumentType(c.getName(), c.getGenericType(),
						toolParam != null ? toolParam.description() : "no description",
						toolParam != null ? toolParam.required() : false);
			}).toList();

		}
		catch (Exception e) {
			// 中文：反射相关异常统一包装成运行时异常向上抛出。
			throw new RuntimeException("Failed to extract record field types", e);
		}
	}

	// 中文：便捷重载 —— 只追加"单个"字段。内部包装成单元素 List 后复用主实现。
	public static String augmentToolInputSchema(String jsonSchemaString, String propertyName, Type propertyType,
			String description, boolean required) {

		return augmentToolInputSchema(jsonSchemaString,
				List.of(new AugmentedArgumentType(propertyName, propertyType, description, required)));
	}

	// 中文：主实现 —— 把一批增强字段合并进原始 JSON Schema，返回新的 schema 字符串。
	public static String augmentToolInputSchema(String jsonSchemaString, List<AugmentedArgumentType> argumentType) {

		try {

			// 中文：把原始 schema 解析成 Jackson 的可变 JSON 树，便于原地增删节点。
			ObjectNode schemaObjectNode = (ObjectNode) JacksonUtils.getDefaultJsonMapper().readTree(jsonSchemaString);

			// Handle properties
			// 中文：获取或创建 properties 节点。
			// "存在则复用、不存在则新建并挂载"的写法，可兼容"原本没有任何参数"的空 schema。
			ObjectNode propertiesNode;
			if (schemaObjectNode.has("properties")) {
				propertiesNode = (ObjectNode) schemaObjectNode.get("properties");
			}
			else {
				propertiesNode = JacksonUtils.getDefaultJsonMapper().createObjectNode();
				schemaObjectNode.set("properties", propertiesNode);
			}

			// 中文：逐个字段插入 schema。
			for (AugmentedArgumentType argument : argumentType) {

				// 中文：根据 Java 类型生成对应的 JSON Schema 片段（如 String -> {"type":"string"}）。
				ObjectNode parameterNode = JsonSchemaUtils.getJsonSchema(argument.type());

				// 中文：描述非空时才写入，避免产生 "description": "" 这样的无效噪声。
				if (argument.description() != null && !argument.description().isEmpty()) {
					parameterNode.put("description", argument.description());
				}
				// 中文：注意用的是 set 而非"若不存在才放"，因此同名字段会被覆盖 ——
				// 即增强字段优先级高于原工具的同名参数。
				propertiesNode.set(argument.name(), parameterNode);

				// 中文：必填字段还需追加到顶层 required 数组。
				if (argument.required()) {

					// 中文：同样是"存在则复用、不存在则新建"的模式。
					ArrayNode requiredArray;
					if (schemaObjectNode.has("required")) {
						requiredArray = (ArrayNode) schemaObjectNode.get("required");
					}
					else {
						requiredArray = JacksonUtils.getDefaultJsonMapper().createArrayNode();
						schemaObjectNode.set("required", requiredArray);
					}
					requiredArray.add(argument.name());

				}
			}

			// 中文：序列化回字符串。使用美化打印器，生成的 schema 可读性更好，便于调试查看。
			return JacksonUtils.getDefaultJsonMapper()
				.writerWithDefaultPrettyPrinter()
				.writeValueAsString(schemaObjectNode);

		}
		catch (Exception e) {
			// 中文：JSON 解析/序列化异常统一包装（常见原因：传入的 schema 字符串本身不是合法 JSON）。
			throw new RuntimeException("Failed to parse JSON Schema", e);
		}
	}

	/**
	 * Represents an extended argument type with additional metadata such as description
	 * and required status.
	 */
	/**
	 * 【中文说明】{@code AugmentedArgumentType}：描述一个「增强字段」的元信息，不可变 record。
	 *
	 * <ul>
	 * <li>{@code name} —— 字段名，同时也是 JSON Schema 中 properties 的 key</li>
	 * <li>{@code type} —— 字段的 Java 类型（用 {@link Type} 以保留泛型信息）</li>
	 * <li>{@code description} —— 字段描述，写入 schema 供模型理解</li>
	 * <li>{@code required} —— 是否必填，决定是否加入 schema 的 required 数组</li>
	 * </ul>
	 */
	public record AugmentedArgumentType(String name, Type type, String description, boolean required) {
	}

}
