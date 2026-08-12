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

import com.knuddels.jtokkit.api.EncodingType;

import org.springframework.ai.document.ContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.util.Assert;

/**
 * Token count based strategy implementation for {@link BatchingStrategy}. Using openai
 * max input token as the default: <a href=
 * "https://platform.openai.com/docs/guides/embeddings#embedding-models">embedding-models</a>
 *
 * This strategy incorporates a reserve percentage to provide a buffer for potential
 * overhead or unexpected increases in token count during processing. The actual max input
 * token count used is calculated as: actualMaxInputTokenCount =
 * originalMaxInputTokenCount * (1 - RESERVE_PERCENTAGE)
 *
 * For example, with the default reserve percentage of 10% (0.1) and the default max input
 * token count of 8191, the actual max input token count used will be 7371.
 *
 * The strategy batches documents based on their token counts, ensuring that each batch
 * does not exceed the calculated max input token count.
 *
 * @author Soby Chacko
 * @author Mark Pollack
 * @author Laura Trotta
 * @author Jihoon Kim
 * @author Yanming Zhou
 * @since 1.0.0
 */
/**
 * 基于 <b>token 数量</b>的分批策略，是 {@link BatchingStrategy} 的默认实现。
 *
 * <p>
 * 核心思路：遍历文档，累加每篇文档的 token 数，一旦累加值超过上限就"切一刀"开启新批次，
 * 从而保证每个子批次的 token 总量都不超过模型限制。
 *
 * <p>
 * <b>预留缓冲（reserve percentage）机制</b>：token 估算算法与服务端真实分词可能存在偏差，
 * 因此实际使用的上限会打一个折扣：
 * <pre>
 * 实际上限 = 原始上限 × (1 - 预留比例)
 * </pre>
 * 以默认值为例：8191 × (1 - 0.1) ≈ 7372，留出约 10% 的安全余量，避免踩线报错。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code tokenCountEstimator}：token 估算器，默认用 jtokkit 的 CL100K_BASE 编码
 * （OpenAI 系模型所用）；</li>
 * <li>{@code maxInputTokenCount}：<b>已经扣除缓冲后</b>的实际上限，构造时即算好；</li>
 * <li>{@code contentFormatter} / {@code metadataMode}：决定统计 token 时是否把文档元数据
 * 也算进去，默认 {@link MetadataMode#NONE} 即只算正文。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 * <pre>{@code
 * var strategy = new TokenCountBatchingStrategy();
 * List<float[]> vectors = embeddingModel.embed(documents, options, strategy);
 * }</pre>
 *
 * @author Soby Chacko
 * @author Mark Pollack
 * @author Laura Trotta
 * @author Jihoon Kim
 * @author Yanming Zhou
 * @since 1.0.0
 */
public class TokenCountBatchingStrategy implements BatchingStrategy {

	/**
	 * Using openai upper limit of input token count as the default.
	 */
	// 默认 token 上限，取自 OpenAI 嵌入模型的输入限制
	private static final int MAX_INPUT_TOKEN_COUNT = 8191;

	/**
	 * The default percentage of tokens to reserve when calculating the actual max input
	 * token count.
	 */
	// 默认预留 10% 作为安全缓冲，抵消 token 估算与服务端实际分词的偏差
	private static final double DEFAULT_TOKEN_COUNT_RESERVE_PERCENTAGE = 0.1;

	// token 数量估算器
	private final TokenCountEstimator tokenCountEstimator;

	// 实际生效的 token 上限（已扣除预留缓冲），构造时一次性算好
	private final int maxInputTokenCount;

	// 内容格式化器，与 metadataMode 配合决定参与 token 统计的文本形态
	private final ContentFormatter contentFormatter;

	// 元数据模式：决定统计 token 时是否包含文档元数据，默认 NONE（只算正文）
	private final MetadataMode metadataMode;

	// 无参构造器：使用 OpenAI 的 CL100K_BASE 编码、8191 上限、10% 预留
	public TokenCountBatchingStrategy() {
		this(EncodingType.CL100K_BASE, MAX_INPUT_TOKEN_COUNT, DEFAULT_TOKEN_COUNT_RESERVE_PERCENTAGE);
	}

	/**
	 * @param encodingType {@link EncodingType}
	 * @param maxInputTokenCount upper limit for input tokens
	 * @param reservePercentage the percentage of tokens to reserve from the max input
	 * token count to create a buffer.
	 */
	// 便捷构造器：指定编码与上限，格式化器和元数据模式取默认值（不含元数据）
	public TokenCountBatchingStrategy(EncodingType encodingType, int maxInputTokenCount, double reservePercentage) {
		this(encodingType, maxInputTokenCount, reservePercentage, Document.DEFAULT_CONTENT_FORMATTER,
				MetadataMode.NONE);
	}

