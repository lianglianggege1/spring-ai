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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.AdvisorUtils;
import org.springframework.util.Assert;

/**
 * Base advisor that implements common aspects of the {@link CallAdvisor} and
 * {@link StreamAdvisor}, reducing the boilerplate code needed to implement an advisor.
 * <p>
 * It provides default implementations for the
 * {@link #adviseCall(ChatClientRequest, CallAdvisorChain)} and
 * {@link #adviseStream(ChatClientRequest, StreamAdvisorChain)} methods, delegating the
 * actual logic to the {@link #before(ChatClientRequest, AdvisorChain advisorChain)} and
 * {@link #after(ChatClientResponse, AdvisorChain advisorChain)} methods.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 基础顾问类，实现 {@link CallAdvisor} 与 {@link StreamAdvisor} 的通用逻辑，
 * 减少实现顾问所需的样板代码。
 * <p>
 * 该类为 {@link #adviseCall(ChatClientRequest, CallAdvisorChain)} 与
 * {@link #adviseStream(ChatClientRequest, StreamAdvisorChain)} 方法提供默认实现，
 * 将实际业务逻辑委托给 {@link #before(ChatClientRequest, AdvisorChain advisorChain)}
 * 和 {@link #after(ChatClientResponse, AdvisorChain advisorChain)} 方法。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface BaseAdvisor extends CallAdvisor, StreamAdvisor {

	Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();

	@Override
	default ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
		Assert.notNull(callAdvisorChain, "callAdvisorChain cannot be null");

		ChatClientRequest processedChatClientRequest = before(chatClientRequest, callAdvisorChain);
		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(processedChatClientRequest);
		return after(chatClientResponse, callAdvisorChain);
	}

	@Override
	default Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
		Assert.notNull(streamAdvisorChain, "streamAdvisorChain cannot be null");
		Assert.notNull(getScheduler(), "scheduler cannot be null");

		Flux<ChatClientResponse> chatClientResponseFlux = Mono.just(chatClientRequest)
			.publishOn(getScheduler())
			.map(request -> this.before(request, streamAdvisorChain))
			.flatMapMany(streamAdvisorChain::nextStream);

		return chatClientResponseFlux.map(response -> {
			if (AdvisorUtils.onFinishReason().test(response)) {
				response = after(response, streamAdvisorChain);
			}
			return response;
		}).onErrorResume(error -> Flux.error(new IllegalStateException("Stream processing failed", error)));
	}

	@Override
	default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Logic to be executed before the rest of the advisor chain is called.
	 */
	/**
	 * 在执行顾问链其余逻辑之前运行的业务逻辑。
	 */
	ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain);

	/**
	 * Logic to be executed after the rest of the advisor chain is called.
	 */
	/**
	 * 在顾问链其余逻辑执行完毕后运行的业务逻辑。
	 */
	ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain);

	/**
	 * Scheduler used for processing the advisor logic when streaming.
	 */
	/**
	 * 流式处理时用于执行顾问逻辑的调度器。
	 */
	default Scheduler getScheduler() {
		return DEFAULT_SCHEDULER;
	}

}
