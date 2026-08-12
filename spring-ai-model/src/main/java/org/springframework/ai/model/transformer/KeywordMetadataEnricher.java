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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.util.Assert;

/**
 * Keyword extractor that uses generative to extract 'excerpt_keywords' metadata field.
 *
 * @author Christian Tzolov
 * @author YunKui Lu
 */
/**
 * 【中文说明】KeywordMetadataEnricher 是一个<b>文档元数据增强器</b>：
 * 调用大模型为每篇文档抽取关键词，并写入元数据字段 {@code excerpt_keywords}。
 *
 * <p>
 * 用途：属于 RAG（检索增强生成）数据摄取管道的一环。文档被切分后，
 * 用 LLM 提炼关键词存入元数据，后续检索时可据此做关键词过滤或混合检索，
 * 提升召回质量。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@link #CONTEXT_STR_PLACEHOLDER} —— 提示词模板中代表文档正文的占位符名；</li>
 * <li>{@link #KEYWORDS_TEMPLATE} —— 内置的英文提示词模板，要求模型输出逗号分隔的关键词；</li>
 * <li>{@link #EXCERPT_KEYWORDS_METADATA_KEY} —— 结果写入文档元数据时使用的键名；</li>
 * <li>{@code chatModel} —— 执行抽取的聊天模型；</li>
 * <li>{@code keywordsTemplate} —— 最终生效的提示词模板。</li>
 * </ul>
 *
 * <p>
 * 两种构造方式（二选一）：传入"关键词数量"使用内置模板，或直接传入自定义模板。
 * 通过 Builder 构造时二者<b>互斥</b>，详见 {@link Builder#build()}。
 *
 * <p>
 * 典型用法：
 * {@code new KeywordMetadataEnricher(chatModel, 5).apply(documents)}
 *
 * @author Christian Tzolov
 * @author YunKui Lu
 */
public class KeywordMetadataEnricher implements DocumentTransformer {

	// 【中文】日志器，主要用于 Builder 中提示互斥参数被忽略的警告。
	private static final Log logger = LogFactory.getLog(KeywordMetadataEnricher.class);

	// 【中文】提示词模板中的占位符名称。自定义模板时也必须使用这个名字，
	// 否则文档正文无法被正确注入。
	public static final String CONTEXT_STR_PLACEHOLDER = "context_str";

	// 【中文】内置关键词抽取提示词模板。
	// 注意这里混用了两套占位符语法：{context_str} 由 PromptTemplate 在运行时渲染，
	// 而 %s 是 Java String.format 的占位符，在构造器中就被替换成具体的关键词数量。
	public static final String KEYWORDS_TEMPLATE = """
			{context_str}. Give %s unique keywords for this
			document. Format as comma separated. Keywords: """;

	// 【中文】抽取结果写入文档元数据时的键名。
	public static final String EXCERPT_KEYWORDS_METADATA_KEY = "excerpt_keywords";

	/**
	 * Model predictor
	 */
	// 【中文】用于执行关键词抽取的聊天模型（即注释中的"模型预测器"）。
	private final ChatModel chatModel;

	/**
	 * The prompt template to use for keyword extraction.
	 */
	// 【中文】实际使用的提示词模板：可能是由 keywordCount 格式化后的内置模板，也可能是用户自定义模板。
	private final PromptTemplate keywordsTemplate;

	/**
	 * Create a new {@link KeywordMetadataEnricher} instance.
	 * @param chatModel the model predictor to use for keyword extraction.
	 * @param keywordCount the number of keywords to extract.
	 */
	// 【中文】构造方式一：指定要抽取的关键词数量，内部使用内置模板。
	public KeywordMetadataEnricher(ChatModel chatModel, int keywordCount) {
		// 参数校验：模型必填；关键词数量至少为 1（0 或负数无意义）
		Assert.notNull(chatModel, "chatModel must not be null");
		Assert.isTrue(keywordCount >= 1, "keywordCount must be >= 1");

		this.chatModel = chatModel;
		// 用 String.format 把模板里的 %s 替换成关键词数量，再包装为 PromptTemplate。
		// 此时模板中仅剩 {context_str} 占位符，留待运行时渲染
		this.keywordsTemplate = new PromptTemplate(String.format(KEYWORDS_TEMPLATE, keywordCount));
	}

	/**
	 * Create a new {@link KeywordMetadataEnricher} instance.
	 * @param chatModel the model predictor to use for keyword extraction.
	 * @param keywordsTemplate the prompt template to use for keyword extraction.
	 */
	// 【中文】构造方式二：直接传入自定义提示词模板，完全接管抽取逻辑
	// （例如改成中文提示词、或调整关键词格式要求）。
	// 注意：自定义模板中必须包含 {context_str} 占位符。
	public KeywordMetadataEnricher(ChatModel chatModel, PromptTemplate keywordsTemplate) {
		// 参数校验：两者均不可为空
		Assert.notNull(chatModel, "chatModel must not be null");
		Assert.notNull(keywordsTemplate, "keywordsTemplate must not be null");

		this.chatModel = chatModel;
		this.keywordsTemplate = keywordsTemplate;
	}

