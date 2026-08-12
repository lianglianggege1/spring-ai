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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for tool calling observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用可观测性「约定」接口：定义如何把一次工具调用转换成 Micrometer 的 指标名称与标签（KeyValues）。
 *
 * <p>
 * 在 Micrometer Observation 模型中，{@code ObservationConvention} 负责回答三个问题：
 * <ul>
 * <li>这次观测叫什么名字（指标名）；</li>
 * <li>低基数标签有哪些（可安全用作指标维度，如工具名、类型）；</li>
 * <li>高基数标签有哪些（取值空间大，通常只进 tracing，如调用 ID、入参）。</li>
 * </ul>
 *
 * <p>
 * 本接口通过泛型把上下文类型收窄为 {@link ToolCallingObservationContext}，从而在实现类中 免去手动强制类型转换。
 *
 * <p>
 * 典型用法：实现本接口并注册为 Bean，即可自定义工具调用的指标名与标签； 默认实现见
 * {@link DefaultToolCallingObservationConvention}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ToolCallingObservationConvention extends ObservationConvention<ToolCallingObservationContext> {

	/**
	 * 【中文说明】判断当前约定是否适用于给定的观测上下文。
	 * <p>
	 * Micrometer 会遍历所有已注册的 Convention 并逐个询问本方法， 因此这里用 {@code instanceof}
	 * 过滤：只处理工具调用类型的上下文，避免误处理 聊天模型、向量库等其他类型的观测。
	 * @param context 任意观测上下文
	 * @return 当且仅当上下文为 {@link ToolCallingObservationContext} 时返回 true
	 */
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ToolCallingObservationContext;
	}

}
