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

package org.springframework.ai.chat.observation;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.util.Assert;

/**
 * Context used to store metadata for chat model exchanges.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelObservationContext 是一次对话模型调用的"观测上下文"，
 * 在 Micrometer Observation 的生命周期内贯穿始终，用于携带请求与响应数据供各类 Handler 消费。
 *
 * <p>
 * 继承自 {@code ModelObservationContext<Prompt, ChatResponse>}，因此：
 * <ul>
 * <li>{@code getRequest()} 返回本次调用的 {@link Prompt}（观测创建时即确定）。</li>
 * <li>{@code getResponse()} 返回 {@link ChatResponse}，由调用方在拿到结果后回填，
 * 因此在观测开始阶段它可能为 null，所有读取处都必须判空。</li>
 * <li>{@code getOperationMetadata()} 返回操作元数据，这里固定为 CHAT 操作类型 + 具体 provider。</li>
 * </ul>
 *
 * <p>
 * 本类额外增加了 {@code streaming} 字段，用于区分同步调用与流式调用，最终会体现为观测标签
 * {@code gen_ai.request.is_stream}。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * var context = ChatModelObservationContext.builder()
 *     .prompt(prompt)
 *     .provider("openai")
 *     .streaming(false)
 *     .build();
 * }</pre>
 *
 * 注意：构造器是包级私有的，外部只能通过 {@link Builder} 创建实例。
 */
public class ChatModelObservationContext extends ModelObservationContext<Prompt, ChatResponse> {

	// 中文说明：是否为流式调用。true 表示 stream(...)，false 表示 call(...)
	private final boolean streaming;

	// 中文说明：包级私有构造器（无 public/protected 修饰），强制外部走 Builder 创建。
	// 这里固定把操作类型写死为 AiOperationType.CHAT，因为本上下文专用于对话场景。
	ChatModelObservationContext(Prompt prompt, String provider, boolean streaming) {
		super(prompt,
				AiOperationMetadata.builder().operationType(AiOperationType.CHAT.value()).provider(provider).build());
		this.streaming = streaming;
	}

	// 中文说明：判断本次调用是否为流式模式，供观测约定生成对应标签
	public boolean isStreaming() {
		return this.streaming;
	}

	// 中文说明：建造者入口
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】ChatModelObservationContext 的建造者。
	 *
	 * <p>
	 * 必填约束：{@code prompt} 与 {@code provider} 都必须设置，否则 {@link #build()} 会抛
	 * IllegalStateException；{@code streaming} 为可选项，默认 false（同步调用）。
	 */
	public static final class Builder {

		// 中文说明：本次调用的提示词，必填（标注 @Nullable 仅表示"构建过程中可能尚未赋值"）
		private @Nullable Prompt prompt;

		// 中文说明：模型服务提供商标识，如 "openai"、"ollama"，必填
		private @Nullable String provider;

		// 中文说明：是否流式，默认 false
		private boolean streaming;

		// 中文说明：私有构造器，只能由 ChatModelObservationContext.builder() 调用
		private Builder() {
		}

		// 中文说明：设置提示词（必填项）
		public Builder prompt(Prompt prompt) {
			this.prompt = prompt;
			return this;
		}

		// 中文说明：设置服务提供商标识（必填项）
		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		// 中文说明：设置是否为流式调用（可选项）
		public Builder streaming(boolean streaming) {
			this.streaming = streaming;
			return this;
		}

		// 中文说明：构建上下文对象。
		// 参数校验：这里用 Assert.state（而非 notNull）校验两个必填项，
		// 语义上表示"建造者当前状态不合法"，校验失败抛 IllegalStateException。
		// 通过校验后编译器也能确认 prompt/provider 非空，故可直接传给构造器。
		public ChatModelObservationContext build() {
			Assert.state(this.prompt != null, "Prompt must not be null");
			Assert.state(this.provider != null, "Provider must not be null");
			return new ChatModelObservationContext(this.prompt, this.provider, this.streaming);
		}

	}

}
