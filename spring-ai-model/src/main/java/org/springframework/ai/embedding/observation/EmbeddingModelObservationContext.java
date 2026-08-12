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

package org.springframework.ai.embedding.observation;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.util.Assert;

/**
 * Context used to store metadata for embedding model exchanges.
 *
 * @author Thomas Vitale
 * @author Soby Chacko
 * @since 1.0.0
 */
/**
 * 嵌入模型调用的<b>观测上下文</b>：在一次调用的生命周期内，承载请求、响应及操作元数据，
 * 供 {@link EmbeddingModelObservationConvention} 从中提取标签。
 *
 * <p>
 * 生命周期：调用开始前创建（此时只有 request），调用结束后由框架回填 response，
 * 随后 Convention 读取它生成指标与链路数据。
 *
 * <p>
 * 关键设计：
 * <ul>
 * <li>构造器是<b>包级私有</b>的，外部只能经 {@code builder()} 创建，
 * 从而强制走 Builder 中的校验；</li>
 * <li>构造时自动把操作类型固定为 {@link AiOperationType#EMBEDDING}，
 * 调用方只需提供 provider（厂商名）。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * var context = EmbeddingModelObservationContext.builder()
 *         .embeddingRequest(request)
 *         .provider("openai")
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author Soby Chacko
 * @since 1.0.0
 */
public class EmbeddingModelObservationContext extends ModelObservationContext<EmbeddingRequest, EmbeddingResponse> {

	// 包级私有构造器：限制外部直接 new，只能通过 builder() 创建以保证校验被执行
	EmbeddingModelObservationContext(EmbeddingRequest embeddingRequest, String provider) {
		// 操作类型在此硬编码为 EMBEDDING，无需调用方传入
		super(embeddingRequest,
				AiOperationMetadata.builder()
					.operationType(AiOperationType.EMBEDDING.value())
					.provider(provider)
					.build());
	}

	// 获取构建器
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 上下文构建器：两个字段都是<b>必填项</b>，在 {@code build()} 时统一校验。
	 */
	public static final class Builder {

		// 中间状态允许为 null，故标注 @Nullable；但 build() 时必须已被赋值
		private @Nullable EmbeddingRequest embeddingRequest;

		private @Nullable String provider;

		// 私有构造，只能由外层 builder() 创建
		private Builder() {
		}

		// 设置本次观测对应的嵌入请求（必填）
		public Builder embeddingRequest(EmbeddingRequest embeddingRequest) {
			this.embeddingRequest = embeddingRequest;
			return this;
		}

		// 设置模型厂商名（必填），如 openai、ollama
		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		// 构建前统一做必填校验；用 Assert.state 而非 notNull，语义上强调"构建器状态不完整"
		public EmbeddingModelObservationContext build() {
			Assert.state(this.embeddingRequest != null, "request cannot be null");
			Assert.state(this.provider != null, "provider cannot be null or empty");
			return new EmbeddingModelObservationContext(this.embeddingRequest, this.provider);
		}

	}

}
