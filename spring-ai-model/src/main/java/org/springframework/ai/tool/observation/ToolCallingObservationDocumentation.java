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

package org.springframework.ai.tool.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

import org.springframework.ai.observation.conventions.AiObservationAttributes;

/**
 * Tool calling observation documentation.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用可观测性的「文档化定义」：以枚举形式集中声明本模块会产生哪些观测、 每个观测有哪些标签名。
 *
 * <p>
 * 这是 Micrometer 推荐的做法——实现 {@link ObservationDocumentation} 后，可以借助工具 <b>自动生成监控文档</b>，同时也让标签名称有了唯一的
 * 「单一事实来源」，避免各处硬编码字符串导致拼写不一致。
 *
 * <p>
 * 结构说明：
 * <ul>
 * <li>外层枚举只有一个常量 {@code TOOL_CALL}，代表「工具调用」这一类观测；</li>
 * <li>内嵌枚举 {@code LowCardinalityKeyNames}：低基数标签名，可用作指标维度；</li>
 * <li>内嵌枚举 {@code HighCardinalityKeyNames}：高基数标签名，仅用于链路追踪。</li>
 * </ul>
 *
 * <p>
 * 注意：常量使用了「带方法体的枚举常量」写法（{@code TOOL_CALL { ... }}）， 即每个常量都是一个匿名子类，从而可以各自重写方法。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public enum ToolCallingObservationDocumentation implements ObservationDocumentation {

	/**
	 * Tool calling observations.
	 */
	/**
	 * 【中文说明】工具调用观测。通过重写三个方法，声明其默认约定实现与全部标签名。
	 */
	TOOL_CALL {
		// 声明默认使用的约定实现类；用户未自定义 Convention 时由框架回退到它
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultToolCallingObservationConvention.class;
		}

		// 声明全部低基数标签名（values() 返回内嵌枚举的所有常量）
		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		// 声明全部高基数标签名
		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

	};

	/**
	 * Low cardinality key names.
	 */
	/**
	 * 【中文说明】低基数标签名枚举。
	 * <p>
	 * 「低基数」指取值种类有限（如工具名、工具类型），可安全地作为监控系统的指标维度； 每个常量通过重写 {@code asString()}
	 * 返回其在监控系统中的实际标签名。
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The name of the operation being performed.
		 */
		// 【中文】操作类型，取值来自 AI 通用语义约定（此处为 execute_tool）
		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				// 复用统一的属性名常量，保证与其它 AI 观测（聊天、嵌入等）命名一致
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		/**
		 * The provider responsible for the operation.
		 */
		// 【中文】操作的提供方（此处为 spring_ai）
		AI_PROVIDER {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_PROVIDER.value();
			}
		},

		/**
		 * Spring AI kind.
		 */
		// 【中文】Spring AI 观测种类，用于区分 tool_call / chat_client 等
		SPRING_AI_KIND {
			@Override
			public String asString() {
				// 该标签为 Spring AI 私有，故直接硬编码而非取自通用约定
				return "spring.ai.kind";
			}
		},

		/**
		 * The name of the tool.
		 */
		// 【中文】工具名称，最常用的聚合维度
		TOOL_DEFINITION_NAME {
			@Override
			public String asString() {
				return "spring.ai.tool.definition.name";
			}
		},

		/**
		 * The type of the tool.
		 */
		// 【中文】工具类型，通常为 function
		TOOL_TYPE {
			@Override
			public String asString() {
				return "spring.ai.tool.type";
			}
		},

	}

	/**
	 * High cardinality key names.
	 */
	/**
	 * 【中文说明】高基数标签名枚举。
	 * <p>
	 * 「高基数」指取值近乎无限（如调用 ID、入参内容），若作为指标维度会造成时间序列爆炸， 因此这类标签通常只写入链路追踪（tracing）。
	 * <p>
	 * 其中 {@code TOOL_CALL_ARGUMENTS} 与 {@code TOOL_CALL_RESULT} 涉及敏感数据，默认<b>不</b>记录，
	 * 需显式启用 {@link ToolCallingContentObservationFilter} 后才会写入。
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * Description of the tool.
		 */
		// 【中文】工具描述文本
		TOOL_DEFINITION_DESCRIPTION {
			@Override
			public String asString() {
				return "spring.ai.tool.definition.description";
			}
		},

		/**
		 * Schema of the parameters used to call the tool.
		 */
		// 【中文】工具入参的 JSON Schema
		TOOL_DEFINITION_SCHEMA {
			@Override
			public String asString() {
				return "spring.ai.tool.definition.schema";
			}
		},

		/**
		 * The ID of the tool call.
		 */
		// 【中文】本次工具调用的唯一 ID，可用于关联模型请求与工具执行
		TOOL_CALL_ID {
			@Override
			public String asString() {
				return "spring.ai.tool.call.id";
			}
		},

		/**
		 * The input arguments to the tool call.
		 */
		// 【中文】工具入参内容 —— 可能含敏感数据，默认不记录
		TOOL_CALL_ARGUMENTS {
			@Override
			public String asString() {
				return "spring.ai.tool.call.arguments";
			}
		},

		/**
		 * The result of the tool call.
		 */
		// 【中文】工具执行结果 —— 可能含敏感数据，默认不记录
		TOOL_CALL_RESULT {
			@Override
			public String asString() {
				return "spring.ai.tool.call.result";
			}
		}

	}

}
