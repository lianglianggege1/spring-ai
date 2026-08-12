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

package org.springframework.ai.moderation;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Represents a single message intended for moderation, encapsulating the text content.
 * This class provides a basic structure for messages that can be submitted to moderation
 * processes.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】待审核消息：对「一段需要做合规检查的文本」的最简单包装。
 *
 * <p>
 * 它只有一个 {@code text} 字段。之所以不直接用 String 而要包一层，是为了与 Spring AI
 * 「请求对象都是强类型」的设计保持一致，也便于将来在不破坏 API 的前提下扩展字段
 * （例如附加语言、来源渠道等元信息）。
 *
 * <p>
 * 它作为 {@link ModerationPrompt} 的载荷使用，一个 Prompt 对应一条 Message。
 *
 * <p>
 * 注意：本类是<b>可变的</b>（提供了 setText），并非不可变值对象。
 */
public class ModerationMessage {

	// 中文：待审核的文本内容
	private String text;

	// 中文：构造器，直接传入待审核文本
	public ModerationMessage(String text) {
		this.text = text;
	}

	// 中文：读取待审核文本
	public String getText() {
		return this.text;
	}

	// 中文：修改待审核文本（本类可变）
	public void setText(String text) {
		this.text = text;
	}

	@Override
	// 中文：便于日志排查的字符串形式
	public String toString() {
		return "ModerationMessage{" + "text='" + this.text + '\'' + '}';
	}

	@Override
	// 中文：按值比较——先判引用相同，再用 instanceof 模式匹配（Java 16+ 语法，兼顾类型判断与转型），
	// 最后用 Objects.equals 做可空安全的字段比较
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModerationMessage that)) {
			return false;
		}
		return Objects.equals(this.text, that.text);
	}

	@Override
	// 中文：与 equals 保持一致，基于同一字段计算哈希
	public int hashCode() {
		return Objects.hash(this.text);
	}

}
