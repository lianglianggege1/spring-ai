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

package org.springframework.ai.chat.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.model.ModelResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * The chat completion (e.g. generation) response returned by an AI provider.
 *
 * @author Christian Tzolov
 * @author Mark Pollack
 * @author Soby Chacko
 * @author John Blum
 * @author Alexandros Pappas
 * @author Thomas Vitale
 */
/**
 * 【中文说明】ChatResponse 表示一次对话请求（chat completion）的完整响应结果，是 Spring AI 中模型输出的统一载体。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code generations}：生成结果列表。之所以是"列表"，是因为一次请求可以要求模型返回多个候选答案
 * （类似 OpenAI 的 n 参数）。列表在构造时通过 {@code List.copyOf} 拷贝为不可变集合，保证线程安全。</li>
 * <li>{@code chatResponseMetadata}：本次调用的元数据，包含 model 名称、请求 id、token 用量（Usage）、
 * 限流信息（RateLimit）、Prompt 元数据等。</li>
 * </ul>
 *
 * <p>
 * 常用方法：
 * <ul>
 * <li>{@link #getResult()}：取第一个（也是最常用的）生成结果，无结果时返回 null。</li>
 * <li>{@link #getResults()}：取全部候选结果。</li>
 * <li>{@link #hasToolCalls()}：判断模型是否请求了工具调用，是工具调用循环的关键判断条件。</li>
 * <li>{@link #hasFinishReasons(Set)}：判断结束原因是否命中给定集合（如 "STOP"、"TOOL_CALLS"）。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ChatResponse resp = chatModel.call(prompt);
 * String text = resp.getResult().getOutput().getText();
 * Usage usage = resp.getMetadata().getUsage();
 * }</pre>
 *
 * 该类同时实现了 equals/hashCode，可安全用于集合比较与断言测试。
 */
public class ChatResponse implements ModelResponse<Generation> {

	// 中文说明：本次响应的元数据（模型名、id、token 用量、限流信息等），构造时保证非 null
	private final ChatResponseMetadata chatResponseMetadata;

	/**
	 * List of generated messages returned by the AI provider.
	 */
	// 中文说明：AI 服务返回的生成结果列表，构造时已拷贝为不可变 List
	private final List<Generation> generations;

	/**
	 * Construct a new {@link ChatResponse} instance without metadata.
	 * @param generations the {@link List} of {@link Generation} returned by the AI
	 * provider.
	 */
	// 中文说明：便捷构造器——不带元数据时，自动补一个空的 ChatResponseMetadata，避免出现 null 元数据
	public ChatResponse(List<Generation> generations) {
		this(generations, new ChatResponseMetadata());
	}

	/**
	 * Construct a new {@link ChatResponse} instance.
	 * @param generations the {@link List} of {@link Generation} returned by the AI
	 * provider.
	 * @param chatResponseMetadata {@link ChatResponseMetadata} containing information
	 * about the use of the AI provider's API.
	 */
	// 中文说明：主构造器。三个要点：
	// 1) 参数校验：generations 不允许为 null，否则直接抛 IllegalArgumentException（快速失败）；
	// 2) 空值处理：元数据为 null 时用空对象兜底，调用方无需判空；
	// 3) 防御性拷贝：List.copyOf 生成不可变副本，外部后续修改原集合不会影响本对象。
	public ChatResponse(List<Generation> generations, ChatResponseMetadata chatResponseMetadata) {
		Assert.notNull(generations, "'generations' must not be null");
		this.chatResponseMetadata = Objects.requireNonNullElse(chatResponseMetadata, new ChatResponseMetadata());
		this.generations = List.copyOf(generations);
	}

	// 中文说明：建造者模式入口，适合需要逐项填充元数据的场景
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * The {@link List} of {@link Generation generated outputs}.
	 * <p>
	 * It is a {@link List} of {@link List lists} because the Prompt could request
	 * multiple output {@link Generation generations}.
	 * @return the {@link List} of {@link Generation generated outputs}.
	 */

	// 中文说明：返回全部候选生成结果（不可变列表）。一次 Prompt 可能要求返回多个候选答案，故为列表。
	@Override
	public List<Generation> getResults() {
		return this.generations;
	}

	/**
	 * @return Returns the first {@link Generation} in the generations list.
	 */
	// 中文说明：返回首条生成结果，这是日常使用最频繁的方法。
	// 空值处理：列表为空时返回 null（已用 @Nullable 标注），调用方需自行判空。
	public @Nullable Generation getResult() {
		if (CollectionUtils.isEmpty(this.generations)) {
			return null;
		}
		return this.generations.get(0);
	}

	/**
	 * @return Returns {@link ChatResponseMetadata} containing information about the use
	 * of the AI provider's API.
	 */
	// 中文说明：返回本次调用的元数据，构造器已保证其非 null，可直接使用无需判空
	@Override
	public ChatResponseMetadata getMetadata() {
		return this.chatResponseMetadata;
	}

