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

import com.github.victools.jsonschema.generator.MemberScope;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

/**
 * JSON Schema Generator Module for Spring AI.
 * <p>
 * This module provides a set of customizations to the JSON Schema generator to support
 * the Spring AI framework. It allows extracting descriptions from
 * {@code @ToolParam(description = ...)} annotations and to determine whether a property
 * is required based on the presence of a series of annotations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】Spring AI 默认的 JSON Schema 生成模块，基于 {@code @ToolParam} 注解。
 *
 * <p>
 * 用途：继承 {@link AbstractSpringAiSchemaModule}，把父类留下的两个抽象钩子落地为
 * 「读取 {@link ToolParam} 注解」。这是工具调用场景下最常用的实现——开发者在方法参数或 DTO 字段上写
 * {@code @ToolParam(description = "城市名", required = true)}，本模块负责把这些信息翻译进 JSON Schema。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #resolveToolParamDescription}：取注解的 description，空白字符串视同未填。</li>
 * <li>{@link #resolveToolParamRequired}：取注解的 required；注解不存在时返回 null，
 * 交由父类继续走后续判定链。</li>
 * </ul>
 *
 * <p>
 * 设计要点：类声明为 {@code final}，不再允许继续继承；构造器仅透传 options 给父类。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class SpringAiSchemaModule extends AbstractSpringAiSchemaModule {

	// 构造器：把可变参数选项原样交给父类，由父类决定 requiredByDefault
	public SpringAiSchemaModule(Option... options) {
		super(options);
	}

	@Override
	// 实现钩子一：从 @ToolParam 中提取参数描述
	protected @Nullable String resolveToolParamDescription(MemberScope<?, ?> member) {
		// 同时考察字段与其 getter，注解写在任一处都能被找到
		var annotation = member.getAnnotationConsideringFieldAndGetter(ToolParam.class);
		// 双重判断：注解存在且 description 非空白（hasText 会过滤 null、""、纯空格）
		if (annotation != null && StringUtils.hasText(annotation.description())) {
			return annotation.description();
		}
		// 返回 null 表示“无描述”，生成的 Schema 中不会出现 description 字段
		return null;
	}

	@Override
	// 实现钩子二：从 @ToolParam 中提取 required 标志
	protected @Nullable Boolean resolveToolParamRequired(MemberScope<?, ?> member) {
		var annotation = member.getAnnotationConsideringFieldAndGetter(ToolParam.class);
		// 注解不存在时必须返回 null（而非 false），否则会截断父类后续的 @JsonProperty/@Schema/空安全判定链
		return annotation != null ? annotation.required() : null;
	}

}
