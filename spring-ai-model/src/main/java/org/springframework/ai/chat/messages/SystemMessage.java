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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * A message of the type 'system' passed as input. The system message gives high level
 * instructions for the conversation. This role typically provides high-level instructions
 * for the conversation. For example, you might use a system message to instruct the
 * generative to behave like a certain character or to provide answers in a specific
 * format.
 */
/**
 * 系统消息：作为对话的顶层指令输入（role = system）。通常放在对话最前面，用来设定模型整体行为，
 * 例如"扮演某角色"或"以特定格式回答"。由开发者控制，不来自终端用户。由于系统消息必须有文本内容，
 * 构造时若文本为空会被父类校验拦截。
 */
public class SystemMessage extends AbstractMessage {

	/**
	 * 便捷构造器：用文本创建系统消息（无元数据）。
	 */
	public SystemMessage(@Nullable String textContent) {
		this(textContent, Map.of());
	}

	/**
	 * 用资源（如提示词文件）内容作为系统指令文本。
	 */
	public SystemMessage(Resource resource) {
		this(MessageUtils.readResource(resource), Map.of());
	}

	/**
	 * 私有全参构造器。
	 */
	private SystemMessage(@Nullable String textContent, Map<String, Object> metadata) {
		super(MessageType.SYSTEM, textContent, metadata);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SystemMessage that)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return Objects.equals(this.textContent, that.textContent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.textContent);
	}

	@Override
	public String toString() {
		return "SystemMessage{" + "textContent='" + this.textContent + '\'' + ", messageType=" + this.messageType
				+ ", metadata=" + this.metadata + '}';
	}

	/**
	 * 复制一份相同的系统消息（拷贝元数据）。
	 */
	public SystemMessage copy() {
		return new SystemMessage(getText(), Map.copyOf(this.metadata));
	}

	/**
	 * 基于当前消息创建可变 Builder，用于调整字段而不修改原对象。
	 */
	public Builder mutate() {
		Builder builder = new Builder();
		if (this.textContent != null) {
			builder.text(this.textContent);
		}
		builder.metadata(this.metadata);
		return builder;
	}

	/**
	 * 创建空 Builder。
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 建造者：流式构造 SystemMessage。
	 */
	public static final class Builder {

		private @Nullable String textContent;

		private @Nullable Resource resource;

		private Map<String, Object> metadata = new HashMap<>();

		public Builder text(String textContent) {
			this.textContent = textContent;
			return this;
		}

		public Builder text(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		/**
		 * 构建 SystemMessage。文本与资源互斥，同时设置抛异常；仅设置资源时读取其内容作为文本。
		 */
		public SystemMessage build() {
			if (StringUtils.hasText(this.textContent) && this.resource != null) {
				throw new IllegalArgumentException("textContent and resource cannot be set at the same time");
			}
			else if (this.resource != null) {
				this.textContent = MessageUtils.readResource(this.resource);
			}
			return new SystemMessage(this.textContent, this.metadata);
		}

	}

}