	/**
	 * @param encodingType The {@link EncodingType} to be used for token counting.
	 * @param maxInputTokenCount The initial upper limit for input tokens.
	 * @param reservePercentage The percentage of tokens to reserve from the max input
	 * token count. This creates a buffer for potential token count increases during
	 * processing.
	 * @param contentFormatter the {@link ContentFormatter} to be used for formatting
	 * content.
	 * @param metadataMode The {@link MetadataMode} to be used for handling metadata.
	 */
	// 全参构造器（编码类型版）：内部据 encodingType 自建 jtokkit 估算器
	public TokenCountBatchingStrategy(EncodingType encodingType, int maxInputTokenCount, double reservePercentage,
			ContentFormatter contentFormatter, MetadataMode metadataMode) {
		// 参数校验：上限必须为正数；预留比例必须落在 [0,1) 区间——
		// 等于 1 会导致实际上限为 0，任何文档都无法入批，故右开区间
		Assert.notNull(encodingType, "EncodingType must not be null");
		Assert.isTrue(maxInputTokenCount > 0, "MaxInputTokenCount must be greater than 0");
		Assert.isTrue(reservePercentage >= 0 && reservePercentage < 1, "ReservePercentage must be in range [0, 1)");
		Assert.notNull(contentFormatter, "ContentFormatter must not be null");
		Assert.notNull(metadataMode, "MetadataMode must not be null");
		this.tokenCountEstimator = new JTokkitTokenCountEstimator(encodingType);
		// 关键：此处一次性把"扣除缓冲后的上限"算好并缓存，后续 batch() 直接用，无需重复计算
		this.maxInputTokenCount = (int) Math.round(maxInputTokenCount * (1 - reservePercentage));
		this.contentFormatter = contentFormatter;
		this.metadataMode = metadataMode;
	}

	/**
	 * Constructs a TokenCountBatchingStrategy with the specified parameters.
	 * @param tokenCountEstimator the TokenCountEstimator to be used for estimating token
	 * counts.
	 * @param maxInputTokenCount the initial upper limit for input tokens.
	 * @param reservePercentage the percentage of tokens to reserve from the max input
	 * token count to create a buffer.
	 * @param contentFormatter the ContentFormatter to be used for formatting content.
	 * @param metadataMode the MetadataMode to be used for handling metadata.
	 */
	// 全参构造器（自定义估算器版）：适用于非 OpenAI 分词体系的模型，可注入自己的 TokenCountEstimator
	public TokenCountBatchingStrategy(TokenCountEstimator tokenCountEstimator, int maxInputTokenCount,
			double reservePercentage, ContentFormatter contentFormatter, MetadataMode metadataMode) {
		// 校验规则与上一个构造器完全一致
		Assert.notNull(tokenCountEstimator, "TokenCountEstimator must not be null");
		Assert.isTrue(maxInputTokenCount > 0, "MaxInputTokenCount must be greater than 0");
		Assert.isTrue(reservePercentage >= 0 && reservePercentage < 1, "ReservePercentage must be in range [0, 1)");
		Assert.notNull(contentFormatter, "ContentFormatter must not be null");
		Assert.notNull(metadataMode, "MetadataMode must not be null");
		this.tokenCountEstimator = tokenCountEstimator;
		// 同样在构造期算好扣除缓冲后的实际上限
		this.maxInputTokenCount = (int) Math.round(maxInputTokenCount * (1 - reservePercentage));
		this.contentFormatter = contentFormatter;
		this.metadataMode = metadataMode;
	}

	/**
	 * 按 token 上限把文档列表切分为若干子批次。
	 *
	 * <p>
	 * 算法为单趟遍历的贪心累加：维护当前批次及其 token 累计值，超限即封箱另起一批。
	 * 全程按输入顺序追加，因此天然满足 {@link BatchingStrategy} 的"保序"约束。
	 * @param documents 待分批的文档
	 * @return 子批次列表
	 * @throws IllegalArgumentException 当单篇文档自身就超过上限时抛出
	 */
	@Override
	public List<List<Document>> batch(List<Document> documents) {
		List<List<Document>> batches = new ArrayList<>();
		// currentSize 为当前批次已累计的 token 数
		int currentSize = 0;
		List<Document> currentBatch = new ArrayList<>();

		// Do not collect the documents into a Map keyed by Document: equal documents
		// would collapse to a single entry and be silently dropped from the batches.
		// 上面英文注释说明了一个易踩的坑：不能用 Map 以 Document 为键收集，
		// 否则内容相同的文档会因 equals/hashCode 相等而被去重，导致文档被静默丢弃、
		// 最终向量数与文档数对不上。
		for (Document document : documents) {
			// 按配置的格式化器与元数据模式取出参与统计的文本，再估算 token 数
			int tokenCount = this.tokenCountEstimator
				.estimate(document.getFormattedContent(this.contentFormatter, this.metadataMode));
			// 边界情况：单篇文档就超限，再怎么分批也塞不下，直接快速失败
			if (tokenCount > this.maxInputTokenCount) {
				throw new IllegalArgumentException(
						"Tokens in a single document exceeds the maximum number of allowed input tokens");
			}
			currentSize += tokenCount;
			// 累加后超限：把当前批次封箱归档，另起新批，
			// 并把 currentSize 重置为当前这篇文档的 token 数（它将成为新批的第一篇）
			if (currentSize > this.maxInputTokenCount) {
				batches.add(currentBatch);
				currentBatch = new ArrayList<>();
				currentSize = tokenCount;
			}
			currentBatch.add(document);
		}
		// 收尾：循环结束后最后一批尚未归档，非空则补进结果
		if (!currentBatch.isEmpty()) {
			batches.add(currentBatch);
		}
		return batches;
	}

}
