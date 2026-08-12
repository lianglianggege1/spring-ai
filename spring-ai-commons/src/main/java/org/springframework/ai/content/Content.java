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

package org.springframework.ai.content;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Data structure that contains content and metadata. Common parent for the
 * {@link org.springframework.ai.document.Document} and the
 * {@link org.springframework.ai.chat.messages.Message} classes.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 包含内容与元数据的数据结构。
 * 是 {@link org.springframework.ai.document.Document} 和
 * {@link org.springframework.ai.chat.messages.Message} 的公共父接口。
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @since 1.0.0
 */
public interface Content {

	/**
	 * Get the content of the message.
	 * @return the content of the message
	 */
	/**
	 * 获取消息的内容。
	 * @return 消息内容
	 */
	@Nullable String getText();

	/**
	 * Get the metadata associated with the content.
	 * @return the metadata associated with the content
	 */
	/**
	 * 获取与内容关联的元数据。
	 * @return 内容对应的元数据
	 */
	Map<String, Object> getMetadata();

}
