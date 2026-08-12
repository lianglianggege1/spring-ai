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

package org.springframework.ai.document.id;

/**
 * Interface for generating unique document IDs.
 *
 * @author Aliakbar Jafarpour
 * @author Christian Tzolov
 */
/**
 * 用于生成文档唯一ID的接口。
 *
 * @author Aliakbar Jafarpour
 * @author Christian Tzolov
 */
public interface IdGenerator {

	/**
	 * Generate a unique ID for the given content. Note: some generator, such as the
	 * random generator might not depend on or use the content parameters.
	 * @param contents the content to generate an ID for.
	 * @return the generated ID.
	 */
	/**
	 * 为给定内容生成唯一ID。注意：部分生成器（例如随机生成器）可能不依赖或不使用该内容参数。
	 * @param contents 需要生成ID的内容
	 * @return 生成的唯一ID
	 */
	String generateId(Object... contents);

}
