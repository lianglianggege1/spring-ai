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

package org.springframework.ai.audio.transcription;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResult;

/**
 * Represents a response returned by the AI.
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
/**
 * 【中文说明】音频转写（语音转文字）的单条结果对象。
 *
 * <p>
 * 用途：作为 {@link ModelResult} 的实现，承载模型对一段音频识别出的文本内容，是
 * {@code AudioTranscriptionResponse} 内部持有的“结果单元”。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code text}：转写得到的文本，final 不可变，通过构造器一次性传入。</li>
 * <li>{@code transcriptionMetadata}：结果级元数据，默认值为
 * {@link AudioTranscriptionMetadata#NULL}（空对象模式），因此调用 {@code getMetadata()} 永远不会返回
 * null，调用方无需判空。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * AudioTranscription t = new AudioTranscription("你好，世界");
 * String text = t.getOutput();
 * }</pre>
 *
 * <p>
 * 该类重写了 equals/hashCode，按 text + metadata 做值相等判断，便于在测试与集合中比较。
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
public class AudioTranscription implements ModelResult<String> {

	// 转写出的文本内容，构造后不可变
	private final String text;

	// 结果级元数据；默认使用 NULL 空对象，避免调用方判空
	private AudioTranscriptionMetadata transcriptionMetadata = AudioTranscriptionMetadata.NULL;

	// 构造器：仅需文本，元数据保持默认的 NULL 空对象
	public AudioTranscription(String text) {
		this.text = text;
	}

	@Override
	// 返回模型输出的转写文本
	public String getOutput() {
		return this.text;
	}

	@Override
	// 返回结果元数据，因有默认空对象，保证非 null
	public AudioTranscriptionMetadata getMetadata() {
		return this.transcriptionMetadata;
	}

	// 以 “withXxx” 风格设置元数据并返回 this，支持链式调用；
	// 注意这里是就地修改当前对象（可变），并非创建新副本
	public AudioTranscription withTranscriptionMetadata(AudioTranscriptionMetadata transcriptionMetadata) {
		this.transcriptionMetadata = transcriptionMetadata;
		return this;
	}

	@Override
	// 值相等语义：文本与元数据同时相等才认为两个转写结果相同
	public boolean equals(@Nullable Object o) {
		// 同一引用直接判定相等，快速路径
		if (this == o) {
			return true;
		}
		// instanceof 模式匹配：类型不符（含 null）直接返回 false，同时完成向下转型
		if (!(o instanceof AudioTranscription that)) {
			return false;
		}
		return Objects.equals(this.text, that.text)
				&& Objects.equals(this.transcriptionMetadata, that.transcriptionMetadata);
	}

	@Override
	// 与 equals 保持一致，使用相同的两个字段计算哈希
	public int hashCode() {
		return Objects.hash(this.text, this.transcriptionMetadata);
	}

	@Override
	// 调试输出；注意前缀是 "Transcript" 而非类名
	public String toString() {
		return "Transcript{" + "text=" + this.text + ", transcriptionMetadata=" + this.transcriptionMetadata + '}';
	}

}
