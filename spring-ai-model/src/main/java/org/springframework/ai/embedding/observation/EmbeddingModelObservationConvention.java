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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for embedding model exchanges.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 嵌入模型观测约定的接口，是 Micrometer {@link ObservationConvention} 的类型化特化版本。
 *
 * <p>
 * 它把泛型参数固定为 {@link EmbeddingModelObservationContext}，并提供
 * {@code supportsContext} 的默认实现，使自定义约定只需关心"打什么标签"，
 * 无需重复编写类型判断代码。
 *
 * <p>
 * 默认实现见 {@link DefaultEmbeddingModelObservationConvention}；
 * 若需自定义指标名或标签，实现本接口并注入到 EmbeddingModel 即可。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface EmbeddingModelObservationConvention extends ObservationConvention<EmbeddingModelObservationContext> {

	// 默认实现：仅当上下文类型匹配时才应用本约定，避免误处理其它模型类型的观测
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof EmbeddingModelObservationContext;
	}

}
