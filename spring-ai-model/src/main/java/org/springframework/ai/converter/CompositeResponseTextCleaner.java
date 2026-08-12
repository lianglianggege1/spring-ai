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

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A composite {@link ResponseTextCleaner} that applies multiple cleaners in sequence.
 * This allows for a flexible pipeline of text cleaning operations.
 *
 * @author liugddx
 * @since 1.1.0
 */
/**
 * 【中文说明】组合式文本清洗器（组合模式 + 责任链）：按顺序串联多个清洗器。
 *
 * <p>
 * 它本身也实现 {@link ResponseTextCleaner}，因此对调用方而言「一个清洗器」和「一串清洗器」
 * 用法完全一致——这正是组合模式的价值。内部按列表顺序把上一个的输出喂给下一个。
 *
 * <p>
 * <b>顺序很重要</b>，推荐的经典管线为：先去思维链标签 → 再剥 Markdown 围栏 → 最后修剪空白。
 *
 * <pre>{@code
 * ResponseTextCleaner cleaner = CompositeResponseTextCleaner.builder()
 * 	.addCleaner(new ThinkingTagCleaner())
 * 	.addCleaner(new MarkdownCodeBlockCleaner())
 * 	.addCleaner(new WhitespaceCleaner())
 * 	.build();
 * String clean = cleaner.clean(rawLlmText);
 * }</pre>
 *
 * <p>
 * 若不传任何清洗器，则退化为「原样返回」的空实现，可安全用作默认值。
 */
public class CompositeResponseTextCleaner implements ResponseTextCleaner {

	// 中文：按执行顺序保存的子清洗器列表，final + 构造时拷贝，保证不可变
	private final List<ResponseTextCleaner> cleaners;

	/**
	 * Creates a composite cleaner with the given cleaners.
	 * @param cleaners the list of cleaners to apply in order
	 */
	// 中文：主构造器。只校验非 null（允许空列表），空列表表示「不做任何清洗」是合法语义
	public CompositeResponseTextCleaner(List<ResponseTextCleaner> cleaners) {
		Assert.notNull(cleaners, "cleaners cannot be null");
		// 中文：防御性拷贝，隔离外部对原列表的后续修改
		this.cleaners = new ArrayList<>(cleaners);
	}

	/**
	 * Creates a composite cleaner with no cleaners. Text will be returned unchanged.
	 */
	// 中文：空管线构造器——相当于「什么都不做」的空对象（Null Object）模式
	public CompositeResponseTextCleaner() {
		this(new ArrayList<>());
	}

	/**
	 * Creates a composite cleaner with the given cleaners.
	 * @param cleaners the cleaners to apply in order
	 */
	// 中文：可变参数便捷构造器，免去手动包 List 的样板代码
	public CompositeResponseTextCleaner(ResponseTextCleaner... cleaners) {
		this(Arrays.asList(cleaners));
	}

	@Override
	public @Nullable String clean(@Nullable String text) {
		String result = text;
		// 中文：链式传递——把上一个清洗器的输出作为下一个的输入，逐级过滤。
		// 注意中间结果可能为 null，故要求每个子清洗器都必须能容忍 null 输入
		for (ResponseTextCleaner cleaner : this.cleaners) {
			result = cleaner.clean(result);
		}
		return result;
	}

	/**
	 * Creates a builder for constructing a composite cleaner.
	 * @return a new builder instance
	 */
	// 中文：建造器入口，链式声明清洗管线时可读性更好
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link CompositeResponseTextCleaner}.
	 */
	/**
	 * 【中文说明】{@link CompositeResponseTextCleaner} 的建造器，按 addCleaner 的调用顺序决定执行顺序。
	 */
	public static final class Builder {

		// 中文：初始为空列表，完全由调用方决定管线内容与顺序
		private final List<ResponseTextCleaner> cleaners = new ArrayList<>();

		// 中文：私有构造，强制走 builder() 入口
		private Builder() {
		}

		/**
		 * Add a cleaner to the pipeline.
		 * @param cleaner the cleaner to add
		 * @return this builder
		 */
		// 中文：向管线尾部追加一个清洗器；返回 this 以支持链式调用
		public Builder addCleaner(ResponseTextCleaner cleaner) {
			Assert.notNull(cleaner, "cleaner cannot be null");
			this.cleaners.add(cleaner);
			return this;
		}

		/**
		 * Build the composite cleaner.
		 * @return a new composite cleaner instance
		 */
		// 中文：构建组合清洗器实例
		public CompositeResponseTextCleaner build() {
			return new CompositeResponseTextCleaner(this.cleaners);
		}

	}

}
