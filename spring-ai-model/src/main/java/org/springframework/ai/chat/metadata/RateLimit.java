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

package org.springframework.ai.chat.metadata;

import java.time.Duration;

/**
 * Abstract Data Type (ADT) encapsulating metadata from an AI provider's API rate limits
 * granted to the API key in use and the API key's current balance.
 *
 * @author John Blum
 * @since 0.7.0
 */
/**
 * 【中文说明】API 限流（Rate Limit）元数据抽象接口，描述当前 API Key 的配额上限与剩余额度。
 *
 * <p>
 * 各厂商通常在 HTTP 响应头中返回这些信息（如 {@code x-ratelimit-remaining-requests}），
 * 适配层解析后封装成本接口。共 6 个指标，可分为对称的两组：
 * <ul>
 * <li>按**请求次数**限流：{@link #getRequestsLimit()} 上限、{@link #getRequestsRemaining()} 剩余、
 * {@link #getRequestsReset()} 距配额重置的时间；</li>
 * <li>按**token 数**限流：{@link #getTokensLimit()}、{@link #getTokensRemaining()}、
 * {@link #getTokensReset()}，含义同上。</li>
 * </ul>
 * 两组限制同时生效，任意一组耗尽都会被限流。
 *
 * <p>
 * 典型用法：{@code chatResponse.getMetadata().getRateLimit()}，据此实现客户端侧的
 * 主动降速或退避重试，避免触发 429 错误。
 *
 * <p>
 * 重置时间用 {@link Duration}（相对时长）而非绝对时间点表示，可规避客户端与服务端时钟不同步的问题。
 *
 * <p>
 * 空对象实现见 {@link EmptyRateLimit}。对应英文 javadoc 中的标签：@author John Blum；@since 0.7.0。
 */
public interface RateLimit {

	/**
	 * Returns the maximum number of requests that are permitted before exhausting the
	 * rate limit.
	 * @return an {@link Long} with the maximum number of requests that are permitted
	 * before exhausting the rate limit.
	 * @see #getRequestsRemaining()
	 */
	// 【中文】请求次数配额的上限（在限流窗口内最多允许发起多少次请求）。
	Long getRequestsLimit();

	/**
	 * Returns the remaining number of requests that are permitted before exhausting the
	 * {@link #getRequestsLimit() rate limit}.
	 * @return an {@link Long} with the remaining number of requests that are permitted
	 * before exhausting the {@link #getRequestsLimit() rate limit}.
	 * @see #getRequestsLimit()
	 */
	// 【中文】当前还剩多少次请求配额；降到 0 后继续调用通常会收到 HTTP 429。
	Long getRequestsRemaining();

	/**
	 * Returns the {@link Duration time} until the rate limit (based on requests) resets
	 * to its {@link #getRequestsLimit() initial state}.
	 * @return a {@link Duration} representing the time until the rate limit (based on
	 * requests) resets to its {@link #getRequestsLimit() initial state}.
	 * @see #getRequestsLimit()
	 */
	// 【中文】距离"请求次数"配额恢复到上限还需等待多久，是退避重试时最实用的等待时长依据。
	Duration getRequestsReset();

	/**
	 * Returns the maximum number of tokens that are permitted before exhausting the rate
	 * limit.
	 * @return an {@link Long} with the maximum number of tokens that are permitted before
	 * exhausting the rate limit.
	 * @see #getTokensRemaining()
	 */
	// 【中文】token 配额的上限（限流窗口内最多可消耗多少 token）。
	Long getTokensLimit();

	/**
	 * Returns the remaining number of tokens that are permitted before exhausting the
	 * {@link #getTokensLimit() rate limit}.
	 * @return an {@link Long} with the remaining number of tokens that are permitted
	 * before exhausting the {@link #getTokensLimit() rate limit}.
	 * @see #getTokensLimit()
	 */
	// 【中文】当前还剩多少 token 配额。请求量不大但单次上下文很长时，往往是这一项先耗尽。
	Long getTokensRemaining();

	/**
	 * Returns the {@link Duration time} until the rate limit (based on tokens) resets to
	 * its {@link #getTokensLimit() initial state}.
	 * @return a {@link Duration} with the time until the rate limit (based on tokens)
	 * resets to its {@link #getTokensLimit() initial state}.
	 * @see #getTokensLimit()
	 */
	// 【中文】距离 token 配额恢复到上限还需等待多久。
	Duration getTokensReset();

}
