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

import org.springframework.ai.model.Model;

/**
 * EmbeddingModel is a generic interface for embedding models.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 面向 {@link org.springframework.ai.document.Document} 的嵌入模型接口。
 *
 * <p>
 * 与 {@link EmbeddingModel}（输入是纯字符串 String）的区别：本接口的输入是携带元数据的
 * Document 对象，因此模型实现有机会利用文档的 metadata（如标题、来源）参与向量化，
 * 更贴合 RAG 场景下的文档入库流程。
 *
 * <p>
 * 泛型说明：继承 {@code Model<DocumentEmbeddingRequest, EmbeddingResponse>}，
 * 即"请求类型 -&gt; 响应类型"这一 Spring AI 统一的模型调用抽象。
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public interface DocumentEmbeddingModel extends Model<DocumentEmbeddingRequest, EmbeddingResponse> {

	// 执行一次文档嵌入调用：传入待向量化的文档集合与选项，返回包含向量结果与用量元数据的响应
	@Override
	EmbeddingResponse call(DocumentEmbeddingRequest request);

	// 返回该模型输出向量的维度，供向量库建表/校验使用
	int dimensions();

}
