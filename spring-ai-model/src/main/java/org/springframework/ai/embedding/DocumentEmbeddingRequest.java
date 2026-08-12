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
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelRequest;

/**
 * Represents a request to embed a list of documents.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 文档嵌入请求对象：封装"要向量化哪些 {@link Document}"以及"用什么参数向量化"。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code inputs}：待嵌入的文档列表，<b>顺序即结果顺序</b>，响应中的向量按同样下标一一对应；</li>
 * <li>{@code options}：本次调用的模型参数（模型名、维度等）。</li>
 * </ul>
 *
 * <p>
 * 构造器说明：提供三个重载，前两个是便捷入口，最终都委托给全参构造器，
 * 并在未显式传 options 时填入一个空的默认 {@link EmbeddingOptions}，
 * 这样下游代码无需判空即可直接使用 options（避免 NPE）。
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * var request = new DocumentEmbeddingRequest(List.of(doc1, doc2),
 *         EmbeddingOptions.builder().model("text-embedding-3-small").build());
 * EmbeddingResponse response = documentEmbeddingModel.call(request);
 * }</pre>
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class DocumentEmbeddingRequest implements ModelRequest<List<Document>> {

	// 待嵌入的文档列表；与响应中的向量按下标一一对应，顺序不可打乱
	private final List<Document> inputs;

	// 本次调用使用的嵌入参数（模型名、维度等）
	private final EmbeddingOptions options;

	// 便捷构造器：可变参数写法，内部转为 List 并使用默认空 options
	public DocumentEmbeddingRequest(Document... inputs) {
		this(Arrays.asList(inputs), EmbeddingOptions.builder().build());
	}

	// 便捷构造器：只传文档列表，options 使用默认空实现（表示全部沿用模型默认值）
	public DocumentEmbeddingRequest(List<Document> inputs) {
		this(inputs, EmbeddingOptions.builder().build());
	}

	// 全参构造器：上面两个重载最终都汇聚到这里，字段均为 final，构建后不可变
	public DocumentEmbeddingRequest(List<Document> inputs, EmbeddingOptions options) {
		this.inputs = inputs;
		this.options = options;
	}

	// ModelRequest 约定的方法：返回本次请求的"输入指令"，此处即待嵌入的文档列表
	@Override
	public List<Document> getInstructions() {
		return this.inputs;
	}

	// 返回本次请求的模型参数；因构造时已兜底，此处保证非 null
	@Override
	public EmbeddingOptions getOptions() {
		return this.options;
	}

}
