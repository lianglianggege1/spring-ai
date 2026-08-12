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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

import org.springframework.ai.model.observation.ModelUsageMetricsGenerator;

/**
 * Handler for generating metrics from chat model observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelMeterObservationHandler 负责把对话观测中的 token 用量转换成 Micrometer 指标（Meter）。
 *
 * <p>
 * 与两个日志类 Handler 不同，它产出的是可聚合的<b>指标数据</b>（如
 * {@code gen_ai.client.token.usage} 计数器），可直接对接 Prometheus、Grafana 等监控系统，
 * 用于统计 token 消耗、估算调用成本。
 *
 * <p>
 * 关键字段：{@code meterRegistry} —— Micrometer 的指标注册中心，通过构造器注入，
 * 实际的指标创建与累加委托给 {@code ModelUsageMetricsGenerator} 完成。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * observationRegistry.observationConfig()
 *     .observationHandler(new ChatModelMeterObservationHandler(meterRegistry));
 * }</pre>
 */
public class ChatModelMeterObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	// 中文说明：Micrometer 指标注册中心，所有生成的指标都会注册到这里
	private final MeterRegistry meterRegistry;

	// 中文说明：唯一构造器，通过依赖注入传入 MeterRegistry
	public ChatModelMeterObservationHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	// 中文说明：观测结束时生成指标。
	// 这里是一条三重判空的"安全链"：response -> metadata -> usage，任一环为 null 就跳过，
	// 因为请求失败、流式中途出错等场景下拿不到用量数据，此时不应产出指标（否则会污染统计）。
	@Override
	public void onStop(ChatModelObservationContext context) {
		if (context.getResponse() != null && context.getResponse().getMetadata() != null
				&& context.getResponse().getMetadata().getUsage() != null) {
			ModelUsageMetricsGenerator.generate(context.getResponse().getMetadata().getUsage(), context,
					this.meterRegistry);
		}
	}

	// 中文说明：类型守卫，只处理对话模型的观测上下文
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ChatModelObservationContext;
	}

}
