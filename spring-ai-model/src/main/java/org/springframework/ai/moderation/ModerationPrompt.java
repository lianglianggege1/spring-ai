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

import org.springframework.ai.model.ModelRequest;

/**
 * Represents a prompt for moderation containing a single message and the options for the
 * moderation model. This class offers constructors to create a prompt from a single
 * message or a simple instruction string, allowing for customization of moderation
 * options through `ModerationOptions`. It simplifies creating moderation requests for
 * different use cases.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】审核请求：送入 {@link ModerationModel} 的完整入参对象。
 *
 * <p>
 * 结构上由两部分组成——「审核什么」（{@link ModerationMessage}）与「怎么审核」
 * （{@link ModerationOptions}），这与 ChatModel 的 {@code Prompt = 消息 + 选项} 完全同构。
 *
 * <p>
 * 它实现 {@code ModelRequest<ModerationMessage>}，因此 {@link #getInstructions()}
 * 返回的是待审核消息本体。
 *
 * <p>
 * 三个构造器由繁到简，最常用的是只传文本的那个：
 *
 * <pre>{@code
 * new ModerationPrompt("待检测的文本"); // 使用默认选项
 * new ModerationPrompt("待检测的文本", ModerationOptionsBuilder.builder().model("xxx").build());
 * }</pre>
 */
public class ModerationPrompt implements ModelRequest<ModerationMessage> {

	// 中文：待审核消息，final 不可替换
	private final ModerationMessage message;

	// 中文：审核选项。非 final，允许构建后通过 setOptions 覆盖（例如在拦截器中统一改模型）
	private ModerationOptions moderationModelOptions;

	// 中文：主构造器
	public ModerationPrompt(ModerationMessage message, ModerationOptions moderationModelOptions) {
		this.message = message;
		this.moderationModelOptions = moderationModelOptions;
	}

	// 中文：便捷构造器——直接传文本，内部自动包装成 ModerationMessage
	public ModerationPrompt(String instructions, ModerationOptions moderationOptions) {
		this(new ModerationMessage(instructions), moderationOptions);
	}

	// 中文：最简构造器——只传文本，选项用 Builder 构建一个「空默认值」实例，
	// 保证 options 字段永不为 null，调用方无需判空
	public ModerationPrompt(String instructions) {
		this(new ModerationMessage(instructions), ModerationOptionsBuilder.builder().build());
	}

	@Override
	// 中文：实现 ModelRequest 接口，返回请求载荷（即待审核消息）
	public ModerationMessage getInstructions() {
		return this.message;
	}

	// 中文：获取审核选项
	public ModerationOptions getOptions() {
		return this.moderationModelOptions;
	}

	// 中文：替换审核选项（本对象在此维度上可变）
	public void setOptions(ModerationOptions moderationModelOptions) {
		this.moderationModelOptions = moderationModelOptions;
	}

	@Override
	public String toString() {
		return "ModerationPrompt{" + "message=" + this.message + ", moderationModelOptions="
				+ this.moderationModelOptions + '}';
	}

	@Override
	// 中文：按「消息 + 选项」两个字段做值相等比较
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModerationPrompt that)) {
			return false;
		}
		return Objects.equals(this.message, that.message)
				&& Objects.equals(this.moderationModelOptions, that.moderationModelOptions);
	}

	@Override
	// 中文：与 equals 使用相同字段，保证哈希契约一致
	public int hashCode() {
		return Objects.hash(this.message, this.moderationModelOptions);
	}

}
