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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelOptions;

/**
 * Options for embedding models.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 */
/**
 * 嵌入模型的通用参数接口。
 *
 * <p>
 * 它抽出了所有厂商都具备的两个"最大公约数"参数：模型名与向量维度。各厂商的专有参数
 * （如 OpenAI 的 {@code encodingFormat}、{@code user}）由各自的 XxxEmbeddingOptions
 * 实现类扩展本接口来补充。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@code getModel()} / {@code getDimensions()}：均可返回 {@code null}，
 * 语义是"不覆盖，沿用模型默认值"；</li>
 * <li>{@code builder()}：静态工厂方法，返回默认构建器
 * {@link DefaultEmbeddingOptionsBuilder}，屏蔽具体实现类。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * EmbeddingOptions options = EmbeddingOptions.builder().model("bge-large-zh").build();
 * }</pre>
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 */
public interface EmbeddingOptions extends ModelOptions {

	// 模型名；返回 null 表示不覆盖，使用客户端默认模型
	@Nullable String getModel();

	// 期望的输出向量维度；返回 null 表示使用模型默认维度
	@Nullable Integer getDimensions();

	// 静态工厂方法：对外只暴露接口，隐藏 DefaultEmbeddingOptionsBuilder 这一具体实现
	static Builder builder() {
		return new DefaultEmbeddingOptionsBuilder();
	}

	/**
	 * 参数构建器接口：定义流式设置方法，各实现自行决定如何组装最终的 options 对象。
	 */
	interface Builder {

		// 设置模型名，返回自身以支持链式调用
		Builder model(String model);

		// 设置期望向量维度，返回自身以支持链式调用
		Builder dimensions(Integer dimensions);

		// 完成构建，产出不可变的 EmbeddingOptions 实例
		EmbeddingOptions build();

	}

}
