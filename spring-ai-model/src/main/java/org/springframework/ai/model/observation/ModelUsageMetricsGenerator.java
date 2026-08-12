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

package org.springframework.ai.model.observation;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.Observation;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.observation.conventions.AiObservationMetricAttributes;
import org.springframework.ai.observation.conventions.AiObservationMetricNames;
import org.springframework.ai.observation.conventions.AiTokenType;

/**
 * Generate metrics about the model usage in the context of an AI operation.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ModelUsageMetricsGenerator 负责把模型调用的 <b>token 用量</b>转换为
 * Micrometer 指标（Metrics），从而可以在 Prometheus / Grafana 等系统中监控 AI 成本。
 *
 * <p>
 * 产出的指标：统一使用同一个计数器名（{@code AiObservationMetricNames.TOKEN_USAGE}），
 * 通过 {@code token.type} 标签区分三类用量：
 * <ul>
 * <li>input —— 提示词消耗的 token；</li>
 * <li>output —— 模型生成消耗的 token；</li>
 * <li>total —— 合计。</li>
 * </ul>
 * 采用 {@link Counter}（单调递增计数器）而非 Gauge，因为 token 消耗是累计量。
 *
 * <p>
 * 关键设计：final class + 私有构造器，标准的静态工具类。
 *
 * <p>
 * 典型用法：由观测处理器在模型调用结束后调用
 * {@code ModelUsageMetricsGenerator.generate(usage, context, meterRegistry)}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class ModelUsageMetricsGenerator {

	// 【中文】指标描述文案，会作为 Counter 的 description 暴露给监控系统。
	private static final String DESCRIPTION = "Measures number of input and output tokens used";

	// 【中文】私有构造器：禁止实例化（纯静态工具类）。
	private ModelUsageMetricsGenerator() {
	}

	// 【中文】核心方法：从 Usage 中读取三类 token 数并分别累加到对应的计数器。
	// 三段逻辑结构完全相同，仅 token.type 标签与取值来源不同。
	public static void generate(Usage usage, Observation.Context context, MeterRegistry meterRegistry) {

		// 空值处理：并非所有厂商都返回完整的 token 统计，
		// 因此每一项都要先判空，缺失时跳过而不是记为 0（记 0 会掩盖"数据缺失"这一事实）
		if (usage.getPromptTokens() != null) {
			// 输入（提示词）token 计数器：
			// register() 具有幂等性——同名同标签的计数器只会创建一次，之后返回已有实例，
			// 因此这里每次调用都重新 builder 并不会重复注册
			Counter.builder(AiObservationMetricNames.TOKEN_USAGE.value())
				.tag(AiObservationMetricAttributes.TOKEN_TYPE.value(), AiTokenType.INPUT.value())
				.description(DESCRIPTION)
				.tags(createTags(context))
				.register(meterRegistry)
				.increment(usage.getPromptTokens());
		}

		// 空值处理：输出 token 同样可能缺失
		if (usage.getCompletionTokens() != null) {
			Counter.builder(AiObservationMetricNames.TOKEN_USAGE.value())
				// 与上段的唯一区别：token.type 标签值为 output
				.tag(AiObservationMetricAttributes.TOKEN_TYPE.value(), AiTokenType.OUTPUT.value())
				.description(DESCRIPTION)
				.tags(createTags(context))
				.register(meterRegistry)
				.increment(usage.getCompletionTokens());
		}

		// 空值处理：总量单独上报而非由前两者相加，
		// 因为某些厂商的 total 还包含推理 token 等额外部分，直接相加会失真
		if (usage.getTotalTokens() != null) {
			Counter.builder(AiObservationMetricNames.TOKEN_USAGE.value())
				.tag(AiObservationMetricAttributes.TOKEN_TYPE.value(), AiTokenType.TOTAL.value())
				.description(DESCRIPTION)
				.tags(createTags(context))
				.register(meterRegistry)
				.increment(usage.getTotalTokens());
		}

	}

	// 【中文】把观测上下文中的键值对转换为 Micrometer 的 Tag 列表，作为指标的公共维度
	// （如 gen_ai.operation.name、gen_ai.system、模型名等）。
	//
	// 关键点：这里只取 <b>低基数</b>（low cardinality）键值对。
	// 低基数指取值种类有限的维度（如模型名）；而高基数维度（如具体的提示词内容、请求 ID）
	// 若作为指标标签会导致时间序列爆炸、拖垮监控系统，因此被刻意排除，
	// 它们只用于链路追踪 span 而不用于指标。
	private static List<Tag> createTags(Observation.Context context) {
		List<Tag> tags = new ArrayList<>();
		for (KeyValue keyValue : context.getLowCardinalityKeyValues()) {
			tags.add(Tag.of(keyValue.getKey(), keyValue.getValue()));
		}
		return tags;
	}

}
