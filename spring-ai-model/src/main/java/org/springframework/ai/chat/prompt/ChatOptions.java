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

package org.springframework.ai.chat.prompt;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelOptions;

/**
 * {@link ModelOptions} representing the common options that are portable across different
 * chat models.
 */
/**
 * {@link ModelOptions}，代表可在各类对话模型间通用的公共配置项。
 */
/**
 * 【中文补充说明】ChatOptions 是"可移植（portable）"的模型参数抽象：只收录各家厂商**都支持**的通用参数，
 * 厂商特有参数则由各自的子类（如 OpenAiChatOptions）扩展。
 *
 * <p>
 * 关键参数含义速查：
 * <ul>
 * <li>{@code model}——模型名称，如 gpt-4o、deepseek-chat；</li>
 * <li>{@code temperature}——温度，取值越大输出越随机有创意，越小越稳定确定；</li>
 * <li>{@code topP}——核采样，只从累计概率达到 P 的词中挑选；通常与 temperature 二选一调节；</li>
 * <li>{@code topK}——只从概率最高的 K 个词中采样；</li>
 * <li>{@code maxTokens}——限制生成内容的最大 token 数，用于控制成本与响应长度；</li>
 * <li>{@code frequencyPenalty}——频率惩罚，按出现次数抑制重复用词；</li>
 * <li>{@code presencePenalty}——存在惩罚，只要出现过就抑制，鼓励谈及新话题；</li>
 * <li>{@code stopSequences}——停止序列，模型生成到这些字符串时立即停止。</li>
 * </ul>
 *
 * <p>
 * 所有 getter 返回值都标注了 {@code @Nullable}：null 表示"不设置该参数"，
 * 由厂商服务端使用自己的默认值，这与"设置为 0"有本质区别。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ChatOptions options = ChatOptions.builder().model("gpt-4o").temperature(0.7).build();
 * chatModel.call(new Prompt("你好", options));
 * }</pre>
 */
public interface ChatOptions extends ModelOptions {

	/**
	 * Returns the model to use for the chat.
	 * @return the model to use for the chat
	 */
	/**
	 * 获取当前对话所使用的模型。
	 * @return 对话使用的模型
	 */
	@Nullable String getModel();

	/**
	 * Returns the frequency penalty to use for the chat.
	 * @return the frequency penalty to use for the chat
	 */
	/**
	 * 获取对话使用的频率惩罚系数。
	 * @return 对话频率惩罚系数
	 */
	@Nullable Double getFrequencyPenalty();

	/**
	 * Returns the maximum number of tokens to use for the chat.
	 * @return the maximum number of tokens to use for the chat
	 */
	/**
	 * 获取对话可使用的最大令牌数量。
	 * @return 对话最大令牌数
	 */
	@Nullable Integer getMaxTokens();

	/**
	 * Returns the presence penalty to use for the chat.
	 * @return the presence penalty to use for the chat
	 */
	/**
	 * 获取对话使用的存在惩罚系数。
	 * @return 对话存在惩罚系数
	 */
	@Nullable Double getPresencePenalty();

	/**
	 * Returns the stop sequences to use for the chat.
	 * @return the stop sequences to use for the chat
	 */
	/**
	 * 获取对话所用的停止序列。
	 * @return 对话停止序列集合
	 */
	@Nullable List<String> getStopSequences();

	/**
	 * Returns the temperature to use for the chat.
	 * @return the temperature to use for the chat
	 */
	/**
	 * 获取对话使用的温度系数。
	 * @return 对话温度系数
	 */
	@Nullable Double getTemperature();

	/**
	 * Returns the top K to use for the chat.
	 * @return the top K to use for the chat
	 */
	/**
	 * 获取对话使用的Top-K参数。
	 * @return 对话Top-K取值
	 */
	@Nullable Integer getTopK();

	/**
	 * Returns the top P to use for the chat.
	 * @return the top P to use for the chat
	 */
	/**
	 * 获取对话使用的Top-P参数。
	 * @return 对话Top-P取值
	 */
	@Nullable Double getTopP();

	/**
	 * Returns a new {@link Builder} initialized with the values of this
	 * {@link ChatOptions}.
	 *
	 * Concrete ChatOptions classes must implement this and return the most concrete
	 * builder implementation.
	 */
	/**
	 * 根据当前{@link ChatOptions}的参数值，创建并返回一个全新初始化的{@link Builder}对象。
	 *
	 * 所有ChatOptions的具体实现类均需要实现该方法，且返回对应最具体的构建器实现。
	 */
	ChatOptions.Builder<?> mutate();

	/**
	 * Creates a new {@link Builder} to create the default {@link ChatOptions}.
	 * @return Returns a new {@link Builder}.
	 */
	/**
	 * 创建用于生成默认{@link ChatOptions}实例的{@link Builder}对象。
	 * @return 全新的Builder实例
	 */
	static ChatOptions.Builder<?> builder() {
		return new DefaultChatOptionsBuilder<>();
	}

