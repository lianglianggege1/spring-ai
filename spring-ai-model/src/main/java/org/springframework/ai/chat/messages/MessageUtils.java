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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Utility class for managing messages.
 *
 * @author Thomas Vitale
 */
/**
 * 消息内部工具类：负责把 {@link org.springframework.core.io.Resource} 读取为字符串，
 * 供 SystemMessage / UserMessage 用文件内容作为消息文本。
 *
 * <p>final + 私有构造器，纯工具类，不可实例化。
 *
 * @author Thomas Vitale
 */
final class MessageUtils {

	private MessageUtils() {
	}

	static String readResource(Resource resource) {
		return readResource(resource, Charset.defaultCharset());
	}

	/**
	 * 读取资源内容为字符串（指定字符集）。
	 */
	static String readResource(Resource resource, Charset charset) {
		Assert.notNull(resource, "resource cannot be null");
		Assert.notNull(charset, "charset cannot be null");
		try (InputStream inputStream = resource.getInputStream()) {
			return StreamUtils.copyToString(inputStream, charset);
		}
		catch (IOException ex) {
			// 读取失败属于不可恢复的配置/环境问题，包装为 RuntimeException 抛出
			throw new RuntimeException("Failed to read resource", ex);
		}
	}

}
