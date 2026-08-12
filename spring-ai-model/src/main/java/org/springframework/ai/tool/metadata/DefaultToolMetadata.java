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

package org.springframework.ai.tool.metadata;

/**
 * Default implementation of {@link ToolMetadata}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code DefaultToolMetadata}：{@link ToolMetadata} 的默认实现，同样使用 {@code record}，
 * 是不可变的值对象。
 *
 * <p>
 * <b>唯一字段：</b>{@code returnDirect} —— 工具执行结果是否直接返回给调用方（不回灌给模型）。
 * record 会自动生成同名访问器 {@code returnDirect()}，恰好实现了 {@link ToolMetadata#returnDirect()}
 * 接口方法，因此无需手写任何方法体。
 * </p>
 *
 * <p>
 * <b>为什么这里没有校验逻辑：</b>字段是基本类型 {@code boolean}，不存在 null 或空值问题，
 * 所以不需要像 {@code DefaultToolDefinition} 那样写紧凑构造器做断言。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * ToolMetadata metadata = DefaultToolMetadata.builder().returnDirect(true).build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public record DefaultToolMetadata(boolean returnDirect) implements ToolMetadata {

	// 中文：获取 Builder 实例的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@code DefaultToolMetadata} 的构建器。
	 *
	 * <p>
	 * 虽然目前只有一个字段、直接 {@code new} 也很方便，但仍保留 Builder，
	 * 目的是<b>为将来新增元数据字段预留扩展空间</b>——届时新增字段不会破坏已有调用方代码。
	 * </p>
	 */
	public static final class Builder {

		// 中文：默认 false，即结果回灌给模型，与 ToolMetadata 接口的默认行为保持一致。
		private boolean returnDirect = false;

		// 中文：私有构造器，强制通过 DefaultToolMetadata.builder() 创建。
		private Builder() {
		}

		// 中文：设置是否直接返回结果，返回 this 支持链式调用。
		public Builder returnDirect(boolean returnDirect) {
			this.returnDirect = returnDirect;
			return this;
		}

		// 中文：完成构建，返回不可变的 record 实例。
		// 返回类型声明为接口 ToolMetadata 而非具体实现，便于调用方面向接口编程。
		public ToolMetadata build() {
			return new DefaultToolMetadata(this.returnDirect);
		}

	}

}
