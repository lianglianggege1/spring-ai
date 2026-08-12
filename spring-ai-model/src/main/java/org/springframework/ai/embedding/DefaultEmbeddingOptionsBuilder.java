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

/**
 * Default implementation of {@link EmbeddingOptions.Builder}.
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 */
/**
 * {@link EmbeddingOptions.Builder} 的默认实现。
 *
 * <p>
 * 设计说明：本类本身不持有任何参数字段，而是内部组合了一个
 * {@link DefaultEmbeddingOptions.Builder}，所有设置方法都<b>委托</b>给它——这是典型的
 * 委托（Delegation）模式。之所以再包一层，是为了对外暴露接口类型
 * {@code EmbeddingOptions.Builder}，让调用方编程到接口而非具体实现，
 * 便于后续替换底层构建逻辑而不影响使用方。
 *
 * <p>
 * 注意各 setter 返回的是 {@code this}（本包装器），而不是内部 builder，
 * 从而保证链式调用过程中始终暴露接口类型。
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * EmbeddingOptions options = new DefaultEmbeddingOptionsBuilder()
 *         .model("text-embedding-3-small")
 *         .dimensions(1536)
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 */
public class DefaultEmbeddingOptionsBuilder implements EmbeddingOptions.Builder {

	// 被委托的真实构建器，实际的参数都存在它里面
	private final DefaultEmbeddingOptions.Builder builder = DefaultEmbeddingOptions.builder();

	// 设置模型名：委托给内部 builder，但返回 this 以保持返回类型为接口
	@Override
	public EmbeddingOptions.Builder model(String model) {
		this.builder.model(model);
		return this;
	}

	// 设置期望向量维度：同样委托给内部 builder
	@Override
	public EmbeddingOptions.Builder dimensions(Integer dimensions) {
		this.builder.dimensions(dimensions);
		return this;
	}

	// 触发内部 builder 完成构建，返回不可变的 EmbeddingOptions
	@Override
	public EmbeddingOptions build() {
		return this.builder.build();
	}

}
