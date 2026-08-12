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

/**
 * Marks a tool argument.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code @ToolParam} 注解：标记工具方法的<b>入参</b>（或入参对象内部的字段），
 * 为其补充「是否必填」和「参数含义描述」这两类信息。
 *
 * <p>
 * <b>用途：</b>Spring AI 在为 {@link Tool} 方法生成 JSON Schema 时会读取该注解，
 * 把 {@link #description()} 写入 schema 的 {@code description} 字段，
 * 把 {@link #required()} 反映到 schema 的 {@code required} 数组中。
 * 描述写得越清楚，模型填参的准确率越高。
 * </p>
 *
 * <p>
 * <b>关键属性：</b>
 * </p>
 * <ul>
 * <li>{@link #required()} —— 默认 {@code true}，即参数默认是必填的；设为 false 表示模型可以不传该参数。</li>
 * <li>{@link #description()} —— 参数语义说明，例如「城市名称，如：北京」。</li>
 * </ul>
 *
 * <p>
 * <b>作用目标：</b>{@code PARAMETER}（方法参数）、{@code FIELD}（入参 POJO 的字段，
 * 用于嵌套复杂对象的场景）、{@code ANNOTATION_TYPE}（组合注解）。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * @Tool(description = "查询指定城市天气")
 * String getWeather(@ToolParam(description = "城市名称，如：北京") String city,
 *         @ToolParam(required = false, description = "温度单位，C 或 F") String unit) { ... }
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see Tool
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

	/**
	 * Whether the tool argument is required.
	 */
	// 中文：该参数是否必填，默认 true（注意与 JSON Schema 惯例相反，Spring AI 这里默认必填）。
	// 设为 false 时，生成的 schema 不会把此参数列入 required 列表，模型可以省略不传。
	boolean required() default true;

	/**
	 * The description of the tool argument.
	 */
	// 中文：参数描述，会写入 JSON Schema 的 description 字段，是模型正确填参的主要依据。
	String description() default "";

}
