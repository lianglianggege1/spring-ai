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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Abstract implementation of the {@link EmbeddingModel} interface that provides
 * dimensions calculation caching.
 *
 * @author Christian Tzolov
 * @author Josh Long
 */
/**
 * {@link EmbeddingModel} 接口的抽象基类，主要职责是<b>缓存向量维度（dimensions）</b>。
 *
 * <p>
 * 各家厂商的嵌入模型（OpenAI、Ollama、Azure 等）输出的向量维度不同，且维度对下游的向量数据库
 * 建表非常关键。本类提供两种获取维度的途径：
 * <ol>
 * <li>查表：从 classpath 下的 {@code /embedding/embedding-model-dimensions.properties}
 * 读取已知模型名 -&gt; 维度的映射，零成本；</li>
 * <li>兜底探测：表中查不到时，实际调用一次 {@code embed("Hello World")}，用返回数组长度作为维度。</li>
 * </ol>
 *
 * <p>
 * 关键字段：{@code embeddingDimensions} 为缓存值，初值 -1 表示"尚未计算"，避免每次调用都发起远程请求。
 *
 * <p>
 * 典型用法：厂商 starter 中的 XxxEmbeddingModel 继承本类，只需实现 {@code call(EmbeddingRequest)}，
 * 即可免费获得 {@code dimensions()} 的缓存能力。
 *
 * <p>
 * 注解说明：{@code @ImportRuntimeHints} 用于 GraalVM 原生镜像，把上面的 properties 文件注册为运行时资源，
 * 否则 AOT 编译后该文件会丢失。
 *
 * @author Christian Tzolov
 * @author Josh Long
 */
@ImportRuntimeHints(AbstractEmbeddingModel.Hints.class)
public abstract class AbstractEmbeddingModel implements EmbeddingModel {

	// 内置的"模型名 -> 维度"配置文件位置（打包在 jar 的 classpath 中）
	private static final Resource EMBEDDING_MODEL_DIMENSIONS_PROPERTIES = new ClassPathResource(
			"/embedding/embedding-model-dimensions.properties");

	// 类加载时一次性读入的已知模型维度表，后续查询走内存，不再读文件
	private static final Map<String, Integer> KNOWN_EMBEDDING_DIMENSIONS = loadKnownModelDimensions();

	/**
	 * Cached embedding dimensions.
	 */
	// 缓存的向量维度；用 AtomicInteger 保证多线程下的可见性，初值 -1 代表"还没算过"
	protected final AtomicInteger embeddingDimensions = new AtomicInteger(-1);

	/**
	 * Return the dimension of the requested embedding generative name. If the generative
	 * name is unknown uses the EmbeddingModel to perform a dummy EmbeddingModel#embed and
	 * count the response dimensions.
	 * @param embeddingModel Fall-back client to determine, empirically the dimensions.
	 * @param modelName Embedding generative name to retrieve the dimensions for.
	 * @param dummyContent Dummy content to use for the empirical dimension calculation.
	 * @return Returns the embedding dimensions for the modelName.
	 */
	/**
	 * 查询指定嵌入模型的向量维度。
	 *
	 * <p>
	 * 策略：先查内置的已知维度表；查不到则退化为"经验探测"——真实调用一次 embed 并数返回数组长度。
	 * 注意探测分支会产生一次真实的模型调用（可能有网络开销与费用），因此调用方应缓存结果。
	 * @param embeddingModel 探测兜底用的模型客户端
	 * @param modelName 待查询维度的嵌入模型名
	 * @param dummyContent 探测时使用的占位文本
	 * @return 该模型的向量维度
	 */
	public static int dimensions(EmbeddingModel embeddingModel, String modelName, String dummyContent) {

		// 分支一：命中内置配置表，直接返回，无任何远程调用
		if (KNOWN_EMBEDDING_DIMENSIONS.containsKey(modelName)) {
			// Retrieve the dimension from a pre-configured file.
			return KNOWN_EMBEDDING_DIMENSIONS.get(modelName);
		}
		else {
			// Determine the dimensions empirically.
			// Generate an embedding and count the dimension size;
			// 分支二：未知模型，发起一次真实 embed 调用，用返回向量的长度反推维度
			return embeddingModel.embed(dummyContent).length;
		}
	}

	// 读取并解析内置的维度 properties 文件，转成 Map<模型名, 维度>
	private static Map<String, Integer> loadKnownModelDimensions() {
		try {
			var resource = EMBEDDING_MODEL_DIMENSIONS_PROPERTIES;
			// 参数/状态校验：资源必须存在，否则说明打包缺失，属于不可恢复的配置错误
			Assert.notNull(resource, "the embedding dimensions must be non-null");
			Assert.state(resource.exists(), "the embedding dimensions properties file must exist");
			var properties = new Properties();
			// try-with-resources 确保输入流关闭
			try (var in = resource.getInputStream()) {
				properties.load(in);
			}
			// properties 的 value 是字符串，这里统一转成 int 维度值
			return properties.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), e -> Integer.parseInt(e.getValue().toString())));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// 返回当前模型的向量维度：懒加载 + 缓存，首次调用可能触发一次真实 embed 探测
	@Override
	public int dimensions() {
		// -1 表示尚未计算过，此时才去计算并写入缓存（后续调用直接命中缓存）
		if (this.embeddingDimensions.get() < 0) {
			this.embeddingDimensions.set(dimensions(this, "Test", "Hello World"));
		}
		return this.embeddingDimensions.get();
	}

	// GraalVM 原生镜像支持：把维度 properties 文件注册进运行时资源，防止 AOT 裁剪后读不到
	static class Hints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerResource(EMBEDDING_MODEL_DIMENSIONS_PROPERTIES);
		}

	}

}
