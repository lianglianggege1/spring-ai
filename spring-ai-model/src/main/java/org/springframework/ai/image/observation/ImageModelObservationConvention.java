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

package org.springframework.ai.image.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for image model exchanges.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 图像模型调用的「观测约定」接口。
 * <p>
 * 所谓 ObservationConvention，是 Micrometer 中用于决定「一次观测叫什么名字、带哪些标签」的策略对象。
 * 实现本接口即可自定义图像模型调用所产生的 metrics/trace 的命名与标签，
 * 框架默认实现见 {@link DefaultImageModelObservationConvention}。
 * <p>
 * 用法：自定义一个实现类并注册为 Bean，或传给具体 ImageModel 实现，即可覆盖默认观测行为。
 *
 * @since 1.0.0
 */
public interface ImageModelObservationConvention extends ObservationConvention<ImageModelObservationContext> {

	// 类型守卫：Micrometer 会用它判断当前约定能否处理某个观测上下文。
	// 这里只接受图像模型上下文，避免把约定误用到对话、嵌入等其它模态的观测上。
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ImageModelObservationContext;
	}

}
