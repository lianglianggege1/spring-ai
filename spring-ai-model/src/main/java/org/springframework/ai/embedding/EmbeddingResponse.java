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
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResponse;
import org.springframework.util.Assert;

/**
 * Embedding response object.
 */
/**
 * 嵌入调用的响应对象：承载一次调用返回的<b>全部</b>向量结果及响应级元数据。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code embeddings}：向量结果列表，与请求中的输入文本按下标一一对应；</li>
 * <li>{@code metadata}：响应级元数据，包含模型名、token 用量（{@code Usage}）等信息，
 * 用于成本统计与可观测性。</li>
 * </ul>
 *
 * <p>
 * 便捷方法：{@code getResult()} 直接取第一条结果（适用于单条文本场景），
 * {@code getResults()} 取全部结果。
 */
public class EmbeddingResponse implements ModelResponse<Embedding> {

	/**
	 * Embedding data.
	 */
	// 向量结果列表，顺序与请求中的输入一一对应
	private final List<Embedding> embeddings;

	/**
	 * Embedding metadata.
	 */
	// 响应级元数据：模型名、token 用量等
	private final EmbeddingResponseMetadata metadata;

	/**
	 * Creates a new {@link EmbeddingResponse} instance with empty metadata.
	 * @param embeddings the embedding data.
	 */
	// 便捷构造器：不关心元数据时使用，自动填入一个空的元数据对象，避免调用方拿到 null
	public EmbeddingResponse(List<Embedding> embeddings) {
		this(embeddings, new EmbeddingResponseMetadata());
	}

	/**
	 * Creates a new {@link EmbeddingResponse} instance.
	 * @param embeddings the embedding data.
	 * @param metadata the embedding metadata.
	 */
	// 全参构造器：两个字段均为 final，响应对象构建后不可变
	public EmbeddingResponse(List<Embedding> embeddings, EmbeddingResponseMetadata metadata) {
		this.embeddings = embeddings;
		this.metadata = metadata;
	}

	/**
	 * @return Get the embedding metadata.
	 */
	// 返回响应级元数据（模型名、token 用量等），保证非 null
	public EmbeddingResponseMetadata getMetadata() {
		return this.metadata;
	}

	// 取第一条向量结果，适用于单条文本输入的场景
	@Override
	public Embedding getResult() {
		// 空结果保护：列表为空时 get(0) 会抛下标越界，这里提前抛出语义清晰的异常
		Assert.notEmpty(this.embeddings, "No embedding data available.");
		return this.embeddings.get(0);
	}

	/**
	 * @return Get the embedding data.
	 */
	// 返回全部向量结果
	@Override
	public List<Embedding> getResults() {
		return this.embeddings;
	}

	// 相等性判断：同时比较向量列表与元数据
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 空值或类型不同，直接判定不等
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EmbeddingResponse that = (EmbeddingResponse) o;
		return Objects.equals(this.embeddings, that.embeddings) && Objects.equals(this.metadata, that.metadata);
	}

	// hashCode 与 equals 使用相同的字段，保证契约一致
	@Override
	public int hashCode() {
		return Objects.hash(this.embeddings, this.metadata);
	}

	// 注意：这里的字符串前缀是 "EmbeddingResult" 而非类名 EmbeddingResponse（历史遗留写法）
	@Override
	public String toString() {
		return "EmbeddingResult{" + "data=" + this.embeddings + ", metadata=" + this.metadata + '}';
	}

}
