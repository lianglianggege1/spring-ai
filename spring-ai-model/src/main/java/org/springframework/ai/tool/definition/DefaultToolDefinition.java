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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.util.ParsingUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ToolDefinition}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code DefaultToolDefinition}：{@link ToolDefinition} 的默认实现，用 Java {@code record} 定义，
 * 因此是<b>不可变（immutable）</b>的值对象，自动拥有 equals/hashCode/toString 和三个同名访问器方法。
 *
 * <p>
 * <b>三个组件（record components）：</b>
 * </p>
 * <ul>
 * <li>{@code name} —— 工具名（非空）</li>
 * <li>{@code description} —— 工具描述（非空）</li>
 * <li>{@code inputSchema} —— 入参 JSON Schema 字符串（非空）</li>
 * </ul>
 *
 * <p>
 * <b>两条构造路径：</b>
 * </p>
 * <ol>
 * <li><b>紧凑构造器</b>：直接 {@code new} 时三个字段都必须有值，否则抛异常——这是最严格的校验。</li>
 * <li><b>Builder</b>：更宽松，允许不传 description，此时会由工具名<b>自动推导</b>出一个描述
 * （驼峰拆词，如 {@code getWeather} → {@code "get Weather"}）。</li>
 * </ol>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * ToolDefinition def = DefaultToolDefinition.builder()
 *         .name("getWeather")
 *         .inputSchema(schemaJson) // description 省略时自动推导为 "get Weather"
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public record DefaultToolDefinition(String name, String description, String inputSchema) implements ToolDefinition {

	// 中文：record 的"紧凑构造器"（compact constructor），只写校验逻辑，
	// 字段赋值由编译器自动补全。这里保证三个字段都是"非 null 且非空白字符串"。
	public DefaultToolDefinition {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
	}

	// 中文：获取 Builder 实例的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@code DefaultToolDefinition} 的构建器。
	 *
	 * <p>
	 * 采用「链式调用 + build() 收尾」的经典 Builder 模式。相比直接 {@code new record}，
	 * 它的额外价值在于：<b>description 可缺省并自动推导</b>。
	 * </p>
	 *
	 * <p>
	 * 三个字段声明为 {@code @Nullable}，是因为在 build() 之前它们确实可能还没被赋值；
	 * 本包被 {@code @NullMarked} 标记（默认非空），所以此处需显式标注 {@code @Nullable}。
	 * </p>
	 */
	public static final class Builder {

		// 中文：工具名，build() 时必填。
		private @Nullable String name;

		// 中文：工具描述，可缺省 —— build() 时会由 name 驼峰拆词自动生成。
		private @Nullable String description;

		// 中文：入参 JSON Schema，build() 时必填。
		private @Nullable String inputSchema;

		// 中文：私有构造器，强制外部只能通过 DefaultToolDefinition.builder() 获取实例。
		private Builder() {
		}

		// 中文：设置工具名，返回 this 以支持链式调用。
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		// 中文：设置工具描述（可不调用，交由 build() 自动推导）。
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		// 中文：设置入参 JSON Schema。
		public Builder inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		// 中文：完成构建。注意校验用的是 Assert.state（校验"对象自身状态"是否合法，
		// 不合法抛 IllegalStateException），而非 Assert.notNull（校验入参）。
		public ToolDefinition build() {
			// 中文：name 是唯一无法自动推导的必填项，先校验。
			Assert.state(this.name != null, "toolName cannot be null or empty");
			// 中文：关键分支 —— description 为空时，用工具名驼峰拆词生成一个兜底描述，
			// 例如 "getWeather" -> "get Weather"。这样调用方可以少写一行配置。
			if (!StringUtils.hasText(this.description)) {
				this.description = ParsingUtils.reConcatenateCamelCase(this.name, " ");
			}
			// 中文：兜底后再次断言，同时也让静态空值分析工具确信 description 已非 null。
			Assert.state(this.description != null, "toolDescription cannot be null or empty");
			Assert.state(this.inputSchema != null, "inputSchema cannot be null or empty");
			// 中文：交给 record 的紧凑构造器做最终的 hasText 校验。
			return new DefaultToolDefinition(this.name, this.description, this.inputSchema);
		}

	}

}
