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

package org.springframework.ai.observation.conventions;

/**
 * Types of Spring AI constructs which can be observed.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 可被观测的Spring AI组件类型。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public enum SpringAiKind {

	// @formatter:off

	/**
	 * Spring AI kind for advisor.
	 */
	/**
	 * Advisor对应的Spring AI类型。
	 */
	ADVISOR("advisor"),

	/**
	 * Spring AI kind for chat client.
	 */
	/**
	 * ChatClient对应的Spring AI类型。
	 */
	CHAT_CLIENT("chat_client"),

	/**
	 * Spring AI kind for tool calling.
	 */
	/**
	 * ToolCalling对应的Spring AI类型。
	 */
	TOOL_CALL("tool_call"),

	/**
	 * Spring AI kind for vector store.
	 */
	/**
	 * VectorStore对应的Spring AI类型。
	 */
	VECTOR_STORE("vector_store");

	private final String value;

	SpringAiKind(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the Spring AI kind.
	 * @return the value of the Spring AI kind
	 */
	/**
	 * 返回Spring AI类型对应的值。
	 * @return Spring AI类型对应的值
	 */
	public String value() {
		return this.value;
	}

	// @formatter:on

}
