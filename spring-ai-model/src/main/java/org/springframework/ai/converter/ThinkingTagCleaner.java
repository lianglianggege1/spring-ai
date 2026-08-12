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

package org.springframework.ai.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link ResponseTextCleaner} that removes thinking tags from LLM responses. This
 * cleaner supports multiple tag patterns to handle different AI models:
 * <ul>
 * <li>Amazon Nova: {@code <thinking>...</thinking>}</li>
 * <li>Qwen models: {@code <think>...</think>}</li>
 * <li>DeepSeek models: various thinking patterns</li>
 * <li>Claude models: thinking blocks in different formats</li>
 * </ul>
 * <p>
 * <b>Performance:</b> This cleaner includes fast-path optimization. For responses without
 * thinking tags (most models), it performs a quick character check and returns
 * immediately, making it safe to use as a default cleaner even for non-thinking models.
 *
 * @author liugddx
 * @since 1.1.0
 */
/**
 * 【中文说明】思维链标签清洗器：移除推理型模型输出中的「思考过程」片段。
 *
 * <p>
 * DeepSeek-R1、Qwen、Claude、Amazon Nova 等具备推理能力的模型，会先输出一段被特殊标签包裹的
 * 内部思考内容，再给出最终答案。这些思考内容对结构化解析是纯噪声，必须先剔除。
 *
 * <p>
 * 内置覆盖的标签形态见 {@link #DEFAULT_PATTERNS}，包含 XML 式标签、Markdown 思考块与 HTML 注释式。
 *
 * <p>
 * <b>性能设计：</b>{@link #clean(String)} 中有一条「快速通道」——若文本既不含 {@code <} 也不含
 * {@code `}，说明不可能存在任何思考标签，直接原样返回，跳过全部正则匹配。因此即使给不带思维链的
 * 普通模型默认挂上这个清洗器，开销也几乎为零。
 *
 * <p>
 * 三种构造方式：无参（用默认规则）、传 {@code List<Pattern>}、传若干正则字符串；
 * 也可用 {@link #builder()} 在默认规则基础上追加自定义规则。
 */
public class ThinkingTagCleaner implements ResponseTextCleaner {

	/**
	 * Default thinking tag patterns used by common AI models.
	 */
	// 中文：内置默认规则集。(?s) 开启 DOTALL 让 . 能匹配换行；.*? 为懒惰匹配，
	// 保证多段思考块时逐段最小匹配而非贪婪吞掉整篇正文；末尾 \s* 顺带吃掉标签后的空白。
	// CASE_INSENSITIVE 用于兼容 <Thinking> 等大小写混写的情况。
	private static final List<Pattern> DEFAULT_PATTERNS = Arrays.asList(
			// Amazon Nova: <thinking>...</thinking>
			Pattern.compile("(?s)<thinking>.*?</thinking>\\s*", Pattern.CASE_INSENSITIVE),
			// Qwen models: <think>...</think>
			Pattern.compile("(?s)<think>.*?</think>\\s*", Pattern.CASE_INSENSITIVE),
			// Alternative XML-style tags
			Pattern.compile("(?s)<reasoning>.*?</reasoning>\\s*", Pattern.CASE_INSENSITIVE),
			// Markdown style thinking blocks
			Pattern.compile("(?s)```thinking.*?```\\s*", Pattern.CASE_INSENSITIVE),
			// Some models use comment-style
			Pattern.compile("(?s)<!--\\s*thinking:.*?-->\\s*", Pattern.CASE_INSENSITIVE));

	// 中文：本实例实际生效的规则列表，final 且构造时防御性拷贝，保证线程安全与不可变
	private final List<Pattern> patterns;

	/**
	 * Creates a cleaner with default thinking tag patterns.
	 */
	// 中文：无参构造——委托给下面的构造器，使用内置默认规则集
	public ThinkingTagCleaner() {
		this(DEFAULT_PATTERNS);
	}

	/**
	 * Creates a cleaner with custom patterns.
	 * @param patterns the list of regex patterns to match thinking tags
	 */
	// 中文：主构造器。校验非 null 且非空——空规则列表意味着清洗器毫无作用，属于配置错误，故直接拒绝
	public ThinkingTagCleaner(List<Pattern> patterns) {
		Assert.notNull(patterns, "patterns cannot be null");
		Assert.notEmpty(patterns, "patterns cannot be empty");
		// 中文：拷贝一份而非直接持有入参引用，防止外部后续修改该列表影响本对象
		this.patterns = new ArrayList<>(patterns);
	}

