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
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation for the {@link ChatOptions}.
 */
/**
 * 【中文说明】{@link ChatOptions} 的默认实现：一个只包含 8 个通用模型参数的**不可变**值对象。
 *
 * <p>
 * 关键设计：
 * <ul>
 * <li>所有字段均为 {@code final} 且带 {@code @Nullable}——null 表示"该参数不设置"，
 * 请求时不会发送给厂商，从而使用服务端默认值；</li>
 * <li>构造器是 {@code protected}——供 {@link DefaultChatOptionsBuilder} 及子类使用，
 * 普通调用方应走 {@code ChatOptions.builder()}；</li>
 * <li>重写了 {@code equals}/{@code hashCode}，可作为 Map 的 key 或用于配置比对。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code ChatOptions.builder().model("gpt-4o").temperature(0.7).build()}，
 * 返回的即是本类实例。
 */
public class DefaultChatOptions implements ChatOptions {

	// 【中文】以下 8 个字段与 ChatOptions 接口的 8 个 getter 一一对应，
	// 全部为 final + @Nullable：构造后不可变，null 表示"未设置该参数"。
	private final @Nullable String model;

	// 【中文】频率惩罚。
	private final @Nullable Double frequencyPenalty;

	// 【中文】最大生成 token 数。
	private final @Nullable Integer maxTokens;

	// 【中文】存在惩罚。
	private final @Nullable Double presencePenalty;

	// 【中文】停止序列列表。
	private final @Nullable List<String> stopSequences;

	// 【中文】温度（随机性）。
	private final @Nullable Double temperature;

	// 【中文】Top-K 采样。
	private final @Nullable Integer topK;

	// 【中文】Top-P（核）采样。
	private final @Nullable Double topP;

	// 【中文】全参构造器，protected 可见性：只允许 Builder 和子类调用，对外隐藏。
	protected DefaultChatOptions(@Nullable String model, @Nullable Double frequencyPenalty, @Nullable Integer maxTokens,
			@Nullable Double presencePenalty, @Nullable List<String> stopSequences, @Nullable Double temperature,
			@Nullable Integer topK, @Nullable Double topP) {
		this.model = model;
		this.frequencyPenalty = frequencyPenalty;
		this.maxTokens = maxTokens;
		this.presencePenalty = presencePenalty;
		// 【中文】唯一的可变引用类型字段需特别处理：用 List.copyOf 生成**不可变副本**，
		// 既防止外部后续修改原列表影响本对象，也保证本对象真正不可变。
		// 注意 List.copyOf 不接受 null，故须先判空（null 时直接保留 null）。
		this.stopSequences = stopSequences != null ? List.copyOf(stopSequences) : null;
		this.temperature = temperature;
		this.topK = topK;
		this.topP = topP;
	}

	// 【中文】以下均为简单的字段访问器，直接返回对应字段（可能为 null）。
	@Override
	public @Nullable String getModel() {
		return this.model;
	}

	@Override
	public @Nullable Double getFrequencyPenalty() {
		return this.frequencyPenalty;
	}

	@Override
	public @Nullable Integer getMaxTokens() {
		return this.maxTokens;
	}

	@Override
	public @Nullable Double getPresencePenalty() {
		return this.presencePenalty;
	}

	// 【中文】返回停止序列。此处虽直接返回引用，但构造时已用 List.copyOf 转为不可变列表，
	// 外部尝试修改会抛 UnsupportedOperationException，内部状态依然安全。
	@Override
	public @Nullable List<String> getStopSequences() {
		return this.stopSequences;
	}

	@Override
	public @Nullable Double getTemperature() {
		return this.temperature;
	}

	@Override
	public @Nullable Integer getTopK() {
		return this.topK;
	}

	@Override
	public @Nullable Double getTopP() {
		return this.topP;
	}

	// 【中文】mutate()：把当前对象的全部参数灌入一个新 Builder 并返回，
	// 便于"复制一份再改其中几项"（如复用默认配置、只改 temperature）。
	// 返回类型带通配符 <?>，因为具体子类会返回各自更具体的 Builder 类型。
	@Override
	public ChatOptions.Builder<?> mutate() {
		return ChatOptions.builder()
			.model(this.model)
			.frequencyPenalty(this.frequencyPenalty)
			.maxTokens(this.maxTokens)
			.presencePenalty(this.presencePenalty)
			.stopSequences(this.stopSequences)
			.temperature(this.temperature)
			.topK(this.topK)
			.topP(this.topP);
	}

	// 【中文】值相等性判断：8 个参数全部相等才算相等。
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		// 【中文】用 getClass() 严格比较类型：子类实例（如 OpenAiChatOptions）与本类实例永不相等。
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultChatOptions that = (DefaultChatOptions) o;
		return Objects.equals(this.model, that.model) && Objects.equals(this.frequencyPenalty, that.frequencyPenalty)
				&& Objects.equals(this.maxTokens, that.maxTokens)
				&& Objects.equals(this.presencePenalty, that.presencePenalty)
				&& Objects.equals(this.stopSequences, that.stopSequences)
				&& Objects.equals(this.temperature, that.temperature) && Objects.equals(this.topK, that.topK)
				&& Objects.equals(this.topP, that.topP);
	}

	// 【中文】与 equals 使用完全相同的 8 个字段计算哈希，保证二者语义一致。
	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.frequencyPenalty, this.maxTokens, this.presencePenalty, this.stopSequences,
				this.temperature, this.topK, this.topP);
	}

}
