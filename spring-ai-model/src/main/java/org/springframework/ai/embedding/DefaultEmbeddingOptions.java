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

package org.springframework.ai.embedding;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link EmbeddingOptions}.
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 */
/**
 * {@link EmbeddingOptions} 的默认实现：一个只含"模型名 + 维度"两个通用参数的不可变值对象。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code model}：要调用的嵌入模型名，为 null 时表示沿用 EmbeddingModel 自身的默认模型；</li>
 * <li>{@code dimensions}：期望的输出向量维度（部分模型如 OpenAI text-embedding-3 支持降维），
 * 为 null 时使用模型默认维度。</li>
 * </ul>
 *
 * <p>
 * 设计说明：两个字段都是 {@code final} 且允许为 {@code null}（用 {@code @Nullable} 显式标注），
 * "null 即不覆盖默认值"是 Spring AI options 体系的统一约定；对象一经构建不可修改，可安全共享。
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * EmbeddingOptions options = DefaultEmbeddingOptions.builder()
 *         .model("text-embedding-3-small")
 *         .dimensions(512)
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 */
public class DefaultEmbeddingOptions implements EmbeddingOptions {

	// 嵌入模型名称；null 表示不覆盖，使用模型客户端的默认配置
	private final @Nullable String model;

	// 期望的输出向量维度；null 表示使用模型默认维度
	private final @Nullable Integer dimensions;

	// 构造器声明为 protected：强制外部通过 builder() 创建，同时仍允许子类扩展
	protected DefaultEmbeddingOptions(@Nullable String model, @Nullable Integer dimensions) {
		this.model = model;
		this.dimensions = dimensions;
	}

	// 获取构建器入口（唯一的公开创建方式）
	public static Builder builder() {
		return new Builder();
	}

	// 返回模型名，可能为 null
	@Override
	public @Nullable String getModel() {
		return this.model;
	}

	// 返回期望维度，可能为 null
	@Override
	public @Nullable Integer getDimensions() {
		return this.dimensions;
	}

	/**
	 * 内部构建器：采用流式（fluent）写法逐项设置参数，最后 {@code build()} 生成不可变实例。
	 *
	 * <p>
	 * 每个 setter 都返回 {@code this} 以支持链式调用；构造器私有，只能由外层的
	 * {@link DefaultEmbeddingOptions#builder()} 创建。
	 */
	public static final class Builder implements EmbeddingOptions.Builder {

		private @Nullable String model;

		private @Nullable Integer dimensions;

		// 私有构造，防止绕过 DefaultEmbeddingOptions.builder() 直接 new
		private Builder() {
		}

		// 设置模型名，返回 this 以支持链式调用
		public Builder model(@Nullable String model) {
			this.model = model;
			return this;
		}

		// 设置期望维度，返回 this 以支持链式调用
		public Builder dimensions(@Nullable Integer dimensions) {
			this.dimensions = dimensions;
			return this;
		}

		// 生成不可变的 options 实例；此处不做非空校验，因为两个参数本就允许为 null
		public DefaultEmbeddingOptions build() {
			return new DefaultEmbeddingOptions(this.model, this.dimensions);
		}

	}

}
