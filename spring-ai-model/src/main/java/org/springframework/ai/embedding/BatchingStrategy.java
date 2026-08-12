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

package org.springframework.ai.embedding;

import java.util.List;

import org.springframework.ai.document.Document;

/**
 * Contract for batching {@link Document} objects so that the call to embed them could be
 * optimized.
 *
 * @author Soby Chacko
 * @since 1.0.0
 */
/**
 * 文档分批策略接口：把一大批 {@link Document} 拆成若干"子批次"，以优化嵌入调用。
 *
 * <p>
 * 存在的原因：嵌入模型 API 通常对<b>单次请求的 token 总数</b>和<b>条目数</b>有上限，
 * 一次性把上千篇文档丢过去会直接报错。该接口把切分逻辑抽象出来，便于替换不同策略。
 *
 * <p>
 * 关键约束：<b>必须保持文档的原始顺序</b>。因为返回的向量是按输入顺序一一对应回填到 Document 的，
 * 顺序错乱会导致文档与向量张冠李戴。
 *
 * <p>
 * 典型实现：{@link TokenCountBatchingStrategy}（按 token 数上限累加切分）。
 *
 * @author Soby Chacko
 * @since 1.0.0
 */
public interface BatchingStrategy {

	/**
	 * EmbeddingModel implementations can call this method to optimize embedding tokens.
	 * The incoming collection of {@link Document}s are split into sub-batches. It is
	 * important to preserve the order of the list of {@link Document}s when batching as
	 * they are mapped to their corresponding embeddings by their order.
	 * @param documents to batch
	 * @return a list of sub-batches that contain {@link Document}s.
	 */
	// 将文档列表切分为多个子批次；实现方必须保证切分前后文档的相对顺序不变
	List<List<Document>> batch(List<Document> documents);

}
