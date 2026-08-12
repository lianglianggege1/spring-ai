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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.document.Document;
import org.springframework.ai.model.Model;
import org.springframework.util.Assert;

/**
 * EmbeddingModel is a generic interface for embedding models.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Soby Chacko
 * @author Jihoon Kim
 * @since 1.0.0
 *
 */
/**
 * 嵌入模型的<b>核心接口</b>，Spring AI 中所有文本向量化能力的统一入口。
 *
 * <p>
 * 所谓 embedding，就是把文本映射为一个高维浮点向量，语义相近的文本其向量距离也相近，
 * 这是 RAG（检索增强生成）和语义搜索的基础。
 *
 * <p>
 * 接口设计：只有 {@code call(EmbeddingRequest)} 和 {@code embed(Document)} 两个抽象方法
 * 需要厂商实现，其余全部是 {@code default} 便捷方法，内部最终都收敛到 {@code call()}。
 * 这样既降低了接入成本，又给调用方提供了多种粒度的 API：
 * <ul>
 * <li>{@code embed(String)}：单条文本 -&gt; 单个向量，最常用；</li>
 * <li>{@code embed(List&lt;String&gt;)}：批量文本 -&gt; 批量向量；</li>
 * <li>{@code embed(List&lt;Document&gt;, options, batchingStrategy)}：批量文档，
 * 并按策略自动分批，规避单次请求 token 上限；</li>
 * <li>{@code embedForResponse(List&lt;String&gt;)}：需要拿到 token 用量等元数据时使用；</li>
 * <li>{@code dimensions()}：获取向量维度。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * float[] vector = embeddingModel.embed("你好，世界");
 * int dim = embeddingModel.dimensions();
 * }</pre>
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Soby Chacko
 * @author Jihoon Kim
 * @since 1.0.0
 */
public interface EmbeddingModel extends Model<EmbeddingRequest, EmbeddingResponse> {

	// 唯一的底层调用方法：所有 default 便捷方法最终都通过它发起真实请求，由各厂商实现
	@Override
	EmbeddingResponse call(EmbeddingRequest request);

	/**
	 * Embeds the given text into a vector.
	 * @param text the text to embed.
	 * @return the embedded vector.
	 */
	// 单条文本向量化：内部包装成单元素列表复用批量逻辑，再取出第一个结果
	default float[] embed(String text) {
		// 入参校验：null 会导致后续 List.of 抛异常，这里提前给出清晰的错误信息
		Assert.notNull(text, "Text must not be null");
		List<float[]> response = this.embed(List.of(text));
		// 只有一条输入，因此必然只有一条输出，直接取迭代器首元素
		return response.iterator().next();
	}

	/**
	 * Embeds the given document's content into a vector.
	 * @param document the document to embed.
	 * @return the embedded vector.
	 */
	// 抽象方法：由实现类决定如何从 Document 中取内容（是否带 metadata）并向量化
	float[] embed(Document document);

	/**
	 * Extracts the text content from a {@link Document} to be used for embedding. By
	 * default, returns {@link Document#getText()}. Implementations that support
	 * {@link org.springframework.ai.document.MetadataMode} should override this method to
	 * return
	 * {@link Document#getFormattedContent(org.springframework.ai.document.MetadataMode)}
	 * with the appropriate metadata mode, so that metadata is included in the text sent
	 * to the embedding API.
	 * @param document the document to extract embedding content from.
	 * @return the text content to embed.
	 */
	/**
	 * 提取 {@link Document} 中真正参与向量化的文本内容。
	 *
	 * <p>
	 * 这是一个<b>扩展点</b>：默认只取正文 {@code getText()}；若实现类支持
	 * {@link org.springframework.ai.document.MetadataMode}，应重写本方法改用
	 * {@code getFormattedContent(metadataMode)}，从而把标题、来源等元数据一并送入嵌入接口，
	 * 通常能提升检索效果。
	 * @param document 待提取内容的文档
	 * @return 用于嵌入的文本，可能为 null
	 */
	default @Nullable String getEmbeddingContent(Document document) {
		// 入参校验：文档不能为空
		Assert.notNull(document, "Document must not be null");
		// 默认策略：只取正文，不含任何元数据
		return document.getText();
	}

