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
 * Enumeration of metric names used in AI observations.
 * <p>
 * Based on OpenTelemetry's Semantic Conventions for AI systems.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * Semantic Conventions</a>.
 */
/**
 * AI观测所使用的指标名称枚举。
 * <p>
 * 基于OpenTelemetry人工智能系统语义约定。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * 语义约定</a>.
 */
public enum AiObservationMetricNames {

	/**
	 * The duration of the AI operation.
	 */
	/**
	 * AI操作的耗时。
	 */
	OPERATION_DURATION("gen_ai.client.operation.duration"),
	/**
	 * The number of AI operations.
	 */
	/**
	 * AI操作的次数。
	 */
	TOKEN_USAGE("gen_ai.client.token.usage");

	private final String value;

	AiObservationMetricNames(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the metric name.
	 * @return the value of the metric name
	 */
	/**
	 * 返回指标名称对应的值。
	 * @return 指标名称对应的值
	 */
	public String value() {
		return this.value;
	}

}
