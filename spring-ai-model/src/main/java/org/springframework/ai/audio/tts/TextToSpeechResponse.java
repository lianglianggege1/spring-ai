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

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResponse;

/**
 * Implementation of the {@link ModelResponse} interface for the text to speech response.
 *
 * @author Alexandros Pappas
 */
/**
 * 语音合成响应对象，是 {@link ModelResponse} 在 TTS 场景下的实现。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code results} —— {@link Speech} 结果列表。同步调用时通常只有一个元素；
 * 流式调用时每个响应块各自携带一段音频。</li>
 * <li>{@code textToSpeechResponseMetadata} —— 响应级元数据，永不为 null。</li>
 * </ul>
 * 注意：与 {@code ImageResponse} 不同，本类未对传入列表做防御性拷贝，
 * 且 {@link #getResult()} 在列表为空时会抛出越界异常，而非返回 null。
 * <p>
 * 典型用法：{@code byte[] audio = response.getResult().getOutput();}
 */
public class TextToSpeechResponse implements ModelResponse<Speech> {

	// 合成结果列表；流式场景下每个响应块携带一段音频
	private final List<Speech> results;

	// 响应级元数据，永不为 null
	private final TextToSpeechResponseMetadata textToSpeechResponseMetadata;

	// 便捷构造：不带元数据时自动填入空的元数据对象，保证 getMetadata() 非空
	public TextToSpeechResponse(List<Speech> results) {
		this(results, new TextToSpeechResponseMetadata());
	}

	// 全参构造：直接持有传入列表的引用（未做防御性拷贝）
	public TextToSpeechResponse(List<Speech> results, TextToSpeechResponseMetadata textToSpeechResponseMetadata) {
		this.results = results;
		this.textToSpeechResponseMetadata = textToSpeechResponseMetadata;
	}

	// 返回全部合成结果
	@Override
	public List<Speech> getResults() {
		return this.results;
	}

	// 便捷方法：取第一条结果。
	// 注意此处未做空列表判断，results 为空时会抛 IndexOutOfBoundsException
	public Speech getResult() {
		return this.results.get(0);
	}

	// 返回响应级元数据
	@Override
	public TextToSpeechResponseMetadata getMetadata() {
		return this.textToSpeechResponseMetadata;
	}

	// 值相等语义：仅比较结果列表，刻意不参与比较元数据
	// （元数据含时间戳等易变信息，纳入比较会导致内容相同的响应被判为不等）
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof TextToSpeechResponse that)) {
			return false;
		}
		return Objects.equals(this.results, that.results);
	}

	// 与 equals 保持一致：同样只基于 results 计算哈希
	@Override
	public int hashCode() {
		return Objects.hash(this.results);
	}

	@Override
	public String toString() {
		return "TextToSpeechResponse{" + "results=" + this.results + '}';
	}

}