	/**
	 * Whether the model has requested the execution of a tool.
	 */
	// 中文说明：判断模型是否发起了工具调用（function calling）。
	// 这是"工具调用循环"的关键判断：为 true 时框架需要执行工具并把结果回传给模型继续对话。
	// 只要任意一个候选结果包含 toolCalls 即返回 true。
	public boolean hasToolCalls() {
		if (CollectionUtils.isEmpty(this.generations)) {
			return false;
		}
		return this.generations.stream().anyMatch(generation -> generation.getOutput().hasToolCalls());
	}

	/**
	 * Whether the model has finished with any of the given finish reasons.
	 */
	// 中文说明：判断本次响应的"结束原因"是否命中给定集合中的任意一个（如 STOP、LENGTH、TOOL_CALLS）。
	// 实现细节：
	// 1) 参数校验：finishReasons 不允许为 null；
	// 2) 空值处理：某个 generation 的 finishReason 为 null 时用空串 "" 兜底，避免 NPE；
	// 3) 比较采用 equalsIgnoreCase，屏蔽各厂商大小写不一致的问题。
	public boolean hasFinishReasons(Set<String> finishReasons) {
		Assert.notNull(finishReasons, "finishReasons cannot be null");
		if (CollectionUtils.isEmpty(this.generations)) {
			return false;
		}
		return this.generations.stream().anyMatch(generation -> {
			var finishReason = (generation.getMetadata().getFinishReason() != null)
					? generation.getMetadata().getFinishReason() : "";
			return finishReasons.stream().anyMatch(fr -> fr.equalsIgnoreCase(finishReason));
		});
	}

	@Override
	public String toString() {
		return "ChatResponse [metadata=" + this.chatResponseMetadata + ", generations=" + this.generations + "]";
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChatResponse that)) {
			return false;
		}
		return Objects.equals(this.chatResponseMetadata, that.chatResponseMetadata)
				&& Objects.equals(this.generations, that.generations);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.chatResponseMetadata, this.generations);
	}

	/**
	 * 【中文说明】ChatResponse 的建造者（Builder 模式）。
	 *
	 * <p>
	 * 使用场景：需要分步骤填充生成结果与各类元数据时（典型如各厂商适配层解析 HTTP 响应、
	 * 或流式聚合完成后构造最终响应）。
	 *
	 * <p>
	 * 约束：{@code generations} 是必填项，{@link #build()} 时会做非空校验；元数据则可选，
	 * 未设置时会得到一个空的 ChatResponseMetadata。构造器为 private，只能通过
	 * {@link ChatResponse#builder()} 获取实例。
	 */
	public static final class Builder {

		// 中文说明：待构建的生成结果列表，可为 null，但 build() 时必须已被赋值
		private @Nullable List<Generation> generations;

		// 中文说明：元数据建造者，在 Builder 构造时即初始化，因此调用 metadata(...) 前无需判空
		private ChatResponseMetadata.Builder chatResponseMetadataBuilder;

		// 中文说明：私有构造器，强制通过 ChatResponse.builder() 创建，符合建造者模式惯例
		private Builder() {
			this.chatResponseMetadataBuilder = ChatResponseMetadata.builder();
		}

		// 中文说明：以另一个 ChatResponse 为模板做"拷贝式构建"，常用于在已有响应基础上做局部修改
		public Builder from(ChatResponse other) {
			this.generations = other.generations;
			return this.metadata(other.chatResponseMetadata);
		}

		// 中文说明：追加单个自定义元数据键值对
		public Builder metadata(String key, Object value) {
			this.chatResponseMetadataBuilder.keyValue(key, value);
			return this;
		}

		// 中文说明：批量复制另一份元数据。先逐项拷贝标准字段（model/id/rateLimit/usage/promptMetadata），
		// 再遍历 entrySet 把自定义扩展键值对也一并复制过来，确保不丢信息。
		public Builder metadata(ChatResponseMetadata other) {
			this.chatResponseMetadataBuilder.model(other.getModel());
			this.chatResponseMetadataBuilder.id(other.getId());
			this.chatResponseMetadataBuilder.rateLimit(other.getRateLimit());
			this.chatResponseMetadataBuilder.usage(other.getUsage());
			this.chatResponseMetadataBuilder.promptMetadata(other.getPromptMetadata());
			Set<Map.Entry<String, Object>> entries = other.entrySet();
			for (Map.Entry<String, Object> entry : entries) {
				this.chatResponseMetadataBuilder.keyValue(entry.getKey(), entry.getValue());
			}
			return this;
		}

		// 中文说明：设置生成结果列表（必填项）
		public Builder generations(List<Generation> generations) {
			this.generations = generations;
			return this;

		}

		// 中文说明：构建最终的不可变 ChatResponse。
		// 参数校验：generations 必填，为 null 时抛异常——这是 Builder 的必填约束检查点。
		public ChatResponse build() {
			Assert.notNull(this.generations, "'generations' must not be null");
			return new ChatResponse(this.generations, this.chatResponseMetadataBuilder.build());
		}

	}

}