	/**
	 * Builder for creating {@link ChatOptions} instance.
	 */
	/**
	 * 用于构建{@link ChatOptions}实例的构建器。
	 */
	/**
	 * 【中文补充说明】此处的 {@code Builder<B extends Builder<B>>} 是**递归泛型**
	 * （又称 CRTP，奇异递归模板模式），是本文件最需要留意的设计点。
	 *
	 * <p>
	 * 解决的问题：若配置方法统一返回 {@code Builder}，那么子类（如 OpenAiChatOptions.Builder）
	 * 在链式调用父类方法后，返回类型会"退化"成父接口，导致无法继续调用子类特有的方法：
	 *
	 * <pre>{@code
	 * // 若无递归泛型，下面这行编译失败——temperature() 返回的是父类型 Builder
	 * OpenAiChatOptions.builder().temperature(0.7).logitBias(...);
	 * }</pre>
	 *
	 * 引入类型参数 {@code B} 并约束 {@code B extends Builder<B>} 后，
	 * 子类声明为 {@code Builder<OpenAiBuilder>}，所有方法便返回子类自身类型，链式调用得以贯通。
	 * 代价是类型签名较难读——这是"自引用泛型"的典型取舍。
	 */
	interface Builder<B extends Builder<B>> extends Cloneable {

		// 【中文】克隆当前构建器（接口继承了 Cloneable）。
		// 用途：基于一份公共默认配置派生出多份互不影响的配置，避免反复从零设置。
		// 返回类型是 B 而非 Object，正是上面递归泛型带来的好处。
		B clone();

		/**
		 * Builds with the model to use for the chat.
		 * @param model
		 * @return the builder
		 */
		/**
		 * 设置对话所使用的模型。
		 * @param model 模型名称
		 * @return 当前构建器对象
		 */
		B model(@Nullable String model);

		/**
		 * Builds with the frequency penalty to use for the chat.
		 * @param frequencyPenalty
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的频率惩罚系数。
		 * @param frequencyPenalty 频率惩罚值
		 * @return 当前构建器实例
		 */
		B frequencyPenalty(@Nullable Double frequencyPenalty);

		/**
		 * Builds with the maximum number of tokens to use for the chat.
		 * @param maxTokens
		 * @return the builder.
		 */
		/**
		 * 设置对话可使用的最大令牌数量。
		 * @param maxTokens 最大令牌数
		 * @return 当前构建器实例
		 */
		B maxTokens(@Nullable Integer maxTokens);

		/**
		 * Builds with the presence penalty to use for the chat.
		 * @param presencePenalty
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的存在惩罚系数。
		 * @param presencePenalty 存在惩罚数值
		 * @return 当前构建器实例
		 */
		B presencePenalty(@Nullable Double presencePenalty);

		/**
		 * Builds with the stop sequences to use for the chat.
		 * @param stopSequences
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的停止序列。
		 * @param stopSequences 停止序列集合
		 * @return 当前构建器实例
		 */
		B stopSequences(@Nullable List<String> stopSequences);

		/**
		 * Builds with the temperature to use for the chat.
		 * @param temperature
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的温度系数。
		 * @param temperature 温度值
		 * @return 当前构建器实例
		 */
		B temperature(@Nullable Double temperature);

		/**
		 * Builds with the top K to use for the chat.
		 * @param topK
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的Top-K参数。
		 * @param topK Top-K取值
		 * @return 当前构建器实例
		 */
		B topK(@Nullable Integer topK);

		/**
		 * Builds with the top P to use for the chat.
		 * @param topP
		 * @return the builder.
		 */
		/**
		 * 设置对话使用的Top-P参数。
		 * @param topP Top-P取值
		 * @return 当前构建器实例
		 */
		B topP(@Nullable Double topP);

		/**
		 * Build the {@link ChatOptions}.
		 * @return the Chat options.
		 */
		/**
		 * 构建并返回一个不可变的{@link ChatOptions}实例。
		 * @return 配置完成的ChatOptions对象
		 */
		ChatOptions build();

		/**
		 * Mutate this builder by taking all {@code other}'s values that are non-null,
		 * retaining {@code this} other values.
		 */
		/**
		 * 将当前构建器实例与另一个构建器实例进行组合，将非空的参数值覆盖当前构建器的对应值。
		 * @param other 另一个构建器实例
		 * @return 当前构建器实例
		 */
		// 【中文补充】合并规则要点：只有 other 中**非 null** 的字段才会覆盖当前构建器的同名字段，
		// 当前构建器中 other 未设置（为 null）的字段保持不变。
		// 典型场景：把"运行时传入的 options"叠加到"全局默认 options"之上，实现按需覆盖。
		B combineWith(ChatOptions.Builder<?> other);

	}

}
