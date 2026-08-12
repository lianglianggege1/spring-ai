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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for chat model exchanges.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelObservationConvention 是对话模型观测的"约定（Convention）"接口。
 *
 * <p>
 * 在 Micrometer Observation 体系中，Convention 负责回答三个问题：
 * <ul>
 * <li>这次观测叫什么名字（{@code getName()}，用于指标名）；</li>
 * <li>上下文名称是什么（{@code getContextualName()}，用于链路追踪的 span 名）；</li>
 * <li>要打上哪些标签（低基数 {@code getLowCardinalityKeyValues()} 用于指标维度，
 * 高基数 {@code getHighCardinalityKeyValues()} 仅用于追踪）。</li>
 * </ul>
 * 这些方法均继承自父接口 {@code ObservationConvention}，本接口只是把泛型固定为
 * {@link ChatModelObservationContext}，并提供类型判断的默认实现。
 *
 * <p>
 * 扩展方式：想自定义标签时，通常继承 {@code DefaultChatModelObservationConvention} 并覆写
 * 相应的 protected 方法，然后把实例注入到 ChatModel 中，而不是从零实现本接口。
 */
public interface ChatModelObservationConvention extends ObservationConvention<ChatModelObservationContext> {

	// 中文说明：类型守卫。Micrometer 会把所有 Convention 依次询问"你能处理这个上下文吗"，
	// 这里通过 instanceof 判断，只认领 ChatModelObservationContext，避免误处理其他类型的观测
	// （如 EmbeddingModel、VectorStore 的观测上下文）。
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ChatModelObservationContext;
	}

}
