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

package org.springframework.ai.model.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.MetadataMode;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Title extractor with adjacent sharing that uses generative to extract
 * 'section_summary', 'prev_section_summary', 'next_section_summary' metadata fields.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】SummaryMetadataEnricher 是一个<b>摘要元数据增强器</b>：
 * 用大模型为每篇文档生成摘要，并支持把<b>相邻文档</b>的摘要也写入当前文档的元数据。
 *
 * <p>
 * 核心亮点——"相邻共享"（adjacent sharing）：文档被切分成多个片段后，单个片段往往缺少上下文。
 * 本类可以把上一段、当前段、下一段的摘要分别写入
 * {@code prev_section_summary}、{@code section_summary}、{@code next_section_summary}
 * 三个元数据字段，让检索到某个片段时也能了解其前后语境，显著改善 RAG 效果。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@link #DEFAULT_SUMMARY_EXTRACT_TEMPLATE} —— 默认摘要提示词模板；</li>
 * <li>{@code summaryTypes} —— 决定写入哪几类摘要（见内部枚举 {@link SummaryType}）；</li>
 * <li>{@code metadataMode} —— 控制在生成摘要时，文档的哪些元数据要一并喂给模型；</li>
 * <li>{@code summaryTemplate} —— 实际使用的提示词模板字符串。</li>
 * </ul>
 *
 * <p>
 * 处理流程分两趟：第一趟为所有文档逐一生成摘要；第二趟再按 summaryTypes 把
 * 前/当前/后摘要回填到各文档。之所以要分两趟，是因为"下一段的摘要"必须等后续文档处理完才拿得到。
 *
 * <p>
 * 典型用法：
 * {@code new SummaryMetadataEnricher(chatModel, List.of(SummaryType.PREVIOUS, SummaryType.CURRENT))}
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public class SummaryMetadataEnricher implements DocumentTransformer {

	// 【中文】默认摘要提示词模板：要求模型总结该段落的关键主题与实体。
	// 其中 {context_str} 会在运行时被替换为文档内容。
	public static final String DEFAULT_SUMMARY_EXTRACT_TEMPLATE = """
			Here is the content of the section:
			{context_str}

			Summarize the key topics and entities of the section.

			Summary:""";

	// 【中文】当前段摘要的元数据键名。
	private static final String SECTION_SUMMARY_METADATA_KEY = "section_summary";

	// 【中文】下一段摘要的元数据键名。
	private static final String NEXT_SECTION_SUMMARY_METADATA_KEY = "next_section_summary";

	// 【中文】上一段摘要的元数据键名。
	private static final String PREV_SECTION_SUMMARY_METADATA_KEY = "prev_section_summary";

	// 【中文】模板中文档内容的占位符名（此处为 private，与 KeywordMetadataEnricher 中的 public 常量不同，
	// 因为本类不支持传入 PromptTemplate 对象、只支持模板字符串，无需对外暴露）。
	private static final String CONTEXT_STR_PLACEHOLDER = "context_str";

	/**
	 * AI client.
	 */
	// 【中文】用于生成摘要的聊天模型。
	private final ChatModel chatModel;

	/**
	 * Number of documents from front to use for title extraction.
	 */
	// 【中文】要生成并写入的摘要类型集合（PREVIOUS / CURRENT / NEXT 可任意组合）。
	// 注意：上方英文注释与实际含义不符（并非"数量"），以本中文说明为准。
	private final List<SummaryType> summaryTypes;

	// 【中文】元数据模式：决定调用模型时，文档自身的元数据以何种方式拼接进内容。
	// 常见取值 ALL（全部带上）、NONE（只用正文）、INFERENCE、EMBED。
	private final MetadataMode metadataMode;

	/**
	 * Template for summary extraction.
	 */
	// 【中文】摘要提示词模板字符串（注意这里是 String，每次调用时才现场构造 PromptTemplate）。
	private final String summaryTemplate;

	// 【中文】简化构造器：使用默认模板与 MetadataMode.ALL。
	public SummaryMetadataEnricher(ChatModel chatModel, List<SummaryType> summaryTypes) {
		this(chatModel, summaryTypes, DEFAULT_SUMMARY_EXTRACT_TEMPLATE, MetadataMode.ALL);
	}

	// 【中文】完整构造器：可自定义模板与元数据模式。
	public SummaryMetadataEnricher(ChatModel chatModel, List<SummaryType> summaryTypes, String summaryTemplate,
			MetadataMode metadataMode) {
		// 参数校验：模型必填；模板必须是非空白字符串（hasText 比 notNull 更严格，会同时排除空串和纯空格）
		Assert.notNull(chatModel, "ChatModel must not be null");
		Assert.hasText(summaryTemplate, "Summary template must not be empty");

		this.chatModel = chatModel;
		// 空值处理 + 默认值：summaryTypes 为 null 或空集合时，回退为"只生成当前段摘要"，
		// 保证至少产出一项有用结果，而不是什么都不做
		this.summaryTypes = CollectionUtils.isEmpty(summaryTypes) ? List.of(SummaryType.CURRENT) : summaryTypes;
		this.metadataMode = metadataMode;
		this.summaryTemplate = summaryTemplate;
	}

	// 【中文】核心方法，采用<b>两趟处理</b>：
	// 第一趟为每篇文档生成摘要并按顺序缓存；第二趟再按 summaryTypes 回填元数据。
	// 必须分两趟的原因：写入"下一段摘要"时需要后面文档的摘要已经算好。
	@Override
	public List<Document> apply(List<Document> documents) {

		// —— 第一趟：逐篇生成摘要，结果的下标与 documents 严格一一对应 ——
		List<String> documentSummaries = new ArrayList<>();
		for (Document document : documents) {

			// 按 metadataMode 取得格式化后的文档内容（可能包含部分元数据）
			var documentContext = document.getFormattedContent(this.metadataMode);

			// 每次循环都新建 PromptTemplate（因为 summaryTemplate 存的是字符串）
			Prompt prompt = new PromptTemplate(this.summaryTemplate)
				.create(Map.of(CONTEXT_STR_PLACEHOLDER, documentContext));
			Generation generation = this.chatModel.call(prompt).getResult();
			// 双重空值兜底：结果为 null、或结果文本为 null 时，一律存入空串 ""。
			// 这一点很关键——必须保证 documentSummaries 与 documents 长度一致、下标对齐，
			// 否则第二趟按下标取相邻摘要就会错位
			documentSummaries
				.add(generation != null ? Objects.requireNonNullElse(generation.getOutput().getText(), "") : "");
		}

		// —— 第二趟：按下标把前/当前/后摘要写回各文档的元数据 ——
		for (int i = 0; i < documentSummaries.size(); i++) {
			Map<String, Object> summaryMetadata = getSummaryMetadata(i, documentSummaries);
			// 就地修改文档元数据（注意会改变传入的对象）
			documents.get(i).getMetadata().putAll(summaryMetadata);
		}

		return documents;
	}

	// 【中文】为下标 i 的文档计算应写入的摘要元数据。
	// 三个 if 相互独立，因此可同时写入多类摘要。
	private Map<String, Object> getSummaryMetadata(int i, List<String> documentSummaries) {
		Map<String, Object> summaryMetadata = new HashMap<>();
		// 边界处理：i > 0 才存在上一篇。第一篇文档不会有 prev_section_summary 字段
		if (i > 0 && this.summaryTypes.contains(SummaryType.PREVIOUS)) {
			summaryMetadata.put(PREV_SECTION_SUMMARY_METADATA_KEY, documentSummaries.get(i - 1));
		}
		// 边界处理：i 小于最后一个下标才存在下一篇。最后一篇不会有 next_section_summary 字段
		if (i < (documentSummaries.size() - 1) && this.summaryTypes.contains(SummaryType.NEXT)) {
			summaryMetadata.put(NEXT_SECTION_SUMMARY_METADATA_KEY, documentSummaries.get(i + 1));
		}
		// 当前段摘要无需边界判断，一定存在
		if (this.summaryTypes.contains(SummaryType.CURRENT)) {
			summaryMetadata.put(SECTION_SUMMARY_METADATA_KEY, documentSummaries.get(i));
		}
		return summaryMetadata;
	}

	/**
	 * 【中文说明】摘要类型枚举，用于声明要为文档写入哪几类摘要：
	 * <ul>
	 * <li>{@code PREVIOUS} —— 上一篇文档的摘要（首篇无此项）；</li>
	 * <li>{@code CURRENT} —— 当前文档自身的摘要；</li>
	 * <li>{@code NEXT} —— 下一篇文档的摘要（末篇无此项）。</li>
	 * </ul>
	 * 三者可自由组合，构造时以 List 形式传入。
	 */
	public enum SummaryType {

		PREVIOUS, CURRENT, NEXT

	}

}
