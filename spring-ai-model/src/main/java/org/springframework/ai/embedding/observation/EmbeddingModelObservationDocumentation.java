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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

import org.springframework.ai.observation.conventions.AiObservationAttributes;
import org.springframework.ai.observation.conventions.AiOperationType;

/**
 * Documented conventions for embedding model observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 嵌入模型观测的<b>文档化定义</b>：以枚举形式集中声明本模块会产生哪些观测、
 * 每个观测有哪些标签键，以及默认使用哪个约定实现。
 *
 * <p>
 * 用途有二：
 * <ol>
 * <li>作为标签键名的<b>唯一事实来源</b>，被 Convention 引用，避免各处硬编码字符串；</li>
 * <li>Micrometer 的文档生成工具可扫描 {@link ObservationDocumentation} 实现，
 * 自动产出可观测性文档。</li>
 * </ol>
 *
 * <p>
 * 结构说明：外层枚举只有一个常量 {@code EMBEDDING_MODEL_OPERATION}，代表"一次嵌入调用"这个观测；
 * 内部两个嵌套枚举分别列出低基数与高基数的标签键。所有键名最终都委托给
 * {@code AiObservationAttributes} 取值，从而与 OpenTelemetry GenAI 语义约定对齐。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public enum EmbeddingModelObservationDocumentation implements ObservationDocumentation {

	// 唯一的观测定义：代表一次嵌入模型调用
	EMBEDDING_MODEL_OPERATION {
		// 指定默认约定实现，未显式配置 Convention 时框架会使用它
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultEmbeddingModelObservationConvention.class;
		}

		// 声明全部低基数标签键（可安全用作指标维度）
		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		// 声明全部高基数标签键（仅用于链路追踪）
		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}
	};

	/**
	 * Low-cardinality observation key names for embedding model operations.
	 */
	/**
	 * 低基数标签键：取值种类有限（操作类型、厂商、模型名），可作为指标维度参与聚合。
	 *
	 * <p>
	 * 每个枚举常量都重写 {@code asString()}，把键名委托给 {@code AiObservationAttributes}，
	 * 保证与 OpenTelemetry 语义约定一致。
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The name of the operation being performed. Possibly, one of
		 * {@link AiOperationType}.
		 */
		// 标签：操作类型，嵌入场景下取值为 embedding
		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		/**
		 * The model provider as identified by the client instrumentation.
		 */
		// 标签：模型厂商标识，如 openai、ollama、azure
		AI_PROVIDER {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_PROVIDER.value();
			}
		},

		/**
		 * The name of the model a request is being made to.
		 */
		// 标签：请求中指定的模型名
		REQUEST_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_MODEL.value();
			}
		},

		/**
		 * The name of the model that generated the response.
		 */
		// 标签：响应中实际返回的模型名（可能与请求指定的不同）
		RESPONSE_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_MODEL.value();
			}
		}

	}

	/**
	 * High-cardinality observation key names for embedding model operations.
	 */
	/**
	 * 高基数标签键：取值范围大（维度、token 数），仅用于链路追踪。
	 *
	 * <p>
	 * 切勿把它们做成指标标签，否则每个不同的 token 数都会派生一条新的时间序列，
	 * 造成监控系统的"基数爆炸"。
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		// Request

		/**
		 * The number of dimensions the resulting output embeddings have.
		 */
		// 标签：请求中指定的输出向量维度
		REQUEST_EMBEDDING_DIMENSIONS {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_EMBEDDING_DIMENSIONS.value();
			}
		},

		// Usage

		/**
		 * The number of tokens used in the model input.
		 */
		// 标签：输入侧消耗的 token 数
		USAGE_INPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_INPUT_TOKENS.value();
			}
		},

		/**
		 * The total number of tokens used in the model exchange.
		 */
		// 标签：本次调用消耗的总 token 数，是成本核算的主要依据
		USAGE_TOTAL_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_TOTAL_TOKENS.value();
			}
		}

	}

}
