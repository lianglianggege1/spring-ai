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

package org.springframework.ai.audio.tts;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelRequest;

/**
 * Implementation of the {@link ModelRequest} interface for the text to speech prompt.
 *
 * @author Alexandros Pappas
 */
/**
 * 语音合成请求对象，是 {@link ModelRequest} 在 TTS 场景下的实现。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code message} —— 待朗读的文本消息，构造后不可变。注意与图像模块不同，
 * 这里是单条消息而非列表：一次 TTS 请求只合成一段文本。</li>
 * <li>{@code options} —— 合成选项（模型、音色、格式、语速），非 final，可通过
 * {@link #setOptions} 在调用前替换，常用于框架层注入默认配置。</li>
 * </ul>
 * 四个重载构造方法形成便捷链：字符串/消息 × 带选项/不带选项，
 * 不带选项时会自动填入一份空的默认选项，保证 {@link #getOptions()} 不返回 null。
 * <p>
 * 典型用法：{@code new TextToSpeechPrompt("你好", options)}
 */
public class TextToSpeechPrompt implements ModelRequest<TextToSpeechMessage> {

	// 待合成的文本消息，构造后不可变
	private final TextToSpeechMessage message;

	// 合成选项；非 final，允许调用前通过 setOptions 覆盖（注意：这使本类并非不可变对象）
	private TextToSpeechOptions options;

	// 最简构造：仅传文本，自动包装成消息并填入空的默认选项
	public TextToSpeechPrompt(String text) {
		this(new TextToSpeechMessage(text), TextToSpeechOptions.builder().build());
	}

	// 便捷构造：文本 + 选项
	public TextToSpeechPrompt(String text, TextToSpeechOptions options) {
		this(new TextToSpeechMessage(text), options);
	}

	// 便捷构造：消息对象 + 默认空选项
	public TextToSpeechPrompt(TextToSpeechMessage message) {
		this(message, TextToSpeechOptions.builder().build());
	}

	// 核心构造方法：其余三个重载最终都汇聚到这里
	public TextToSpeechPrompt(TextToSpeechMessage message, TextToSpeechOptions options) {
		this.message = message;
		this.options = options;
	}

	// 返回待朗读的文本消息，对应 ModelRequest 契约中的「指令/输入」
	@Override
	public TextToSpeechMessage getInstructions() {
		return this.message;
	}

	// 返回合成选项；因所有构造方法都会赋值，故不会为 null
	@Override
	public TextToSpeechOptions getOptions() {
		return this.options;
	}

	// 替换合成选项。常见于框架层：先合并全局默认配置与请求级配置，再回写到本对象
	public void setOptions(TextToSpeechOptions options) {
		this.options = options;
	}

	// 值相等语义：消息与选项都相等才视为同一请求
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof TextToSpeechPrompt that)) {
			return false;
		}
		// 逐字段做空安全比较
		return Objects.equals(this.message, that.message) && Objects.equals(this.options, that.options);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.message, this.options);
	}

	@Override
	public String toString() {
		return "TextToSpeechPrompt{" + "message=" + this.message + ", options=" + this.options + '}';
	}

}
