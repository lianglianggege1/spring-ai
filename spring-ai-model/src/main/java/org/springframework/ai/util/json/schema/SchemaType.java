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

package org.springframework.ai.util.json.schema;

/**
 * The type of schema to generate for a given Java type.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】描述“要为某个 Java 类型生成哪种规格的 Schema”的枚举。
 *
 * <p>
 * 用途：不同厂商对工具参数的结构描述要求不同——多数遵循标准 JSON Schema，而部分（如 Vertex AI/Gemini）
 * 要求 OpenAPI 规范的 Schema 子集。本枚举作为开关传给 {@code JsonSchemaGenerator}，决定输出方言。
 *
 * <p>
 * 枚举常量：
 * <ul>
 * <li>{@link #JSON_SCHEMA}：标准 JSON Schema（Draft 2020-12），默认选项。</li>
 * <li>{@link #OPEN_API_SCHEMA}：OpenAPI 3.0 风格的 Schema，字段集更受限。</li>
 * </ul>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public enum SchemaType {

	/**
	 * JSON schema.
	 */
	// 标准 JSON Schema，绝大多数模型厂商采用
	JSON_SCHEMA,

	/**
	 * Open API schema.
	 */
	// OpenAPI 方言，用于要求该格式的厂商（生成时会做相应裁剪与适配）
	OPEN_API_SCHEMA

}
