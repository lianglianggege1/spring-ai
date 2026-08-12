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

package org.springframework.ai.model.tool;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptionsBuilder;

/**
 * Default implementation of {@link StructuredOutputChatOptions}.
 *
 * Mainly to be used in model generic tests, as concrete chat implementations typically
 * use dedicated sub implementations specific to the model.
 *
 * @author Eric Bottard
 * @author Sebastien Deleuze
 */
/**
 * 【中文说明】{@link StructuredOutputChatOptions} 的默认实现。
 *
 * <p>
 * 在 {@link DefaultChatOptions} 的通用采样参数之外，仅增加一个 {@code outputSchema} 字段
 * （期望模型输出遵循的 JSON Schema 字符串）。
 *
 * <p>
 * 如英文注释所述，本类<b>主要用于模型层的通用测试</b>；实际接入的各家 ChatModel
 * 通常会提供自己专属的选项实现类（因为不同厂商传递 schema 的字段与格式不同）。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Eric Bottard、Sebastien Deleuze。
 */
public class DefaultStructuredOutputChatOptions extends DefaultChatOptions implements StructuredOutputChatOptions {

	// 【中文说明】输出 JSON Schema，不可变；未启用结构化输出时为 null
	private final @Nullable String outputSchema;

	// 【中文说明】受保护的全参构造器，由 Builder 调用；通用参数交给父类保存
	protected DefaultStructuredOutputChatOptions(@Nullable String model, @Nullable Double frequencyPenalty,
			@Nullable Integer maxTokens, @Nullable Double presencePenalty, @Nullable List<String> stopSequences,
			@Nullable Double temperature, @Nullable Integer topK, @Nullable Double topP,
			@Nullable String outputSchema) {
		super(model, frequencyPenalty, maxTokens, presencePenalty, stopSequences, temperature, topK, topP);
		this.outputSchema = outputSchema;
	}

	// 【中文说明】返回输出 JSON Schema（可能为 null）
	@Override
	public @Nullable String getOutputSchema() {
		return this.outputSchema;
	}

	/**
	 * 【中文说明】将当前对象所有字段回填到新的 Builder，实现「复制并修改」。
	 *
	 * <p>
	 * 注意这里返回的是接口默认 Builder（经由 {@code StructuredOutputChatOptions.builder()}），
	 * 因此 {@code mutate()} 的结果始终是 {@code DefaultStructuredOutputChatOptions} 系列。
	 */
	@Override
	public StructuredOutputChatOptions.Builder<?> mutate() {
		return StructuredOutputChatOptions.builder()
			.model(this.getModel())
			.frequencyPenalty(this.getFrequencyPenalty())
			.maxTokens(this.getMaxTokens())
			.presencePenalty(this.getPresencePenalty())
			.stopSequences(this.getStopSequences())
			.temperature(this.getTemperature())
			.topK(this.getTopK())
			.topP(this.getTopP())
			.outputSchema(this.getOutputSchema());
	}

	/**
	 * 【中文说明】相等性判断：严格类型比较 + 父类通用参数比较 + outputSchema 比较。
	 */
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		// 严格类型比较，保证 equals 的对称性
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		// 先比较父类持有的通用采样参数
		if (!super.equals(o)) {
			return false;
		}
		DefaultStructuredOutputChatOptions that = (DefaultStructuredOutputChatOptions) o;
		return Objects.equals(this.outputSchema, that.outputSchema);
	}

	// 【中文说明】与 equals 保持一致，将父类 hashCode 一并纳入计算
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.outputSchema);
	}

	/**
	 * 【中文说明】{@link StructuredOutputChatOptions.Builder} 的默认实现。
	 *
	 * <p>
	 * 同样使用<b>递归泛型</b>（{@code B extends Builder<B>}）配合 {@code self()}，
	 * 保证链式调用不丢失子类型。
	 */
	public static class Builder<B extends DefaultStructuredOutputChatOptions.Builder<B>>
			extends DefaultChatOptionsBuilder<B> implements StructuredOutputChatOptions.Builder<B> {

		// 【中文说明】构建过程中暂存的输出 JSON Schema
		protected @Nullable String outputSchema;

		// 【中文说明】设置输出 JSON Schema，直接覆盖旧值
		@Override
		public B outputSchema(@Nullable String outputSchema) {
			this.outputSchema = outputSchema;
			return self();
		}

		// 【中文说明】克隆 Builder；outputSchema 是不可变的 String，直接赋值即可，无需深拷贝
		@Override
		public B clone() {
			B copy = super.clone();
			copy.outputSchema = this.outputSchema;
			return copy;
		}

		// 【中文说明】收口构建，生成不可变的选项对象
		@Override
		public StructuredOutputChatOptions build() {
			return new DefaultStructuredOutputChatOptions(this.model, this.frequencyPenalty, this.maxTokens,
					this.presencePenalty, this.stopSequences, this.temperature, this.topK, this.topP,
					this.outputSchema);
		}

		/**
		 * 【中文说明】合并另一个 Builder 的配置。
		 *
		 * <p>
		 * 先由父类合并通用采样参数；随后仅当对方也是本类型 Builder 且其
		 * {@code outputSchema} 非 null 时才覆盖当前值——即「未设置不覆盖」，
		 * 避免用 null 把已有 schema 冲掉。
		 */
		@Override
		public B combineWith(ChatOptions.Builder<?> other) {
			super.combineWith(other);
			// 类型匹配才处理结构化输出字段
			if (other instanceof DefaultStructuredOutputChatOptions.Builder<?> that) {
				// 空值不参与覆盖，保护当前已有配置
				if (that.outputSchema != null) {
					this.outputSchema = that.outputSchema;
				}
			}
			return self();
		}

	}

}
