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

package org.springframework.ai.chat.messages;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Lets the generative know the content was generated as a response to the user. This role
 * indicates messages that the generative has previously generated in the conversation. By
 * including assistant messages in the series, you provide context to the generative about
 * prior exchanges in the conversation.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 助手消息：代表生成模型产出的回复。把助手消息放入对话历史，可向模型提供过往交互的上下文。
 *
 * <p>实现 {@link MediaContent}，可携带多媒体。重点字段 {@code toolCalls} 表示模型在回复中
 * 「请求调用」的工具；当模型决定调用工具时，回复里没有最终文本，而是给出工具调用请求，
 * 由应用侧执行后再用 {@link ToolResponseMessage} 把结果回传。
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class AssistantMessage extends AbstractMessage implements MediaContent {

	/**
	 * 模型请求调用的工具列表。非空即表示本次回复是"要执行工具"而非直接给文本答案。
	 */
	private final List<ToolCall> toolCalls;

	/**
	 * 多媒体附件（模型回复中可能包含的图片等）。
	 */
	protected final List<Media> media;

	/**
	 * 便捷构造器：仅文本，无工具调用、无附件。
	 */
	public AssistantMessage(@Nullable String content) {
		this(content, Map.of(), List.of(), List.of());
	}

	/**
	 * 受保护的全参构造器。
	 *
	 * <p>注意：第二个参数命名为 {@code properties}，实际传给父类的就是 {@code metadata}（同一概念），别被命名混淆。
	 */
	protected AssistantMessage(@Nullable String content, Map<String, Object> properties, List<ToolCall> toolCalls,
			List<Media> media) {
		super(MessageType.ASSISTANT, content, properties);
		Assert.notNull(toolCalls, "Tool calls must not be null");
		Assert.notNull(media, "Media must not be null");
		this.toolCalls = toolCalls;
		this.media = media;
	}

	/**
	 * 获取模型请求调用的工具列表。
	 */
	public List<ToolCall> getToolCalls() {
		return this.toolCalls;
	}

	/**
	 * 判断本次回复是否包含工具调用请求。
	 */
	public boolean hasToolCalls() {
		return !CollectionUtils.isEmpty(this.toolCalls);
	}

	/**
	 * 获取多媒体附件。
	 */
	@Override
	public List<Media> getMedia() {
		return this.media;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AssistantMessage that)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return Objects.equals(this.toolCalls, that.toolCalls) && Objects.equals(this.media, that.media);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.toolCalls, this.media);
	}

	@Override
	public String toString() {
		return "AssistantMessage [messageType=" + this.messageType + ", toolCalls=" + this.toolCalls + ", textContent="
				+ this.textContent + ", metadata=" + this.metadata + "]";
	}

	/**
	 * 创建空 Builder（使用无界泛型，返回具体 Builder 实例）。
	 */
	public static Builder<?> builder() {
		return new Builder<>();
	}

	/**
	 * 工具调用请求的不可变快照：记录模型"想调用哪个工具、传什么参数"。
	 *
	 * @param id        本次工具调用的唯一标识，用于后续匹配 ToolResponse 的 id
	 * @param type      工具类型（通常为 "function"）
	 * @param name      被调用的工具（函数）名称
	 * @param arguments 调用参数（JSON 字符串形式）
	 */
	public record ToolCall(String id, String type, String name, String arguments) {

	}

	/**
	 * 建造者：使用「自类型递归泛型」{@code B extends Builder<B>}，使子类继承时流式调用返回类型仍正确。
	 *
	 * @param <B> Builder 自身的子类型
	 */
	public static class Builder<B extends Builder<B>> {

		protected @Nullable String content;

		protected Map<String, Object> properties = Map.of();

		protected List<ToolCall> toolCalls = List.of();

		protected List<Media> media = List.of();

		protected Builder() {
		}

		/**
		 * 返回 this 的精确类型，用于维持流式 API 的返回类型。
		 */
		@SuppressWarnings("unchecked")
		protected B self() {
			return (B) this;
		}

		public B content(@Nullable String content) {
			this.content = content;
			return self();
		}

		public B properties(Map<String, Object> properties) {
			this.properties = properties;
			return self();
		}

		public B toolCalls(List<ToolCall> toolCalls) {
			this.toolCalls = toolCalls;
			return self();
		}

		public B media(List<Media> media) {
			this.media = media;
			return self();
		}

		/**
		 * 构建 AssistantMessage 实例。
		 */
		public AssistantMessage build() {
			return new AssistantMessage(this.content, this.properties, this.toolCalls, this.media);
		}

	}

}
