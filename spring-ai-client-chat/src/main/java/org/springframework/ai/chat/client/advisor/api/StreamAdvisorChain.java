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

package org.springframework.ai.chat.client.advisor.api;

import java.util.List;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

/**
 * A chain of {@link StreamAdvisor} instances orchestrating the execution of a
 * {@link ChatClientRequest} on the next {@link StreamAdvisor} in the chain.
 *
 * @author Christian Tzolov
 * @author Dariusz Jedrzejczyk
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 由 {@link StreamAdvisor} 实例组成的顾问链，用于编排链中下一个 {@link StreamAdvisor}
 * 执行 {@link ChatClientRequest}。
 *
 * @author Christian Tzolov
 * @author Dariusz Jedrzejczyk
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface StreamAdvisorChain extends AdvisorChain {

	/**
	 * Invokes the next {@link StreamAdvisor} in the {@link StreamAdvisorChain} with the
	 * given request.
	 */
	/**
	 * 使用给定的请求调用 {@link StreamAdvisorChain} 中的下一个 {@link StreamAdvisor}。
	 */
	Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest);

	/**
	 * Returns the list of all the {@link StreamAdvisor} instances included in this chain
	 * at the time of its creation.
	 */
	/**
	 * 返回该顾问链创建时所包含的全部 {@link StreamAdvisor} 实例列表。
	 */
	List<StreamAdvisor> getStreamAdvisors();

	/**
	 * Creates a new StreamAdvisorChain copy that contains all advisors after the
	 * specified advisor.
	 * @param after the StreamAdvisor after which to copy the chain
	 * @return a new StreamAdvisorChain containing all advisors after the specified
	 * advisor
	 * @throws IllegalArgumentException if the specified advisor is not part of the chain
	 */
	/**
	 * 创建一个新的 StreamAdvisorChain 副本，包含指定顾问之后的所有顾问。
	 * @param after 以此 StreamAdvisor 为分界，复制其后的顾问链
	 * @return 新的 StreamAdvisorChain，包含指定顾问之后的全部顾问
	 * @throws IllegalArgumentException 如果指定顾问不在该顾问链中
	 */
	StreamAdvisorChain copy(StreamAdvisor after);

}