	// 【中文】DocumentTransformer 的核心方法：逐篇处理文档并写入关键词元数据。
	// 重要：本方法<b>就地修改</b>传入的 Document 元数据，并原样返回同一个列表，
	// 而不是创建新对象——调用方需注意入参会被改变。
	@Override
	public List<Document> apply(List<Document> documents) {
		// 逐篇串行处理：每篇文档都会触发一次独立的模型调用，文档量大时耗时和费用需留意
		for (Document document : documents) {
			String text = document.getText();
			Map<String, Object> vars = new HashMap<>();
			// 空值处理：文档正文可能为 null（如纯媒体文档），
			// 此时不放入占位符变量。注意这会导致模板渲染时缺少变量，
			// 因此本增强器仅适用于有文本内容的文档
			if (text != null) {
				vars.put(CONTEXT_STR_PLACEHOLDER, text);
			}
			// 渲染模板得到最终提示词
			Prompt prompt = this.keywordsTemplate.create(vars);
			// 调用模型并取第一条结果
			Generation generation = this.chatModel.call(prompt).getResult();
			// 空值处理（第一重）：模型可能没有返回任何结果
			if (generation != null) {
				String keywords = generation.getOutput().getText();
				// 空值处理（第二重）：结果文本也可能为空。
				// 两重判空后才写入元数据，确保不会写进 null 值
				if (keywords != null) {
					document.getMetadata().put(EXCERPT_KEYWORDS_METADATA_KEY, keywords);
				}
			}
		}
		return documents;
	}

	// Exposed for testing purposes
	// 【中文】包级私有的访问器（无 public 修饰），如原注释所言仅供单元测试验证模板是否正确构造，
	// 不属于对外 API。
	PromptTemplate getKeywordsTemplate() {
		return this.keywordsTemplate;
	}

	// 【中文】静态工厂方法，返回 Builder。chatModel 作为必填项在此处传入，
	// 从而保证 Builder 一创建就持有必需依赖。
	public static Builder builder(ChatModel chatModel) {
		return new Builder(chatModel);
	}

	/**
	 * 【中文说明】KeywordMetadataEnricher 的建造者（Builder 模式）。
	 *
	 * <p>
	 * 互斥约束（重点）：{@code keywordCount} 与 {@code keywordsTemplate} 二者互斥——
	 * 自定义模板一旦设置，关键词数量就失去意义（数量已写死在模板里）。
	 * 本 Builder 的处理策略是"模板优先 + 打印警告"，而非抛异常，属于宽容式设计。
	 */
	public static final class Builder {

		// 【中文】必填依赖，构造 Builder 时即注入，故为 final。
		private final ChatModel chatModel;

		// 【中文】关键词数量。注意它是基本类型 int，默认值为 0，
		// 下面 build() 正是利用"是否仍为 0"来判断用户有没有显式设置过。
		private int keywordCount;

		// 【中文】自定义模板，可选，故用 @Nullable 标注；null 表示未设置。
		private @Nullable PromptTemplate keywordsTemplate;

		public Builder(ChatModel chatModel) {
			// 参数校验：模型是必需依赖
			Assert.notNull(chatModel, "The chatModel must not be null");
			this.chatModel = chatModel;
		}

		// 【中文】设置关键词数量（与 keywordsTemplate 互斥）。返回 this 支持链式调用。
		public Builder keywordCount(int keywordCount) {
			Assert.isTrue(keywordCount >= 1, "The keywordCount must be >= 1");
			this.keywordCount = keywordCount;
			return this;
		}

		// 【中文】设置自定义提示词模板（与 keywordCount 互斥）。返回 this 支持链式调用。
		public Builder keywordsTemplate(PromptTemplate keywordsTemplate) {
			Assert.notNull(keywordsTemplate, "The keywordsTemplate must not be null");
			this.keywordsTemplate = keywordsTemplate;
			return this;
		}

		// 【中文】构建实例，并在此集中处理两个互斥参数的优先级。
		public KeywordMetadataEnricher build() {
			// 分支一：设置了自定义模板 —— 模板优先
			if (this.keywordsTemplate != null) {

				// 互斥检测：keywordCount 不为 0 说明用户也显式设置过它，
				// 此时只警告不报错，明确告知该参数将被忽略
				if (this.keywordCount != 0) {
					logger.warn("keywordCount will be ignored as keywordsTemplate is set.");
				}

				return new KeywordMetadataEnricher(this.chatModel, this.keywordsTemplate);
			}

			// 分支二：未设置模板 —— 走 keywordCount 路径。
			// 注意：若用户两个都没设置，keywordCount 为默认值 0，
			// 会在构造器的 Assert.isTrue(keywordCount >= 1) 处抛出异常，从而强制用户必须二选一
			return new KeywordMetadataEnricher(this.chatModel, this.keywordCount);
		}

	}

}
