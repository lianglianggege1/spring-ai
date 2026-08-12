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

package org.springframework.ai.support;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * A utility class to provide support methods handling {@link Usage}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Jewoo Shin
 */
/**
 * 【中文说明】Token 用量（{@link Usage}）的计算与累加工具类。
 *
 * <p>
 * 用途：在“多轮递归调用”场景下（典型如工具调用循环 tool-calling loop、结构化输出校验重试循环、
 * 流式响应聚合），单次请求会产生多个 {@link ChatResponse}，本类负责把各轮的 token 用量累加起来，
 * 最终对外暴露一份总用量，避免统计遗漏。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #getCumulativeUsage(Usage, ChatResponse)}：核心累加逻辑，合并当前用量与上一轮用量。</li>
 * <li>{@link #isEmpty(Usage)}：判空辅助方法，null 或 totalTokens 为 0 都视为空。</li>
 * <li>{@link #accumulateResponseUsage(ChatResponse, ChatResponse)}：面向响应对象的累加封装。</li>
 * <li>{@link #withUsage(ChatResponse, Usage)}：复制一份响应并替换其中的 usage。</li>
 * </ul>
 *
 * <p>
 * 重要约束：一旦发生实际相加，返回的是通用的 {@link DefaultUsage}，厂商专有的
 * {@link Usage#getNativeUsage() nativeUsage} 会丢失（因为不同响应的原生对象无法合并），
 * 只有 token 计数与缓存指标会被保留。
 *
 * <p>
 * 设计要点：{@code final class} + 私有构造器且主动抛异常，是比单纯私有构造器更严格的“禁止实例化”写法
 * （连反射调用也会失败）。
 *
 * @author Ilayaperumal Gopinathan
 * @author Jewoo Shin
 */
public final class UsageCalculator {

	// 私有构造器并主动抛异常：即使通过反射强行创建也会失败，彻底禁止实例化
	private UsageCalculator() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Accumulate usage tokens from the previous chat response to the current usage
	 * tokens.
	 * <p>
	 * Note: when the two usages are actually summed, the result is a plain
	 * {@link DefaultUsage} and the provider-specific {@link Usage#getNativeUsage() native
	 * usage} object is <em>not</em> preserved (it cannot be merged across responses).
	 * Only the token counts and cache metrics carry over. The original
	 * {@code currentUsage} (native usage included) is returned unchanged when there is
	 * nothing to accumulate.
	 * @param currentUsage the current usage.
	 * @param previousChatResponse the previous chat response.
	 * @return accumulated usage.
	 */
	// 核心方法：把上一轮响应的 token 用量累加到当前用量上，返回累计后的 Usage
	public static Usage getCumulativeUsage(final Usage currentUsage,
			final @Nullable ChatResponse previousChatResponse) {
		Usage usageFromPreviousChatResponse = null;
		// 分支一：存在上一轮响应，取出它的用量准备累加
		if (previousChatResponse != null) {
			usageFromPreviousChatResponse = previousChatResponse.getMetadata().getUsage();
		}
		else {
			// Return the current usage when the previous chat response usage is empty or
			// null.
			// 分支二：没有上一轮（首轮调用），无需累加，原样返回当前用量。
			// 注意此路径会完整保留厂商专有的 nativeUsage
			return currentUsage;
		}
		// For a valid usage from previous chat response, accumulate it to the current
		// usage.
		// 当前用量非空时才做真正的相加
		if (!isEmpty(currentUsage)) {
			Integer promptTokens = currentUsage.getPromptTokens();
			Integer generationTokens = currentUsage.getCompletionTokens();
			Integer totalTokens = currentUsage.getTotalTokens();
			// Make sure to accumulate the usage from the previous chat response.
			// 三类 token 分别累加（Integer 自动拆箱相加后再装箱）
			promptTokens += usageFromPreviousChatResponse.getPromptTokens();
			generationTokens += usageFromPreviousChatResponse.getCompletionTokens();
			totalTokens += usageFromPreviousChatResponse.getTotalTokens();
			// Accumulate cache metrics, preserving null when neither side reports them.
			// 缓存指标的累加策略：只要有一方上报就相加（缺失方按 0 处理）；
			// 若双方都没上报则保持 null，以区分“未上报”和“上报为 0”这两种语义
			Long cacheRead = null;
			if (currentUsage.getCacheReadInputTokens() != null
					|| usageFromPreviousChatResponse.getCacheReadInputTokens() != null) {
				cacheRead = (currentUsage.getCacheReadInputTokens() != null ? currentUsage.getCacheReadInputTokens()
						: 0L)
						+ (usageFromPreviousChatResponse.getCacheReadInputTokens() != null
								? usageFromPreviousChatResponse.getCacheReadInputTokens() : 0L);
			}
			// 缓存写入 token 采用与 cacheRead 完全一致的“任一非空则累加”策略
			Long cacheWrite = null;
			if (currentUsage.getCacheWriteInputTokens() != null
					|| usageFromPreviousChatResponse.getCacheWriteInputTokens() != null) {
				cacheWrite = (currentUsage.getCacheWriteInputTokens() != null ? currentUsage.getCacheWriteInputTokens()
						: 0L)
						+ (usageFromPreviousChatResponse.getCacheWriteInputTokens() != null
								? usageFromPreviousChatResponse.getCacheWriteInputTokens() : 0L);
			}
			// Native usage is passed as null: provider-specific objects cannot be merged
			// across responses, so only token counts and cache metrics are accumulated.
			// 第 4 个参数 nativeUsage 传 null：厂商原生用量对象跨响应无法合并，
			// 因此累加结果统一降级为通用的 DefaultUsage
			return new DefaultUsage(promptTokens, generationTokens, totalTokens, null, cacheRead, cacheWrite);
		}
		// When current usage is empty, return the usage from the previous chat response.
		// 当前用量为空（如流式中间块未带用量），直接返回上一轮的用量即可
		return usageFromPreviousChatResponse;
	}

