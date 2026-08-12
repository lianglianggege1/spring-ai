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

/**
 * Provides the API for embedding observations.
 */

/**
 * 嵌入（embedding）能力的核心 API 包。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@link org.springframework.ai.embedding.EmbeddingModel}：核心接口，文本 -&gt; 向量；</li>
 * <li>{@link org.springframework.ai.embedding.AbstractEmbeddingModel}：带维度缓存的抽象基类；</li>
 * <li>请求/响应模型：{@code EmbeddingRequest}、{@code EmbeddingResponse}、{@code Embedding}；</li>
 * <li>参数体系：{@code EmbeddingOptions} 及其默认实现与构建器；</li>
 * <li>分批策略：{@code BatchingStrategy} 与 {@code TokenCountBatchingStrategy}。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示：本包内所有类型<b>默认不可为 null</b>，
 * 只有显式标注 {@code @Nullable} 的地方才允许为 null，便于静态分析工具做空指针检查。
 */
@NullMarked
package org.springframework.ai.embedding;

import org.jspecify.annotations.NullMarked;
