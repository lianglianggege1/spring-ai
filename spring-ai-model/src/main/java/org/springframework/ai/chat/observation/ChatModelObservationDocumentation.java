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

package org.springframework.ai.chat.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

import org.springframework.ai.observation.conventions.AiObservationAttributes;

/**
 * Documented conventions for chat model observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelObservationDocumentation 以枚举形式"声明式地"描述对话模型观测的规范：
 * 包含默认约定类、以及全部低基数/高基数标签名。
 *
 * <p>
 * 用途有三：
 * <ul>
 * <li>作为文档来源：Micrometer 的文档生成器会扫描 {@code ObservationDocumentation}
 * 实现，自动产出观测指标文档。</li>
 * <li>作为测试契约：单元测试可据此断言"实际产生的标签"与"声明的标签"一致。</li>
 * <li>作为常量池：{@code DefaultChatModelObservationConvention} 直接引用这里的枚举项来构造
 * KeyValue，避免字符串硬编码。</li>
 * </ul>
 *
 * <p>
 * 关键概念——低基数 vs 高基数：
 * <ul>
 * <li><b>低基数（LowCardinality）</b>：取值种类有限，如操作类型、厂商名、模型名。会作为
 * 指标（Metrics）的维度标签，因此数量必须可控，否则会造成指标爆炸。</li>
 * <li><b>高基数（HighCardinality）</b>：取值几乎无限，如响应 id、token 数、温度参数。
 * 只会附加到链路追踪（Tracing）的 span 上，不进入指标维度。</li>
 * </ul>
 *
 * <p>
 * 标签名本身并不在这里硬编码，而是统一委托给 {@code AiObservationAttributes}，
 * 以保证与 OpenTelemetry GenAI 语义约定保持一致。
 */
public enum ChatModelObservationDocumentation implements ObservationDocumentation {

	// 中文说明：唯一的枚举常量，代表"一次对话模型操作"这个观测点。
	// 采用"常量特定类主体"写法（枚举常量后跟 {...}），为该常量单独提供方法实现。
	CHAT_MODEL_OPERATION {
		// 中文说明：声明默认使用的约定实现类；用户未自定义 Convention 时由 Micrometer 回退到它
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultChatModelObservationConvention.class;
		}