	/**
	 * Embeds a batch of texts into vectors.
	 * @param texts list of texts to embed.
	 * @return list of embedded vectors.
	 */
	// 批量文本向量化：构造请求 -> 调用 -> 从每个 Embedding 中抽出 float[] 组成列表
	default List<float[]> embed(List<String> texts) {
		Assert.notNull(texts, "Texts must not be null");
		// 使用空的默认 options，即全部参数沿用模型默认值
		return this.call(new EmbeddingRequest(texts, EmbeddingOptions.builder().build()))
			.getResults()
			.stream()
			.map(Embedding::getOutput)
			.toList();
	}

	/**
	 * Embeds a batch of {@link Document}s into vectors based on a
	 * {@link BatchingStrategy}.
	 * @param documents list of {@link Document}s.
	 * @param options {@link EmbeddingOptions}.
	 * @param batchingStrategy {@link BatchingStrategy}.
	 * @return a list of float[] that represents the vectors for the incoming
	 * {@link Document}s. The returned list is expected to be in the same order of the
	 * {@link Document} list.
	 */
	/**
	 * 按分批策略批量向量化文档列表。
	 *
	 * <p>
	 * 这是文档入库（RAG 建索引）的主力方法。流程为：先用 {@link BatchingStrategy}
	 * 把文档切成若干子批次以规避单次请求的 token 上限，再逐批调用模型，
	 * 最后把各批结果<b>按原顺序</b>拼接返回。
	 * @param documents 待向量化的文档列表
	 * @param options 嵌入参数，可为 null
	 * @param batchingStrategy 分批策略
	 * @return 与入参文档顺序一一对应的向量列表
	 */
	default List<float[]> embed(List<Document> documents, @Nullable EmbeddingOptions options,
			BatchingStrategy batchingStrategy) {
		Assert.notNull(documents, "Documents must not be null");
		// 预分配容量，避免 ArrayList 反复扩容
		List<float[]> embeddings = new ArrayList<>(documents.size());
		// 第一步：按策略切分为多个子批次（每批的 token 总量在模型限制内）
		List<List<Document>> batch = batchingStrategy.batch(documents);
		// 第二步：逐批调用模型；外层 for 保证批次间顺序，内层 for 保证批次内顺序
		for (List<Document> subBatch : batch) {
			// 通过可重写的 getEmbeddingContent 提取文本，从而尊重实现类的 MetadataMode 设置
			List<String> texts = subBatch.stream().map(this::getEmbeddingContent).toList();
			EmbeddingRequest request = new EmbeddingRequest(texts, options);
			EmbeddingResponse response = this.call(request);
			// 按下标顺序取出结果，确保向量与文档严格对应
			for (int i = 0; i < subBatch.size(); i++) {
				embeddings.add(response.getResults().get(i).getOutput());
			}
		}
		// 兜底断言：输入输出数量必须一致，否则说明某个实现丢批或乱序了，属于严重错误
		Assert.isTrue(embeddings.size() == documents.size(),
				"Embeddings must have the same number as that of the documents");
		return embeddings;
	}

	/**
	 * Embeds a batch of texts into vectors and returns the {@link EmbeddingResponse}.
	 * @param texts list of texts to embed.
	 * @return the embedding response.
	 */
	// 与 embed(List<String>) 的区别：返回完整响应对象，可进一步读取 token 用量等元数据
	default EmbeddingResponse embedForResponse(List<String> texts) {
		Assert.notNull(texts, "Texts must not be null");
		return this.call(new EmbeddingRequest(texts, EmbeddingOptions.builder().build()));
	}

	/**
	 * Get the number of dimensions of the embedded vectors. Note that by default, this
	 * method will call the remote Embedding endpoint to get the dimensions of the
	 * embedded vectors. If the dimensions are known ahead of time, it is recommended to
	 * override this method.
	 * @return the number of dimensions of the embedded vectors.
	 */
	// 获取向量维度。注意：默认实现会真实发起一次远程调用（有耗时与费用），
	// 若维度事先已知，强烈建议重写本方法或继承 AbstractEmbeddingModel 以获得缓存能力
	default int dimensions() {
		return embed("Test String").length;
	}

}
