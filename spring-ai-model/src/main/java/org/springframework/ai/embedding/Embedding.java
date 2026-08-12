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

import java.util.Arrays;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResult;

/**
 * Represents a single embedding vector.
 */
/**
 * 表示<b>单条</b>嵌入向量结果。
 *
 * <p>
 * 一次嵌入调用可能同时处理多段文本，响应里会有多个 {@code Embedding}；每个实例代表其中一条。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code embedding}：向量本体，{@code float[]}（用基本类型数组而非 List&lt;Float&gt;，
 * 是为了避免装箱开销，向量动辄上千维）；</li>
 * <li>{@code index}：该向量在结果列表中的序号，用于和输入文本按序对应；</li>
 * <li>{@code metadata}：附加元数据（如模态类型、原始文档引用），默认是
 * {@link EmbeddingResultMetadata#EMPTY} 空对象，避免 null 判断。</li>
 * </ul>
 *
 * <p>
 * 实现 {@code ModelResult<float[]>}，因此可通过统一的 {@code getOutput()} 拿到向量。
 */
public class Embedding implements ModelResult<float[]> {

	// 向量本体；使用 float[] 基本类型数组以规避装箱开销
	private final float[] embedding;

	// 该向量在批量结果中的下标，与输入文本顺序一一对应
	private final Integer index;

	// 与该向量关联的元数据，默认为 EMPTY 空对象（空对象模式，免去调用方判空）
	private final EmbeddingResultMetadata metadata;

	/**
	 * Creates a new {@link Embedding} instance.
	 * @param embedding the embedding vector values.
	 * @param index the embedding index in a list of embeddings.
	 */
	// 便捷构造器：不关心元数据时使用，元数据自动填充为 EMPTY 空对象
	public Embedding(float[] embedding, Integer index) {
		this(embedding, index, EmbeddingResultMetadata.EMPTY);
	}

	/**
	 * Creates a new {@link Embedding} instance.
	 * @param embedding the embedding vector values.
	 * @param index the embedding index in a list of embeddings.
	 * @param metadata the metadata associated with the embedding.
	 */
	// 全参构造器：三个字段均为 final，实例创建后不可变
	public Embedding(float[] embedding, Integer index, EmbeddingResultMetadata metadata) {
		this.embedding = embedding;
		this.index = index;
		this.metadata = metadata;
	}

	/**
	 * @return Get the embedding vector values.
	 */
	// 返回向量本体（注意：直接返回内部数组引用，调用方不应修改其内容）
	@Override
	public float[] getOutput() {
		return this.embedding;
	}

	/**
	 * @return Get the embedding index in a list of embeddings.
	 */
	// 返回该向量在批量结果中的序号
	public Integer getIndex() {
		return this.index;
	}

	/**
	 * @return Get the metadata associated with the embedding.
	 */
	// 返回关联的元数据，保证非 null
	public EmbeddingResultMetadata getMetadata() {
		return this.metadata;
	}

	// 相等性判断：仅比较向量内容与下标，不比较 metadata
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用直接判等，快速路径
		if (this == o) {
			return true;
		}
		// 空值与类型不同一律不等
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Embedding other = (Embedding) o;
		// 数组必须用 Arrays.equals 逐元素比较，直接 == 或 equals 只会比较引用地址
		return Arrays.equals(this.embedding, other.embedding) && Objects.equals(this.index, other.index);
	}

	// hashCode 与 equals 保持一致：同样只基于向量内容和下标计算
	@Override
	public int hashCode() {
		// 数组需先用 Arrays.hashCode 求内容哈希，否则得到的是引用哈希
		return Objects.hash(Arrays.hashCode(this.embedding), this.index);
	}

	// toString 刻意不打印向量内容：上千维浮点数会污染日志，只标记"空/有数据"
	@Override
	public String toString() {
		String message = this.embedding.length == 0 ? "<empty>" : "<has data>";
		return "Embedding{" + "embedding=" + message + ", index=" + this.index + '}';
	}

}
