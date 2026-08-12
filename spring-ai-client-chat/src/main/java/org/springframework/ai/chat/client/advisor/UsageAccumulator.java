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

package org.springframework.ai.chat.client.advisor;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.support.UsageCalculator;

/**
 * Accumulates token {@link Usage} across the iterations of a recursive advisor (for
 * example a tool-calling loop or a structured-output validation retry loop), so that the
 * final response reports the cumulative usage of every model call rather than only the
 * last one.
 * <p>
 * An instance is stateful and not thread-safe: create one per non-streaming
 * {@code adviseCall} invocation, or one per stream subscription (inside a
 * {@link Flux#defer}) to keep the accumulated usage subscription-local.
 * <p>
 * The token arithmetic and {@link ChatResponse} metadata copying are delegated to
 * {@link UsageCalculator}; this class owns the recursive advisor loop state and the final
 * response stamping.
 * <p>
 * Typical non-streaming use:
 *
 * <pre>{@code
 * UsageAccumulator usage = new UsageAccumulator();
 * ChatClientResponse response;
 * do {
 *     response = chain.nextCall(request);
 *     usage.addRoundResponse(response.chatResponse());
 *     // ... decide whether to loop again ...
 * }
 * while (loopAgain);
 * return usage.applyAccumulatedUsage(response);
 * }</pre>
 *
 * @author Christian Tzolov
 * @author Jewoo Shin
 * @since 2.0.0
 */
/**
 * 在递归顾问的多次迭代中累计 {@link Usage} 令牌使用量
 *（例如工具调用循环或结构化输出校验重试循环），
 * 使最终响应上报每一次模型调用的累计用量，而不仅仅是最后一次调用的用量。
 * <p>
 * 该实例为有状态对象，非线程安全：
 * 针对每个非流式 {@code adviseCall} 调用创建一个实例，
 * 或在 {@link Flux#defer} 内部为每一个流订阅创建实例，保证用量累计作用于对应订阅域。
 * <p>
 * 令牌计算以及 {@link ChatResponse} 元数据拷贝逻辑委托给 {@link UsageCalculator}；
 * 本类维护递归顾问循环状态并负责给最终响应打上累计用量标记。
 * <p>
 * 典型非流式用法：
 *
 * <pre>{@code
 * UsageAccumulator usage = new UsageAccumulator();
 * ChatClientResponse response;
 * do {
 *     response = chain.nextCall(request);
 *     usage.addRoundResponse(response.chatResponse());
 *     // ... decide whether to loop again ...
 * }
 * while (loopAgain);
 * return usage.applyAccumulatedUsage(response);
 * }</pre>
 *
 * @author Christian Tzolov
 * @author Jewoo Shin
 * @since 2.0.0
 */
public final class UsageAccumulator {

	private @Nullable ChatResponse accumulated;

	/**
	 * Returns the cumulative usage folded in so far, or {@code null} if none. In a
	 * streaming loop this is the total of the <em>previous</em> rounds, since it is read
	 * before the in-flight round is {@link #addRoundResponse(ChatResponse) added}.
	 * @return the accumulated chat response carrying the cumulative usage, or
	 * {@code null}
	 */
	/**
	 * 返回目前已汇总的累计用量，若无则返回 {@code null}。
	 * 在流式循环场景下，该值代表**之前所有轮次**的总用量，
	 * 因为读取该值时，当前正在执行的轮次还未通过 {@link #addRoundResponse(ChatResponse)} 加入统计。
	 * @return 携带累计用量的聊天响应对象，或 {@code null}
	 */
	public @Nullable ChatResponse accumulatedResponse() {
		return this.accumulated;
	}

	/**
	 * Folds the usage of the given round response into the running total.
	 * @param roundChatResponse the response produced by the current round, or
	 * {@code null}
	 * @return the updated cumulative total, or {@code null} if still empty
	 */
	/**
	 * 将当前轮次响应的用量合并到累计总量中。
	 * @param roundChatResponse 当前轮次产生的响应，可以为 {@code null}
	 * @return 更新后的累计总用量，尚无数据时返回 {@code null}
	 */
	public @Nullable ChatResponse addRoundResponse(@Nullable ChatResponse roundChatResponse) {
		this.accumulated = UsageCalculator.accumulateResponseUsage(roundChatResponse, this.accumulated);
		return this.accumulated;
	}

	/**
	 * Stamps the accumulated total usage onto the given response. Used to finalize a
	 * non-streaming loop or a streaming return-direct result. The response is returned
	 * unchanged when there is nothing accumulated, or when its usage already equals the
	 * accumulated total.
	 * @param chatClientResponse the response to stamp the total onto
	 * @return the response carrying the accumulated total usage
	 */
	/**
	 * 将累计总用量标记到传入的响应对象上。用于完成非流式循环或流式直接返回结果的收尾处理。
	 * 当没有累计数据，或者响应本身的用量已经等于累计总量时，直接原样返回该响应。
	 * @param chatClientResponse 需要打上总用量标记的响应对象
	 * @return 携带累计总用量的响应对象
	 */
	public ChatClientResponse applyAccumulatedUsage(ChatClientResponse chatClientResponse) {
		return stampTotalUsage(chatClientResponse, this.accumulated);
	}

