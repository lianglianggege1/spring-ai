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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

import org.springframework.ai.model.observation.ModelUsageMetricsGenerator;

/**
 * Handler for generating metrics from embedding model observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 观测处理器：把嵌入调用的 <b>token 用量</b>转换成 Micrometer 指标。
 *
 * <p>
 * 与 Convention 的分工：Convention 负责"给观测打什么标签"，本 Handler 负责
 * "在观测的某个生命周期节点做什么额外动作"——这里是在调用结束时额外产出用量指标
 * （如 {@code gen_ai.client.token.usage}），便于按 token 统计成本。
 *
 * <p>
 * 关键字段：{@code meterRegistry}，Micrometer 的指标注册中心，所有指标最终注册到它上面。
 *
 * <p>
 * 典型用法：注册到 {@code ObservationRegistry} 后自动生效：
 * <pre>{@code
 * observationRegistry.observationConfig()
 *         .observationHandler(new EmbeddingModelMeterObservationHandler(meterRegistry));
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class EmbeddingModelMeterObservationHandler implements ObservationHandler<EmbeddingModelObservationContext> {

	// Micrometer 指标注册中心
	private final MeterRegistry meterRegistry;

	public EmbeddingModelMeterObservationHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	// 在观测结束（调用完成）时触发：若响应中带有 token 用量，则生成对应指标
	@Override
	public void onStop(EmbeddingModelObservationContext context) {
		// 三重判空：调用失败时 response 为 null，某些厂商也可能不返回 metadata / usage，
		// 任一环节缺失就跳过打点，不能因监控代码本身抛 NPE 影响主流程
		if (context.getResponse() != null && context.getResponse().getMetadata() != null
				&& context.getResponse().getMetadata().getUsage() != null) {
			ModelUsageMetricsGenerator.generate(context.getResponse().getMetadata().getUsage(), context,
					this.meterRegistry);
		}
	}

	// 类型过滤：只处理嵌入模型的观测上下文，其它类型（如 chat）交给各自的 Handler
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof EmbeddingModelObservationContext;
	}

}
