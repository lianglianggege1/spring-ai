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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelRequest;

/**
 * Request to embed a list of input instructions.
 *
 * @author Christian Tzolov
 */
/**
 * 文本嵌入请求对象：封装"要向量化哪些文本"与"用什么参数"。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code inputs}：待向量化的文本列表，<b>顺序即结果顺序</b>；</li>
 * <li>{@code options}：本次调用的参数，<b>允许为 null</b>（与
 * {@link DocumentEmbeddingRequest} 不同，此处不做默认值兜底），
 * 实现类需自行处理 null，通常表示"完全沿用模型默认配置"。</li>
 * </ul>
 *
 * <p>
 * 该对象为不可变值对象，两个字段均为 {@code final}，可在多线程间安全传递。
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * var request = new EmbeddingRequest(List.of("文本A", "文本B"),
 *         EmbeddingOptions.builder().model("bge-large-zh").build());
 * EmbeddingResponse response = embeddingModel.call(request);
 * }</pre>
 *
 * @author Christian Tzolov
 */
public class EmbeddingRequest implements ModelRequest<List<String>> {

	// 待向量化的文本列表，与响应结果按下标一一对应
	private final List<String> inputs;

	// 本次调用的嵌入参数；注意此处允许为 null，实现类需做空值处理
	private final @Nullable EmbeddingOptions options;

	// 唯一构造器：不做非空校验，options 为 null 时语义为"使用模型默认配置"
	public EmbeddingRequest(List<String> inputs, @Nullable EmbeddingOptions options) {
		this.inputs = inputs;
		this.options = options;
	}

	// ModelRequest 约定方法：返回本次请求的输入内容，此处为文本列表
	@Override
	public List<String> getInstructions() {
		return this.inputs;
	}

	// 返回本次请求的参数，可能为 null，调用方需判空
	@Override
	public @Nullable EmbeddingOptions getOptions() {
		return this.options;
	}

}
