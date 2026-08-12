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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelRequest;

/**
 * 图像生成请求对象。是 {@link ModelRequest} 在图像模态下的实现，
 * 封装了「提示词消息列表 + 生成选项」，作为 {@link ImageModel#call(ImagePrompt)} 的入参。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code messages} —— 一条或多条 {@link ImageMessage}，支持多段带权重的提示词。</li>
 * <li>{@code imageModelOptions} —— 本次请求的生成选项（尺寸、数量、模型名等），可为 null。</li>
 * </ul>
 * 类中提供了多个重载构造方法，形成由简到繁的「便捷构造链」：
 * 字符串 → 单条消息 → 消息列表，最终都汇聚到持有 messages/options 的构造逻辑，
 * 让最常见的「一句话生成图片」写法保持极简。
 * <p>
 * 典型用法：{@code new ImagePrompt("一只戴墨镜的柴犬")}
 */
public class ImagePrompt implements ModelRequest<List<ImageMessage>> {

	// 提示词消息列表，构造后引用不可变
	private final List<ImageMessage> messages;

	// 本次请求的图像生成选项；使用仅传 messages 的构造方法时为 null，表示全部走模型默认值
	private @Nullable ImageOptions imageModelOptions;

	// 构造方法：只提供消息列表，不指定任何选项（options 保持 null）
	public ImagePrompt(List<ImageMessage> messages) {
		this.messages = messages;
	}

	// 核心构造方法：同时提供消息列表与生成选项，其余重载最终大多汇聚到这里
	public ImagePrompt(List<ImageMessage> messages, ImageOptions imageModelOptions) {
		this.messages = messages;
		this.imageModelOptions = imageModelOptions;
	}

	// 便捷构造方法：单条消息 + 选项；用 singletonList 包装为不可变单元素列表
	public ImagePrompt(ImageMessage imageMessage, ImageOptions imageOptions) {
		this(Collections.singletonList(imageMessage), imageOptions);
	}

	// 便捷构造方法：直接传提示词字符串 + 选项，内部自动包装成 ImageMessage
	public ImagePrompt(String instructions, ImageOptions imageOptions) {
		this(new ImageMessage(instructions), imageOptions);
	}

	// 最简构造方法：仅传提示词字符串；此处用 Builder 生成一份「全空」的默认选项，
	// 以保证 getOptions() 返回非 null 对象，避免下游实现频繁判空
	public ImagePrompt(String instructions) {
		this(new ImageMessage(instructions), ImageOptionsBuilder.builder().build());
	}

	// 返回提示词消息列表，对应 ModelRequest 契约中的「指令/输入」
	@Override
	public List<ImageMessage> getInstructions() {
		return this.messages;
	}

	// 返回本次请求的生成选项；使用单参数 List 构造方法时可能为 null
	@Override
	public @Nullable ImageOptions getOptions() {
		return this.imageModelOptions;
	}

	@Override
	public String toString() {
		return "NewImagePrompt{" + "messages=" + this.messages + ", imageModelOptions=" + this.imageModelOptions + '}';
	}

	// 值相等语义：消息列表与选项都相等才视为同一请求
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof ImagePrompt that)) {
			return false;
		}
		// options 可能为 null，使用 Objects.equals 做空安全比较
		return Objects.equals(this.messages, that.messages)
				&& Objects.equals(this.imageModelOptions, that.imageModelOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.messages, this.imageModelOptions);
	}

}