		// 中文说明：声明全部低基数标签名（会成为指标维度）
		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		// 中文说明：声明全部高基数标签名（仅用于链路追踪）
		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

	};

	/**
	 * Low-cardinality observation key names for chat model operations.
	 */
	/**
	 * 【中文说明】低基数标签名枚举——取值种类有限，可安全用作指标维度。
	 *
	 * <p>
	 * 共 4 项：操作类型、服务商、请求模型名、响应模型名。每个枚举项通过覆写
	 * {@code asString()} 把自己映射到 {@code AiObservationAttributes} 中的标准属性名。
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The name of the operation being performed.
		 */
		// 中文说明：操作类型，对话场景下固定为 "chat"（来自 AiOperationType.CHAT）
		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		/**
		 * The model provider as identified by the client instrumentation.
		 */
		// 中文说明：模型服务提供商，如 openai、ollama、zhipuai 等
		AI_PROVIDER {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_PROVIDER.value();
			}
		},

		/**
		 * The name of the model a request is being made to.
		 */
		// 中文说明：请求中指定的模型名（来自 ChatOptions.getModel()），未指定时打点为 "none"
		REQUEST_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_MODEL.value();
			}
		},

		/**
		 * The name of the model that generated the response.
		 */
		// 中文说明：响应中实际返回的模型名。它可能与请求模型名不同
		// （例如请求写 gpt-4o，服务端实际返回 gpt-4o-2024-08-06 这类带版本号的具体模型）
		RESPONSE_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_MODEL.value();
			}
		}

	}

	/**
	 * High-cardinality observation key names for chat model operations.
	 */
	/**
	 * 【中文说明】高基数标签名枚举——取值范围大或近乎无限，只附加到链路追踪 span，不作为指标维度。
	 *
	 * <p>
	 * 按来源可分为三组：
	 * <ul>
	 * <li>请求参数组（REQUEST_*）：温度、topP、topK、maxTokens、惩罚系数、停止词、工具名列表、
	 * 是否流式等，均取自 {@code ChatOptions}。</li>
	 * <li>响应信息组（RESPONSE_*）：结束原因列表、响应 id。</li>
	 * <li>用量统计组（USAGE_*）：输入/输出/总 token 数，以及缓存读写 token 数。</li>
	 * </ul>
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * The frequency penalty setting for the model request.
		 */
		// 中文说明：频率惩罚系数，数值越高越抑制重复用词
		REQUEST_FREQUENCY_PENALTY {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_FREQUENCY_PENALTY.value();
			}
		},

		/**
		 * The maximum number of tokens the model generates for a request.
		 */
		// 中文说明：本次请求允许模型生成的最大 token 数
		REQUEST_MAX_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_MAX_TOKENS.value();
			}
		},

		/**
		 * The presence penalty setting for the model request.
		 */
		// 中文说明：存在惩罚系数，数值越高越鼓励模型引入新话题
		REQUEST_PRESENCE_PENALTY {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_PRESENCE_PENALTY.value();
			}
		},

		/**
		 * List of sequences that the model will use to stop generating further tokens.
		 */
		// 中文说明：停止词序列列表，模型生成到这些内容时会立即停止
		REQUEST_STOP_SEQUENCES {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_STOP_SEQUENCES.value();
			}
		},

		/**
		 * Indicates whether the GenAI request was made in streaming mode.
		 */
		// 中文说明：是否为流式请求。注意：默认约定中只有 streaming 为 true 时才打此标签
		REQUEST_STREAM {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_STREAM.value();
			}
		},

		/**
		 * The temperature setting for the model request.
		 */
		// 中文说明：采样温度，越高输出越随机，越低越确定
		REQUEST_TEMPERATURE {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_TEMPERATURE.value();
			}
		},

		/**
		 * List of tool definitions provided to the model in the request.
		 */
		// 中文说明：本次请求提供给模型的工具（函数）名称列表
		REQUEST_TOOL_NAMES {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_TOOL_NAMES.value();
			}
		},

		/**
		 * The top_k sampling setting for the model request.
		 */
		// 中文说明：top-k 采样参数，只从概率最高的 k 个候选 token 中采样
		REQUEST_TOP_K {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_TOP_K.value();
			}
		},

		/**
		 * The top_p sampling setting for the model request.
		 */
		// 中文说明：top-p（核采样）参数，从累计概率达到 p 的最小候选集合中采样
		REQUEST_TOP_P {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_TOP_P.value();
			}
		},

		// Response

		/**
		 * Reasons the model stopped generating tokens, corresponding to each generation
		 * received.
		 */
		// 中文说明：结束原因列表（与每条生成结果一一对应），常见值如 STOP、LENGTH、TOOL_CALLS
		RESPONSE_FINISH_REASONS {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_FINISH_REASONS.value();
			}
		},

		/**
		 * The unique identifier for the AI response.
		 */
		// 中文说明：本次响应的唯一标识，排查线上问题时可用它与厂商侧日志对账
		RESPONSE_ID {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_ID.value();
			}
		},

		// Usage

		/**
		 * The number of input tokens written to a provider-managed cache.
		 */
		// 中文说明：写入服务端缓存的输入 token 数（Prompt Caching 特性，如 Anthropic 的缓存写入）
		USAGE_CACHE_WRITE_INPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_CACHE_WRITE_INPUT_TOKENS.value();
			}
		},

		/**
		 * The number of input tokens served from a provider-managed cache.
		 */
		// 中文说明：命中服务端缓存的输入 token 数，这部分通常计费更低，是成本优化的重要观测项
		USAGE_CACHE_READ_INPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_CACHE_READ_INPUT_TOKENS.value();
			}
		},

		/**
		 * The number of tokens used in the model input (prompt).
		 */
		// 中文说明：输入（提示词）消耗的 token 数，对应 Usage.getPromptTokens()
		USAGE_INPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_INPUT_TOKENS.value();
			}
		},

		/**
		 * The number of tokens used in the model output (completion).
		 */
		// 中文说明：输出（补全）消耗的 token 数，对应 Usage.getCompletionTokens()
		USAGE_OUTPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_OUTPUT_TOKENS.value();
			}
		},

		/**
		 * The total number of tokens used in the model exchange.
		 */
		// 中文说明：本次交互消耗的总 token 数，是成本核算最直接的指标
		USAGE_TOTAL_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_TOTAL_TOKENS.value();
			}
		}

	}

}
