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

package org.springframework.ai.chat.metadata;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * Abstract Data Type (ADT) modeling metadata gathered by the AI during request
 * processing.
 *
 * @author John Blum
 * @since 0.7.0
 */
/**
 * 【中文说明】提示词（Prompt）层面的元数据，主要承载各条输入提示被内容审核系统过滤/标记的结果。
 *
 * <p>
 * 设计要点（本文件是理解 Java 惯用法的好例子）：
 * <ul>
 * <li>标注了 {@code @FunctionalInterface}——本接口只有一个抽象方法，即继承自
 * {@link Iterable} 的 {@code iterator()}，因此可以用 Lambda / 方法引用来实现，
 * {@link #of(Iterable)} 里的 {@code iterable::iterator} 正是利用了这一点；</li>
 * <li>继承 {@link Iterable}，所以能直接用增强 for 循环遍历其中的
 * {@link PromptFilterMetadata}；</li>
 * <li>提供 {@code empty()} / {@code of(...)} 系列静态工厂方法，隐藏了实现细节。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code chatResponse.getMetadata().getPromptMetadata().findByPromptIndex(0)}，
 * 用于查看第 0 条提示是否被审核系统标记。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum；@since 0.7.0。
 */
@FunctionalInterface
public interface PromptMetadata extends Iterable<PromptMetadata.PromptFilterMetadata> {

	/**
	 * Factory method used to create empty {@link PromptMetadata} when the information is
	 * not supplied by the AI provider.
	 * @return empty {@link PromptMetadata}.
	 */
	// 【中文】创建"空"的提示词元数据（厂商未提供该信息时使用）。
	// 巧妙之处：直接调用不传任何参数的 of()，可变参数会得到一个空数组，从而生成一个不含元素的实例。
	static PromptMetadata empty() {
		return of();
	}

	/**
	 * Factory method used to create a new {@link PromptMetadata} composed of an array of
	 * {@link PromptFilterMetadata}.
	 * @param array array of {@link PromptFilterMetadata} used to compose the
	 * {@link PromptMetadata}.
	 * @return a new {@link PromptMetadata} composed of an array of
	 * {@link PromptFilterMetadata}.
	 */
	// 【中文】静态工厂（可变参数版）：把数组转成 List 后委托给下面的 Iterable 版本，避免逻辑重复。
	static PromptMetadata of(PromptFilterMetadata... array) {
		return of(Arrays.asList(array));
	}

	/**
	 * Factory method used to create a new {@link PromptMetadata} composed of an
	 * {@link Iterable} of {@link PromptFilterMetadata}.
	 * @param iterable {@link Iterable} of {@link PromptFilterMetadata} used to compose
	 * the {@link PromptMetadata}.
	 * @return a new {@link PromptMetadata} composed of an {@link Iterable} of
	 * {@link PromptFilterMetadata}.
	 */
	// 【中文】核心静态工厂：由 Iterable 构造 PromptMetadata。
	static PromptMetadata of(Iterable<PromptFilterMetadata> iterable) {
		Assert.notNull(iterable, "An Iterable of PromptFilterMetadata must not be null");
		// 【中文】关键一行：因为本接口是函数式接口（唯一抽象方法是 iterator()），
		// 这里用方法引用 iterable::iterator 就等价于写了一个匿名实现类，
		// 即"把 PromptMetadata 的迭代行为委托给传入的 iterable"。写法极简，但初读容易困惑。
		return iterable::iterator;
	}

	/**
	 * Returns an {@link Optional} {@link PromptFilterMetadata} at the given index.
	 * @param promptIndex index of the {@link PromptFilterMetadata} contained in this
	 * {@link PromptMetadata}.
	 * @return {@link Optional} {@link PromptFilterMetadata} at the given index.
	 * @throws IllegalArgumentException if the prompt index is less than 0.
	 */
	// 【中文】按提示词下标查找对应的过滤元数据，返回 Optional 以显式表达"可能查不到"。
	default Optional<PromptFilterMetadata> findByPromptIndex(int promptIndex) {

		// 【中文】参数校验：下标必须 >= 0（写成 > -1 与 >= 0 等价）；非法下标直接快速失败。
		Assert.isTrue(promptIndex > -1, "Prompt index [%d] must be greater than equal to 0".formatted(promptIndex));

		// 【中文】本接口只有 Iterable 能力、没有随机访问能力，所以借助 spliterator() 转成 Stream 后过滤。
		// 第二个参数 false 表示使用串行流（数据量很小，并行反而得不偿失）。
		return StreamSupport.stream(this.spliterator(), false)
			.filter(promptFilterMetadata -> promptFilterMetadata.getPromptIndex() == promptIndex)
			.findFirst();
	}

	/**
	 * Abstract Data Type (ADT) modeling filter metadata for all prompts sent during an AI
	 * request.
	 */
	/**
	 * 【中文说明】单条提示词的过滤（内容审核）元数据，是 {@link PromptMetadata} 中被遍历的元素类型。
	 *
	 * <p>
	 * 两个方法：{@link #getPromptIndex()} 表示这是第几条提示（与请求中提示的顺序一一对应），
	 * {@link #getContentFilterMetadata()} 返回厂商原始的审核结果对象。
	 */
	interface PromptFilterMetadata {

		/**
		 * Factory method used to construct a new {@link PromptFilterMetadata} with the
		 * given prompt index and content filter metadata.
		 * @param promptIndex index of the prompt filter metadata contained in the AI
		 * response.
		 * @param contentFilterMetadata underlying AI provider metadata for filtering
		 * applied to prompt content.
		 * @return a new instance of {@link PromptFilterMetadata} with the given prompt
		 * index and content filter metadata.
		 */
		// 【中文】静态工厂：由下标 + 厂商原始审核对象创建实例。
		static PromptFilterMetadata from(int promptIndex, Object contentFilterMetadata) {

			// 【中文】此处返回**匿名内部类**实例。之所以不能像外层那样用 Lambda，
			// 是因为本接口有两个抽象方法，不是函数式接口。
			// 两个方法直接捕获（闭包）了工厂方法的参数——参数在 Java 8+ 中隐式为 final，故可被安全捕获。
			return new PromptFilterMetadata() {

				@Override
				public int getPromptIndex() {
					return promptIndex;
				}

				@Override
				// 【中文】@SuppressWarnings("unchecked")：抑制下面强制类型转换的编译告警。
				// 由于目标类型 T 由调用方指定、编译期无法校验，类型写错会在运行时抛 ClassCastException。
				@SuppressWarnings("unchecked")
				public <T> T getContentFilterMetadata() {
					return (T) contentFilterMetadata;
				}
			};
		}

		/**
		 * Index of the prompt filter metadata contained in the AI response.
		 * @return an {@link Integer index} fo the prompt filter metadata contained in the
		 * AI response.
		 */
		// 【中文】该审核结果对应的是请求中第几条提示（从 0 开始）。
		int getPromptIndex();

		/**
		 * Returns the underlying AI provider metadata for filtering applied to prompt
		 * content.
		 * @param <T> {@link Class Type} used to cast the filtered content metadata into
		 * the AI provider-specific type.
		 * @return the underlying AI provider metadata for filtering applied to prompt
		 * content.
		 */
		// 【中文】返回厂商原始的内容审核结果对象；泛型 T 由调用方指定，需自行确保类型正确。
		<T> T getContentFilterMetadata();

	}

}
