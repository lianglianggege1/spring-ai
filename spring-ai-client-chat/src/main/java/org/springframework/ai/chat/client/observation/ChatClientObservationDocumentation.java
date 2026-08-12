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

package org.springframework.ai.chat.client.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documented conventions for chat client observations.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 聊天客户端观测指标的文档化约定。
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public enum ChatClientObservationDocumentation implements ObservationDocumentation {

	/**
	 * AI Chat Client observations
	 */
	/**
	 * AI聊天客户端观测指标
	 */
	AI_CHAT_CLIENT {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultChatClientObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

	};

	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Spring AI kind.
		 */
		/**
		 * Spring‑AI 类型标识。
		 */
		SPRING_AI_KIND {
			@Override
			public String asString() {
				return "spring.ai.kind";
			}
		},

		/**
		 * Is the chat model response a stream.
		 */
		/**
		 * 标记聊天模型响应是否为流式返回。
		 */
		STREAM {
			@Override
			public String asString() {
				return "spring.ai.chat.client.stream";
			}
		}

	}

	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * List of configured chat client advisors.
		 */
		/**
		 * 已配置的聊天客户端顾问列表。
		 */
		CHAT_CLIENT_ADVISORS {
			@Override
			public String asString() {
				return "spring.ai.chat.client.advisors";
			}
		},

		/**
		 * The identifier of the conversation.
		 */
		/**
		 * 会话唯一标识。
		 */
		CHAT_CLIENT_CONVERSATION_ID {
			@Override
			public String asString() {
				return "spring.ai.chat.client.conversation.id";
			}
		},

		// Request

		/**
		 * Names of the tools made available to the chat client.
		 */
		/**
		 * 聊天客户端可用的工具名称列表。
		 */
		CHAT_CLIENT_TOOL_NAMES {
			@Override
			public String asString() {
				return "spring.ai.chat.client.tool.names";
			}
		}

	}

}