	private static ChatClientResponse stampTotalUsage(ChatClientResponse chatClientResponse,
			@Nullable ChatResponse accumulatedChatResponse) {
		ChatResponse currentChatResponse = chatClientResponse.chatResponse();
		if (currentChatResponse == null || accumulatedChatResponse == null) {
			return chatClientResponse;
		}
		Usage accumulatedUsage = accumulatedChatResponse.getMetadata().getUsage();
		if (UsageCalculator.isEmpty(accumulatedUsage)
				|| Objects.equals(currentChatResponse.getMetadata().getUsage(), accumulatedUsage)) {
			return chatClientResponse;
		}
		return chatClientResponse.mutate()
			.chatResponse(UsageCalculator.withUsage(currentChatResponse, accumulatedUsage))
			.build();
	}

	/**
	 * Adds the previous rounds' accumulated usage onto a single streamed chunk that
	 * already carries its own usage. Chunks without usage (or when there is no previous
	 * accumulated response) are returned unchanged, so the previous rounds' usage is
	 * reflected only on usage-bearing chunks.
	 * @param chunk the streamed chunk
	 * @param previousAccumulatedResponse the response carrying the accumulated usage of
	 * previous rounds, or {@code null}
	 * @return the chunk with the previous rounds' usage added to its usage, or unchanged
	 */
	/**
	 * 将之前轮次的累计用量叠加到已携带自身用量的流式分片上。
	 * 对于不带用量的分片（或不存在之前累计响应的场景），直接原样返回分片，
	 * 即仅在携带用量的分片上体现之前轮次的消耗统计。
	 * @param chunk 流式返回分片
	 * @param previousAccumulatedResponse 携带之前轮次累计用量的响应对象，可以为 {@code null}
	 * @return 叠加了历史轮次用量的分片，无需处理时返回原分片
	 */
	static ChatClientResponse applyPreviousAccumulatedUsageToChunk(ChatClientResponse chunk,
			@Nullable ChatResponse previousAccumulatedResponse) {
		ChatResponse currentChatResponse = chunk.chatResponse();
		if (currentChatResponse == null || previousAccumulatedResponse == null) {
			return chunk;
		}
		Usage currentUsage = currentChatResponse.getMetadata().getUsage();
		if (UsageCalculator.isEmpty(currentUsage)
				|| UsageCalculator.isEmpty(previousAccumulatedResponse.getMetadata().getUsage())) {
			return chunk;
		}
		Usage cumulativeUsage = UsageCalculator.getCumulativeUsage(currentUsage, previousAccumulatedResponse);
		if (Objects.equals(currentUsage, cumulativeUsage)) {
			return chunk;
		}
		return chunk.mutate().chatResponse(UsageCalculator.withUsage(currentChatResponse, cumulativeUsage)).build();
	}

	/**
	 * Builds the trailing usage-only emission for a streaming loop whose final round
	 * reported no usage of its own. In that case the accumulated total from previous
	 * rounds was never stamped onto an emitted chunk, so a usage-only
	 * {@link ChatClientResponse} (with empty generations, so no content is duplicated)
	 * carrying the accumulated total is emitted. Returns an empty flux when no correction
	 * is needed (the round reported its own usage, or there is nothing accumulated to
	 * preserve).
	 * @param aggregatedResponse the aggregated response of the final round
	 * @param roundChatResponse the final round's own response (its own usage)
	 * @param accumulatedChatResponse the response carrying the cumulative usage
	 * @return a single usage-only response, or an empty flux when no correction is needed
	 */
	/**
	 * 为流式循环构建仅携带用量的尾部输出报文，适用于最后一轮本身未上报任何用量的场景。
	 * 该场景下，历史轮次的累计用量从未标记到任何已发出的分片，因此会发出一条
	 * 仅包含用量信息的 {@link ChatClientResponse}（生成内容为空，避免内容重复），携带全部累计用量。
	 * 无需修正时返回空Flux（例如当前轮已上报自身用量，或无需要保留的累计用量）。
	 * @param aggregatedResponse 最后一轮的聚合响应
	 * @param roundChatResponse 最后一轮自身的响应（携带本轮用量）
	 * @param accumulatedChatResponse 携带累计总用量的响应对象
	 * @return 仅携带用量的单条响应；无需修正则返回空Flux
	 */
	static Flux<ChatClientResponse> emitFinalUsageCorrectionIfNecessary(ChatClientResponse aggregatedResponse,
			@Nullable ChatResponse roundChatResponse, @Nullable ChatResponse accumulatedChatResponse) {
		if (accumulatedChatResponse == null) {
			return Flux.empty();
		}
		Usage accumulatedUsage = accumulatedChatResponse.getMetadata().getUsage();
		if (!UsageCalculator.isEmpty(getUsage(roundChatResponse)) || UsageCalculator.isEmpty(accumulatedUsage)) {
			return Flux.empty();
		}
		ChatResponse usageOnlyChatResponse = ChatResponse.builder()
			.from(accumulatedChatResponse)
			.generations(List.of())
			.build();
		return Flux.just(aggregatedResponse.mutate().chatResponse(usageOnlyChatResponse).build());
	}

	private static @Nullable Usage getUsage(@Nullable ChatResponse chatResponse) {
		return chatResponse != null ? chatResponse.getMetadata().getUsage() : null;
	}

}