	/**
	 * Check if the {@link Usage} is empty. Returns true when the {@link Usage} is null.
	 * Returns true when the {@link Usage} has zero tokens.
	 * @param usage the usage to check against.
	 * @return the boolean value to represent if it is empty.
	 */
	// 判空辅助：null 或 totalTokens 为 0 都视为“无有效用量”
	public static boolean isEmpty(@Nullable Usage usage) {
		// 短路求值：先判 null 再取值，避免 NPE；与 0L 比较会触发 Integer 拆箱后的数值提升
		return usage == null || usage.getTotalTokens() == 0L;
	}

	/**
	 * Folds the usage of the current chat response into the previously accumulated
	 * {@link ChatResponse}. The returned {@link ChatResponse} carries the cumulative
	 * usage (current plus previously accumulated). This is the building block for
	 * accumulating usage across the iterations of a recursive flow (for example a
	 * tool-calling loop or a validation retry loop).
	 * @param currentChatResponse the chat response produced by the current iteration, or
	 * {@code null}
	 * @param accumulatedChatResponse the chat response carrying the cumulative usage from
	 * previous iterations, or {@code null} if none yet
	 * @return a chat response carrying the cumulative usage, the previously accumulated
	 * response unchanged when the current response is {@code null}, or {@code null} when
	 * no usage has been reported
	 * @since 2.0.0
	 */
	// 递归/循环流程中的用量折叠：把本轮响应的用量并入历史累计响应，返回携带总用量的新响应
	public static @Nullable ChatResponse accumulateResponseUsage(@Nullable ChatResponse currentChatResponse,
			@Nullable ChatResponse accumulatedChatResponse) {
		// 本轮没有产生响应，直接沿用历史累计结果
		if (currentChatResponse == null) {
			return accumulatedChatResponse;
		}
		Usage currentUsage = currentChatResponse.getMetadata().getUsage();
		// 归一化处理：历史累计用量为空时统一按 null 传入，让 getCumulativeUsage 走“无需累加”分支
		ChatResponse previousChatResponse = isEmpty(getUsage(accumulatedChatResponse)) ? null : accumulatedChatResponse;
		Usage cumulativeUsage = getCumulativeUsage(currentUsage, previousChatResponse);
		// 两边都没有有效用量时返回 null，表示“本次调用未上报任何用量”
		if (isEmpty(cumulativeUsage)) {
			return null;
		}
		// 以当前响应为模板（保留最新的内容与元数据），仅把 usage 换成累计值
		return withUsage(currentChatResponse, cumulativeUsage);
	}

	/**
	 * Returns a copy of the given chat response with its usage replaced by the provided
	 * {@link Usage}, preserving all other response metadata.
	 * @param chatResponse the chat response to copy
	 * @param usage the usage to set on the copy
	 * @return a new chat response carrying the given usage
	 * @since 2.0.0
	 */
	// 写时复制：ChatResponse 不可变，所以“替换 usage”只能重新 build 一个副本
	public static ChatResponse withUsage(ChatResponse chatResponse, Usage usage) {
		return ChatResponse.builder()
			// from(...) 复制原响应的全部内容（generations 等）
			.from(chatResponse)
			// 仅覆盖元数据部分，其中 usage 被替换为新值
			.metadata(metadataWithUsage(chatResponse.getMetadata(), usage))
			.build();
	}

	// 私有辅助：逐字段复制元数据，只把 usage 换成新的
	private static ChatResponseMetadata metadataWithUsage(ChatResponseMetadata metadata, Usage usage) {
		ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder()
			.id(metadata.getId())
			.model(metadata.getModel())
			.rateLimit(metadata.getRateLimit())
			// 这里用传入的新 usage 覆盖原值，是整个方法的目的所在
			.usage(usage)
			.promptMetadata(metadata.getPromptMetadata());
		// 元数据还携带任意扩展键值对，需要逐条搬运，否则厂商自定义信息会丢失
		metadata.entrySet().forEach(entry -> builder.keyValue(entry.getKey(), entry.getValue()));
		return builder.build();
	}

	// 私有辅助：安全地从可能为 null 的响应中取出 usage
	private static @Nullable Usage getUsage(@Nullable ChatResponse chatResponse) {
		return chatResponse != null ? chatResponse.getMetadata().getUsage() : null;
	}

}
