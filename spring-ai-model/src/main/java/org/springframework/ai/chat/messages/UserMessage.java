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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A message of the type 'user' passed as input Messages with the user role are from the
 * end-user or developer. They represent questions, prompts, or any input that you want
 * the generative to respond to.
 */
/**
 * 用户消息：角色为「用户」的输入消息，由终端用户或开发者发出，承载问题、提示词或任何希望模型回答的内容。
 *
 * <p>实现 {@link MediaContent}，可携带图片/音频等多媒体附件（{@code media}），是多模态输入的关键。
 */
public class UserMessage extends AbstractMessage implements MediaContent {

	/**
	 * 多媒体附件列表（图片、音频、视频等），用于多模态输入。
	 */
	protected final List<Media> media;

	/**
	 * 便捷构造器：直接用文本创建用户消息（无附件、无元数据）。
	 */
	public UserMessage(@Nullable String textContent) {
		this(textContent, new ArrayList<>(), Map.of());
	}

	/**
	 * 私有全参构造器：完成字段校验与赋值。
	 */
	private UserMessage(@Nullable String textContent, Collection<Media> media, Map<String, Object> metadata) {
		super(MessageType.USER, textContent, metadata);
		Assert.notNull(media, "media cannot be null");
		Assert.noNullElements(media, "media cannot have null elements");
		this.media = new ArrayList<>(media);
	}

	/**
	 * 用资源（如文本文件）内容作为用户消息文本。
	 */
	public UserMessage(Resource resource) {
		this(MessageUtils.readResource(resource));
	}

	@Override
	public String toString() {
		return "UserMessage{" + "content='" + getText() + '\'' + ", metadata=" + this.metadata + ", messageType="
				+ this.messageType + '}';
	}

	/**
	 * 获取多媒体附件列表。
	 */
	@Override
	public List<Media> getMedia() {
		return this.media;
	}

	/**
	 * 复制一份相同的用户消息（深拷贝媒体与元数据）。
	 */
	public UserMessage copy() {
		return mutate().build();
	}

	/**
	 * 基于当前消息创建可变 Builder，用于在不修改原对象的前提下调整字段。
	 */
	public Builder mutate() {
		Builder builder = new Builder().media(List.copyOf(getMedia())).metadata(Map.copyOf(getMetadata()));
		if (this.textContent != null) {
			builder.text(this.textContent);
		}
		return builder;
	}

	/**
	 * 创建空 Builder。
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 建造者：以流式 API 构造 UserMessage。
	 */
	public static final class Builder {

		private @Nullable String textContent;

		private @Nullable Resource resource;

		private List<Media> media = new ArrayList<>();

		private Map<String, Object> metadata = new HashMap<>();

		public Builder text(String textContent) {
			this.textContent = textContent;
			return this;
		}

		public Builder text(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Builder media(List<Media> media) {
			this.media = media;
			return this;
		}

		public Builder media(Media... media) {
			this.media = Arrays.asList(media);
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		/**
		 * 构建 UserMessage。
		 *
		 * <p>关键约束：文本内容（textContent）与文本资源（resource）二者互斥，
		 * 同时设置会抛异常——因为无法确定到底用哪个作为消息文本。
		 */
		public UserMessage build() {
			if (StringUtils.hasText(this.textContent) && this.resource != null) {
				throw new IllegalArgumentException("textContent and resource cannot be set at the same time");
			}
			else if (this.resource != null) {
				// 仅设置了资源时，读取其内容作为文本
				this.textContent = MessageUtils.readResource(this.resource);
			}
			return new UserMessage(this.textContent, this.media, this.metadata);
		}

	}

}
