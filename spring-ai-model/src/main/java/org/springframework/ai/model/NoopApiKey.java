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

package org.springframework.ai.model;

/**
 * This implementation of ApiKey indicates that no API key should be used, e.g. no HTTP
 * headers should be set.
 *
 * @author Paul Bakker
 */
/**
 * 【中文说明】NoopApiKey 是 {@link ApiKey} 的"空实现"，表示<b>本次调用不需要任何密钥</b>。
 *
 * <p>
 * 用途：某些场景下模型服务无需鉴权，例如本地部署的 Ollama、LM Studio，或通过网关代理
 * 由外层统一注入认证信息。此时使用本类可以让上层代码保持统一的 ApiKey 编程模型，
 * 而不必到处写 if (apiKey != null) 的判空分支。
 *
 * <p>
 * 关键行为：{@link #getValue()} 返回<b>空字符串</b>而不是 null。框架在构造 HTTP 请求头时，
 * 通常会判断密钥是否为空，为空则<b>不设置</b> Authorization 头，这正是"noop"语义的实现方式。
 * 返回空串而非 null 也避免了下游出现 NullPointerException。
 *
 * <p>
 * 典型用法：{@code new OpenAiApi.Builder().apiKey(new NoopApiKey())...}
 *
 * @author Paul Bakker
 */
public class NoopApiKey implements ApiKey {

	// 【中文】固定返回空字符串，表示"无密钥"。
	// 注意：这里刻意不返回 null——空串既能被上层的 StringUtils.hasText 判定为"无值"从而跳过设置请求头，
	// 又不会引发空指针风险。
	@Override
	public String getValue() {
		return "";
	}

}
