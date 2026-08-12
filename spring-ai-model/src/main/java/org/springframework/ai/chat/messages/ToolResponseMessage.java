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

/**
 * The ToolResponseMessage class represents a message with a function content in a chat
 * application.
 *
 * @author Christian Tzolov
 * @author Eric Bottard
 * @since 1.0.0
 */
/**
 * 工具响应消息：承载「工具/函数调用的结果」，用于把外部系统的执行结果回传给模型，让模型据此继续生成最终回复。
 *
 * <p>典型流程：模型在 AssistantMessage 中给出 ToolCall → 应用侧执行对应工具 → 用本消息把结果
 * 回传 → 模型聚合后产出最终答案。
 *
 * @author Christian Tzolov
 * @author Eric Bottard
 * @since 1.0.0
 */
public class ToolResponseMessage extends AbstractMessage {

	/**
	 * 工具响应列表：一次消息可以包含多个工具的返回结果。
	 */
	protected final List<ToolResponse> responses;

	/**
	 * 受保护构造器。注意：传给父类的文本为 ""（空串），因为工具消息本身没有"文本回复"，结果都在 responses 里。
	 */
	protected ToolResponseMessage(List<ToolResponse> responses, Map<String, Object> metadata) {
		super(MessageType.TOOL, "", metadata);
		this.responses = responses;
	}

	/**
	 * 创建空 Builder。
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 获取工具响应列表。
	 */
	public List<ToolResponse> getResponses() {
		return this.responses;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ToolResponseMessage that)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return Objects.equals(this.responses, that.responses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.responses);
	}

	@Override
	public String toString() {
		return "ToolResponseMessage{" + "responses=" + this.responses + ", messageType=" + this.messageType
				+ ", metadata=" + this.metadata + '}';
	}

	/**
	 * 单个工具响应的不可变快照。
	 *
	 * @param id           工具调用标识，需与对应 ToolCall 的 id 一致以配对
	 * @param name         被调用的工具（函数）名称
	 * @param responseData 工具实际返回的数据（通常 JSON 或纯文本）
	 */
	public record ToolResponse(String id, String name, String responseData) {

	}

	/**
	 * 建造者：流式构造 ToolResponseMessage。
	 */
	public static final class Builder {

		private List<ToolResponse> responses = List.of();

		private Map<String, Object> metadata = Map.of();

		private Builder() {
		}

		public Builder responses(List<ToolResponse> responses) {
			this.responses = responses;
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		/**
		 * 构建 ToolResponseMessage 实例。
		 */
		public ToolResponseMessage build() {
			return new ToolResponseMessage(this.responses, this.metadata);
		}

	}

}
