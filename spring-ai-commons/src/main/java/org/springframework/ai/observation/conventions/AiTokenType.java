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
 * Types of tokens produced and consumed in an AI operation. Based on the OpenTelemetry
 * Semantic Conventions for AI Systems.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * Semantic Conventions</a>.
 */
/**
 * AI操作中产生与消耗的Token类型。基于OpenTelemetry人工智能系统语义约定。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * 语义约定</a>.
 */
public enum AiTokenType {

// @formatter:off

	/**
	 * Input token.
	 */
	/**
	 * 输入Token。
	 */
	INPUT("input"),
	/**
	 * Output token.
	 */
	/**
	 * 输出Token。
	 */
	OUTPUT("output"),
	/**
	 * Total token.
	 */
	/**
	 * 总Token。
	 */
	TOTAL("total");

	private final String value;

	AiTokenType(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the token type.
	 * @return the value of the token type
	 */
	/**
	 * 返回Token类型对应的值。
	 * @return Token类型对应的值
	 */
	public String value() {
		return this.value;
	}

// @formatter:on

}
