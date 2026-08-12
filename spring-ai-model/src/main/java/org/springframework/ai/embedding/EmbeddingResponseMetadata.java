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

import java.util.Map;

import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.model.AbstractResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;

/**
 * Common AI provider metadata returned in an embedding response.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author Mengqi Xu
 */
/**
 * 嵌入<b>响应级</b>元数据：描述"这次调用整体的情况"，而非单条向量的情况。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code model}：实际处理本次请求的模型名（可能与请求中指定的不同，例如服务端做了版本映射），
 * 默认空串而非 null；</li>
 * <li>{@code usage}：token 用量统计，默认是 {@link EmptyUsage} 空实现（空对象模式），
 * 因此调用方无需判空即可安全调用；</li>
 * <li>继承自 {@code AbstractResponseMetadata} 的 {@code map}：存放厂商专有的额外键值对。</li>
 * </ul>
 *
 * <p>
 * 注意：本类<b>可变</b>（提供了 setModel / setUsage），这是为了方便各厂商实现在解析
 * HTTP 响应的过程中分步填充，与包内其它不可变的值对象设计取向不同。
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * EmbeddingResponseMetadata md = response.getMetadata();
 * long totalTokens = md.getUsage().getTotalTokens();
 * }</pre>
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author Mengqi Xu
 */
public class EmbeddingResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {

	// 实际处理请求的模型名；初始化为空串而非 null，避免调用方判空
	private String model = "";

	// token 用量；默认使用 EmptyUsage 空对象，保证 getUsage() 永不返回 null
	private Usage usage = new EmptyUsage();

	// 无参构造器：字段保持默认值（空模型名 + 空用量），供后续 setter 分步填充
	public EmbeddingResponseMetadata() {
	}

	// 便捷构造器：无额外自定义元数据时使用，委托给全参构造器并传入空 Map
	public EmbeddingResponseMetadata(String model, Usage usage) {
		this(model, usage, Map.of());
	}

	// 全参构造器：额外的厂商专有元数据会被拷贝进父类的 map 中
	public EmbeddingResponseMetadata(String model, Usage usage, Map<String, Object> metadata) {
		this.model = model;
		this.usage = usage;
		// 用 putAll 拷贝而非直接持有引用，避免外部后续修改传入的 Map 影响到本对象
		this.map.putAll(metadata);
	}

	/**
	 * The model that handled the request.
	 */
	// 返回实际处理本次请求的模型名
	public String getModel() {
		return this.model;
	}

	// 设置模型名：供厂商实现在解析响应时回填
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * The AI provider specific metadata on API usage.
	 * @see Usage
	 */
	// 返回 token 用量统计，保证非 null（最差情况是 EmptyUsage）
	public Usage getUsage() {
		return this.usage;
	}

	// 设置 token 用量：供厂商实现在解析响应时回填
	public void setUsage(Usage usage) {
		this.usage = usage;
	}

}
