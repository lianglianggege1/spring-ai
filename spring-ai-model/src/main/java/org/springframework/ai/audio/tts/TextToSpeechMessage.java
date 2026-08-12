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

/**
 * Implementation of the {@link TextToSpeechMessage} interface for the text to speech
 * message.
 *
 * @author Alexandros Pappas
 */
/**
 * 语音合成的输入消息，封装一段待转换为语音的文本。
 * <p>
 * 关键字段：{@code text} —— 待朗读的文本内容，构造后不可变。
 * <p>
 * 与 {@link org.springframework.ai.image.ImageMessage} 不同，本类不含权重等附加参数，
 * 是一个纯粹的文本包装类型；之所以不直接用 String，是为了让 {@link TextToSpeechPrompt}
 * 与框架的 ModelRequest 抽象保持一致，并为将来扩展（如 SSML、分段语速）预留空间。
 * <p>
 * 典型用法：{@code new TextToSpeechMessage("你好，世界")}
 */
public class TextToSpeechMessage {

	// 待合成为语音的文本内容
	private final String text;

	// 构造方法：传入待朗读文本
	public TextToSpeechMessage(String text) {
		this.text = text;
	}

	// 获取待朗读文本
	public String getText() {
		return this.text;
	}

	// 值相等语义：文本内容相同即视为同一条消息
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof TextToSpeechMessage that)) {
			return false;
		}
		// 空安全比较文本内容
		return Objects.equals(this.text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.text);
	}

	@Override
	public String toString() {
		return "TextToSpeechMessage{" + "text='" + this.text + '\'' + '}';
	}

}