	/**
	 * Creates a cleaner with custom pattern strings.
	 * @param patternStrings the list of regex pattern strings to match thinking tags
	 */
	// 中文：便捷构造器——直接传正则字符串，内部统一以忽略大小写模式编译为 Pattern
	public ThinkingTagCleaner(String... patternStrings) {
		Assert.notNull(patternStrings, "patternStrings cannot be null");
		Assert.notEmpty(patternStrings, "patternStrings cannot be empty");
		this.patterns = new ArrayList<>();
		for (String patternString : patternStrings) {
			this.patterns.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE));
		}
	}

	@Override
	public @Nullable String clean(@Nullable String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		// Fast path: if text doesn't contain '<' character, no tags to remove
		// 中文：快速通道——所有内置规则都以 '<' 或 '`' 开头，两者皆无则必然无需清洗，
		// 直接返回可省掉整轮正则匹配，这是本类可被无脑设为默认清洗器的关键
		if (!text.contains("<") && !text.contains("`")) {
			return text;
		}

		String result = text;
		// 中文：遍历所有规则依次替换，而非命中一条就退出
		for (Pattern pattern : this.patterns) {
			String afterReplacement = pattern.matcher(result).replaceAll("");
			// If replacement occurred, update result and continue checking other patterns
			// (since multiple tag types might coexist)
			// 中文：同一份输出中可能同时存在多种标签（如 <think> 与 ```thinking），
			// 因此发生替换后仍要继续跑完剩余规则，不能提前 break
			if (!afterReplacement.equals(result)) {
				result = afterReplacement;
			}
		}
		return result;
	}

	/**
	 * Creates a builder for constructing a thinking tag cleaner.
	 * @return a new builder instance
	 */
	// 中文：获取建造器入口。适合「保留默认规则再补充自定义规则」的场景
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link ThinkingTagCleaner}.
	 */
	/**
	 * 【中文说明】{@link ThinkingTagCleaner} 的建造器。
	 *
	 * <p>
	 * 默认已预置全部内置规则；若只想使用自己的规则，先调用 {@link #withoutDefaultPatterns()}
	 * 再调用 {@code addPattern(...)}，首次添加时会清空默认规则。
	 */
	public static final class Builder {

		// 中文：初始即填入默认规则集，所以直接 build() 等价于无参构造
		private final List<Pattern> patterns = new ArrayList<>(DEFAULT_PATTERNS);

		// 中文：标记位，配合 withoutDefaultPatterns() 实现「延迟清空默认规则」
		private boolean useDefaultPatterns = true;

		// 中文：私有构造，强制通过 builder() 创建
		private Builder() {
		}

		/**
		 * Disable default patterns. Only custom patterns added via
		 * {@link #addPattern(String)} or {@link #addPattern(Pattern)} will be used.
		 * @return this builder
		 */
		// 中文：此处只是「打标记」，并未立刻清空 patterns；真正的清空发生在首次 addPattern 时。
		// 这样设计是为了让「弃用默认规则」与「至少提供一条自定义规则」绑定，避免规则列表为空导致构造失败
		public Builder withoutDefaultPatterns() {
			this.useDefaultPatterns = false;
			return this;
		}

		/**
		 * Add a custom pattern string.
		 * @param patternString the regex pattern string
		 * @return this builder
		 */
		// 中文：追加一条正则字符串规则（自动按忽略大小写编译）
		public Builder addPattern(String patternString) {
			Assert.hasText(patternString, "patternString cannot be empty");
			// 中文：若之前调用过 withoutDefaultPatterns()，在首次追加时清空默认规则；
			// 随后把标记复位为 true，确保后续 addPattern 不会再次清空已添加的自定义规则
			if (!this.useDefaultPatterns) {
				this.patterns.clear();
				this.useDefaultPatterns = true; // Reset flag after first custom pattern
			}
			this.patterns.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE));
			return this;
		}

		/**
		 * Add a custom pattern.
		 * @param pattern the regex pattern
		 * @return this builder
		 */
		// 中文：追加一条已编译好的 Pattern（重载版本，可自行指定 flags）
		public Builder addPattern(Pattern pattern) {
			Assert.notNull(pattern, "pattern cannot be null");
			// 中文：与字符串重载相同的「首次追加时清空默认规则」逻辑
			if (!this.useDefaultPatterns) {
				this.patterns.clear();
				this.useDefaultPatterns = true; // Reset flag after first custom pattern
			}
			this.patterns.add(pattern);
			return this;
		}

		/**
		 * Build the thinking tag cleaner.
		 * @return a new thinking tag cleaner instance
		 */
		// 中文：构建实例。构造器内部会再拷贝一份规则列表，故 build 后继续用同一 Builder 不影响已建对象
		public ThinkingTagCleaner build() {
			return new ThinkingTagCleaner(this.patterns);
		}

	}

}
