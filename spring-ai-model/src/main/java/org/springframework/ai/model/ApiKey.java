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
 * Some model providers API leverage short-lived api keys which must be renewed at regular
 * intervals using another credential. For example, a GCP service account can be exchanged
 * for an api key to call Vertex AI.
 *
 * Model clients use the ApiKey interface to get an api key before they make any request
 * to the model provider. Implementations of this interface can cache the api key and
 * perform a key refresh when it is required.
 *
 * @author Adib Saikali
 */
/**
 * 【中文说明】ApiKey 是对"调用模型服务所需凭据"的抽象接口。
 *
 * <p>
 * 为什么不用简单的 String：部分厂商采用<b>短时效</b>密钥，需要用另一套凭据定期换取。
 * 例如 GCP 的 service account 需换成 access token 才能调用 Vertex AI。若直接传字符串，
 * 密钥过期后就无法自动续期。抽象成接口后，实现类可以在内部完成缓存与自动刷新。
 *
 * <p>
 * 核心契约（非常重要）：
 * <ul>
 * <li>调用方<b>不得</b>缓存 {@link #getValue()} 的返回值，每次需要时都应重新调用；</li>
 * <li>实现方<b>必须</b>保证返回的密钥未过期，刷新逻辑对调用方透明。</li>
 * </ul>
 *
 * <p>
 * 内置实现：{@link SimpleApiKey}（静态密钥）、{@link NoopApiKey}（无需密钥的场景，
 * 如本地 Ollama）。用户也可自行实现动态刷新型密钥。
 *
 * @author Adib Saikali
 */
public interface ApiKey {

	/**
	 * Returns an api key to use for a making request. Users of this method should NOT
	 * cache the returned api key, instead call this method whenever you need an api key.
	 * Implementors of this method MUST ensure that the returned key is not expired.
	 * @return the current value of the api key
	 */
	// 【中文】获取当前可用的密钥值。
	// 使用约定：每次发请求前实时调用，切勿把返回值存起来复用，否则会破坏自动续期机制。
	// 实现约定：必须返回未过期的密钥，必要时在方法内部先完成刷新再返回。
	String getValue();

}
