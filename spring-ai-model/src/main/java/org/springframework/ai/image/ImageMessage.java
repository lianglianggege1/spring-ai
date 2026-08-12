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

package org.springframework.ai.image;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * 图像生成的提示词消息。一条消息 = 一段描述文本 + 可选权重。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code text} —— 描述期望画面的提示词文本，必填。</li>
 * <li>{@code weight} —— 该段提示词的权重，可空。正值表示强调，负值可用于负向提示
 * （具体语义取决于底层模型，如 Stability AI 支持多段带权重的提示词；OpenAI 则忽略该字段）。</li>
 * </ul>
 * 一个 {@link ImagePrompt} 可携带多条本对象，以实现「多段加权提示词」的效果。
 * <p>
 * 典型用法：{@code new ImageMessage("a cat on the moon", 0.8f)}
 */
public class ImageMessage {

	// 提示词文本，构造后不可变
	private final String text;

	// 该段提示词的权重，可为 null 表示不指定；是否生效取决于底层模型能力
	private @Nullable Float weight;

	// 便捷构造方法：只给文本，不指定权重（weight 保持 null）
	public ImageMessage(String text) {
		this.text = text;
	}

	// 全参构造方法：同时指定提示词文本与权重
	public ImageMessage(String text, Float weight) {
		this.text = text;
		this.weight = weight;
	}

	// 获取提示词文本
	public String getText() {
		return this.text;
	}

	// 获取提示词权重，可能为 null（未指定）
	public @Nullable Float getWeight() {
		return this.weight;
	}

	@Override
	public String toString() {
		return "ImageMessage{" + "text='" + this.text + '\'' + ", weight=" + this.weight + '}';
	}

	// 值相等语义：文本与权重都相同才视为同一条提示词消息
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，直接相等
		if (this == o) {
			return true;
		}
		// 类型不匹配（含传入 null）时返回 false
		if (!(o instanceof ImageMessage that)) {
			return false;
		}
		// weight 可能为 null，故用 Objects.equals 做空安全比较
		return Objects.equals(this.text, that.text) && Objects.equals(this.weight, that.weight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.text, this.weight);
	}

}
