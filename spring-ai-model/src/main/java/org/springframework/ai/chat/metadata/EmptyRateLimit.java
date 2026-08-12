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
 * A RateLimit implementation that returns zero for all property getters
 *
 * @author John Blum
 * @since 0.7.0
 */
/**
 * 【中文说明】{@link RateLimit} 的"**空对象**"实现（Null Object 模式）：所有指标一律返回 0。
 *
 * <p>
 * 用途：当厂商响应中没有限流信息时，用它作为 {@link ChatResponseMetadata} 中
 * {@code rateLimit} 字段的默认值，替代 null，使调用方无需判空即可安全取值。
 *
 * <p>
 * 使用注意：返回 0 表示"**信息缺失**"，而不是"配额真的为 0"。因此不要仅凭
 * {@code getRequestsRemaining() == 0} 就判定被限流，需先确认厂商确实提供了该信息。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum；@since 0.7.0。
 */
public class EmptyRateLimit implements RateLimit {

	// 【中文】请求次数上限恒为 0（表示信息缺失）。
	@Override
	public Long getRequestsLimit() {
		return 0L;
	}

	// 【中文】剩余请求次数恒为 0。
	@Override
	public Long getRequestsRemaining() {
		return 0L;
	}

	// 【中文】请求配额重置时间恒为零时长；用 Duration.ZERO 常量而非 new，避免重复创建对象。
	@Override
	public Duration getRequestsReset() {
		return Duration.ZERO;
	}

	// 【中文】token 上限恒为 0。
	@Override
	public Long getTokensLimit() {
		return 0L;
	}

	// 【中文】剩余 token 恒为 0。
	@Override
	public Long getTokensRemaining() {
		return 0L;
	}

	// 【中文】token 配额重置时间恒为零时长。
	@Override
	public Duration getTokensReset() {
		return Duration.ZERO;
	}

}
