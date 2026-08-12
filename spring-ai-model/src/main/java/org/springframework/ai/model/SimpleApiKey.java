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

import org.springframework.util.Assert;

/**
 * A simple implementation of {@link ApiKey} that holds an immutable API key value. This
 * implementation is suitable for cases where the API key is static and does not need to
 * be refreshed or rotated.
 *
 * @author Adib Saikali
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】SimpleApiKey 是 {@link ApiKey} 最常用的实现：持有一个<b>不可变的静态密钥</b>。
 *
 * <p>
 * 用途：适用于密钥固定、无需轮换或刷新的场景，例如直接配置 OpenAI 的 sk-xxx。
 * 若密钥有时效性需要动态换取，则应自行实现 ApiKey 而非使用本类。
 *
 * <p>
 * 关键设计：
 * <ul>
 * <li>使用 Java {@code record} 定义，天然获得不可变性、equals/hashCode 以及
 * 访问器方法 {@code value()}，代码极为精简；</li>
 * <li>重写了 {@code toString()} 做<b>脱敏</b>处理，避免密钥出现在日志中造成泄露——
 * 这是本类最值得注意的安全细节，因为 record 默认的 toString 会打印全部字段值。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code new SimpleApiKey("sk-xxx")}，或由 Spring Boot 自动配置从
 * {@code spring.ai.openai.api-key} 属性构造。
 *
 * @author Adib Saikali
 * @author Christian Tzolov
 * @since 1.0.0
 */
public record SimpleApiKey(String value) implements ApiKey {

	/**
	 * Create a new SimpleApiKey.
	 * @param value the API key value, must not be null
	 * @throws IllegalArgumentException if value is null
	 */
	// 【中文】紧凑构造器的完整写法：record 允许显式声明规范构造器以插入校验逻辑。
	public SimpleApiKey(String value) {
		// 参数校验：密钥不允许为 null，违反则抛 IllegalArgumentException，
		// 保证对象一旦创建即处于合法状态（注意此处只校验 null，空串是允许的）
		Assert.notNull(value, "API key value must not be null");
		this.value = value;
	}

	// 【中文】实现 ApiKey 接口方法。内部直接委托给 record 自动生成的访问器 value()，
	// 因为密钥是静态的，无需任何刷新逻辑。
	@Override
	public String getValue() {
		return this.value();
	}

	// 【中文】安全脱敏：故意用 '***' 替代真实密钥，覆盖 record 默认会打印全部字段的 toString，
	// 防止密钥随日志、异常堆栈或调试输出意外泄露。
	@Override
	public String toString() {
		return "SimpleApiKey{value='***'}";
	}
}
