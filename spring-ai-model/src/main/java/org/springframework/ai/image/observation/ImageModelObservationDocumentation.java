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

package org.springframework.ai.image.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

import org.springframework.ai.observation.conventions.AiObservationAttributes;

/**
 * Documented conventions for image model observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 图像模型观测的「文档化定义」。
 * <p>
 * 这是 Micrometer 推荐的写法：用枚举实现 {@link ObservationDocumentation}，
 * 集中声明某类观测的默认约定与全部可用标签名。它有两个作用：
 * <ul>
 * <li>运行时：为 {@link DefaultImageModelObservationConvention} 提供统一的标签名常量。</li>
 * <li>构建期：Micrometer 的文档生成插件可据此自动产出「该应用暴露了哪些指标、含哪些标签」的文档，
 * 并在测试中校验实际产生的标签是否与声明一致。</li>
 * </ul>
 * 内部包含两个嵌套枚举：{@code LowCardinalityKeyNames}（低基数，用作指标维度）与
 * {@code HighCardinalityKeyNames}（高基数，仅用于 tracing）。
 * 所有标签名最终都来源于 {@code AiObservationAttributes}，以保证跨模态命名统一。
 *
 * @since 1.0.0
 */
public enum ImageModelObservationDocumentation implements ObservationDocumentation {

	// 唯一枚举常量：代表「一次图像模型调用」这一类观测。
	// 采用「常量特定类主体」写法，每个常量各自覆写方法，等价于一个匿名子类
	IMAGE_MODEL_OPERATION {
		// 声明该观测默认使用的约定实现，未显式配置时由框架自动采用
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultImageModelObservationConvention.class;
		}

		// 声明全部低基数标签名（会成为指标维度）
		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		// 声明全部高基数标签名（仅进入 tracing span）
		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

	};

	/**
	 * Low-cardinality observation key names for image model operations.
	 */
	/**
	 * 低基数标签名枚举：取值集合有限（操作类型、服务商、模型名），
	 * 可安全用作 metrics 的维度而不会导致指标基数爆炸。
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The name of the operation being performed.
		 */
		// 操作类型，图像场景下恒为 image
		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		/**
		 * The model provider as identified by the client instrumentation.
		 */
		// AI 服务商标识，如 openai、stabilityai
		AI_PROVIDER {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_PROVIDER.value();
			}
		},

		/**
		 * The name of the model a request is being made to.
		 */
		// 请求指定的模型名；未指定时约定实现会填充 "none" 占位
		REQUEST_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_MODEL.value();
			}
		}

	}

	/**
	 * High-cardinality observation key names for image model operations.
	 */
	/**
	 * 高基数标签名枚举：取值组合多（尺寸、风格、响应 ID、token 用量等），
	 * 仅写入 tracing span，不作为 metrics 维度，以免指标基数失控。
	 * <p>
	 * 按用途分为三组：Request（请求参数）、Response（响应信息）、Usage（token 消耗）。
	 * 注意：Response 与 Usage 两组标签由各厂商实现按需填充，默认约定实现只填充 Request 组。
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		// Request

		/**
		 * The format in which the generated image is returned.
		 */
		// 返回格式：url 或 b64_json
		REQUEST_IMAGE_RESPONSE_FORMAT {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_IMAGE_RESPONSE_FORMAT.value();
			}
		},

		/**
		 * The size of the image to generate.
		 */
		// 图片尺寸，格式为 宽x高，如 1024x1024
		REQUEST_IMAGE_SIZE {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_IMAGE_SIZE.value();
			}
		},

		/**
		 * The style of the image to generate.
		 */
		// 画面风格，如 vivid、natural
		REQUEST_IMAGE_STYLE {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_IMAGE_STYLE.value();
			}
		},

		// Response

		/**
		 * The unique identifier for the AI response.
		 */
		// 响应唯一标识，便于与服务商侧日志对账排障
		RESPONSE_ID {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_ID.value();
			}
		},

		/**
		 * The name of the model that generated the response.
		 */
		// 实际生成响应的模型名，可能与请求指定的模型名不同（服务端会做版本解析）
		RESPONSE_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.RESPONSE_MODEL.value();
			}
		},

		// Usage

		/**
		 * The number of tokens used in the model input (prompt).
		 */
		// 输入（提示词）消耗的 token 数
		USAGE_INPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_INPUT_TOKENS.value();
			}
		},

		/**
		 * The number of tokens used in the model output (generation).
		 */
		// 输出（生成内容）消耗的 token 数
		USAGE_OUTPUT_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_OUTPUT_TOKENS.value();
			}
		},

		/**
		 * The total number of tokens used in the model exchange.
		 */
		// 本次交互消耗的 token 总数（通常为输入 + 输出）
		USAGE_TOTAL_TOKENS {
			@Override
			public String asString() {
				return AiObservationAttributes.USAGE_TOTAL_TOKENS.value();
			}
		}

	}

}
