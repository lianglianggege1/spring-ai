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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.model.ModelResult;

/**
 * Represents a response returned by the AI.
 */
/**
 * 【中文说明】Generation 表示 AI 返回的"单条生成结果"，是 {@link ChatResponse} 中候选列表的元素。
 *
 * <p>
 * 它实现了 {@code ModelResult<AssistantMessage>}，即一次生成的产物统一表现为一条助手消息。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code assistantMessage}：模型生成的助手消息，内部包含文本内容、工具调用（toolCalls）、
 * 媒体附件以及消息级 metadata。</li>
 * <li>{@code chatGenerationMetadata}：本条生成的元数据，最典型的是 finishReason（结束原因）、
 * contentFilter（内容过滤信息）等。</li>
 * </ul>
 *
 * <p>
 * 空值处理：当未提供元数据时，统一使用空对象 {@code ChatGenerationMetadata.NULL}（空对象模式），
 * 因此 {@link #getMetadata()} 永远不会返回 null，调用方无需判空。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * Generation g = chatResponse.getResult();
 * String text = g.getOutput().getText();
 * String finishReason = g.getMetadata().getFinishReason();
 * }</pre>
 */
public class Generation implements ModelResult<AssistantMessage> {

	// 中文说明：模型生成的助手消息（文本 + 可能的工具调用），不可变
	private final AssistantMessage assistantMessage;

	// 中文说明：本条生成的元数据，如 finishReason。注意此字段未声明 final（历史原因），但实际不对外提供修改入口
	private ChatGenerationMetadata chatGenerationMetadata;

	// 中文说明：便捷构造器——无元数据时使用空对象 ChatGenerationMetadata.NULL 兜底
	public Generation(AssistantMessage assistantMessage) {
		this(assistantMessage, ChatGenerationMetadata.NULL);
	}

	// 中文说明：主构造器。空值处理：显式把 null 元数据替换为 NULL 空对象，
	// 保证 getMetadata() 的返回值恒非 null（空对象模式，避免调用方到处判空）。
	public Generation(AssistantMessage assistantMessage, ChatGenerationMetadata chatGenerationMetadata) {
		this.assistantMessage = assistantMessage;
		this.chatGenerationMetadata = chatGenerationMetadata != null ? chatGenerationMetadata
				: ChatGenerationMetadata.NULL;
	}

	// 中文说明：获取生成产物——助手消息本体（取文本用 getOutput().getText()）
	@Override
	public AssistantMessage getOutput() {
		return this.assistantMessage;
	}

	// 中文说明：获取本条生成的元数据，恒非 null（最差情况是 ChatGenerationMetadata.NULL）
	@Override
	public ChatGenerationMetadata getMetadata() {
		return this.chatGenerationMetadata;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Generation that)) {
			return false;
		}
		return Objects.equals(this.assistantMessage, that.assistantMessage)
				&& Objects.equals(this.chatGenerationMetadata, that.chatGenerationMetadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.assistantMessage, this.chatGenerationMetadata);
	}

	@Override
	public String toString() {
		return "Generation[" + "assistantMessage=" + this.assistantMessage + ", chatGenerationMetadata="
				+ this.chatGenerationMetadata + ']';
	}

}
