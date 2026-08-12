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

import java.util.Arrays;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

/**
 * Implementation of the {@link ModelResult} interface for the speech model.
 *
 * @author Alexandros Pappas
 */
/**
 * 单条语音合成结果，是 {@link ModelResult} 在 TTS 场景下的实现。
 * <p>
 * 关键字段：{@code speech} —— 合成出的音频二进制数据，其编码格式取决于请求时
 * {@link TextToSpeechOptions#getFormat()} 指定的格式（mp3、wav、opus 等）。
 * <p>
 * 注意：本类直接持有 byte 数组引用，未做防御性拷贝，因此严格来说并非完全不可变；
 * 使用时不应在外部修改传入或取出的数组内容。
 * <p>
 * 典型用法：{@code byte[] audio = response.getResult().getOutput();} 之后写入文件或直接播放。
 */
public class Speech implements ModelResult<byte[]> {

	// 合成后的音频字节数据，格式由请求选项中的 format 决定
	private final byte[] speech;

	// 构造方法：直接持有传入的字节数组引用（未做拷贝）
	public Speech(byte[] speech) {
		this.speech = speech;
	}

	// 返回音频字节数据，对应 ModelResult 契约中的「输出」
	@Override
	public byte[] getOutput() {
		return this.speech;
	}

	// 值相等语义：比较音频内容本身
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof Speech speech1)) {
			return false;
		}
		// 数组必须用 Arrays.equals 逐元素比较；若用 Objects.equals 只会比较引用地址，导致内容相同也不相等
		return Arrays.equals(this.speech, speech1.speech);
	}

	// 与 equals 保持一致：先用 Arrays.hashCode 计算数组内容哈希，而非数组的身份哈希
	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(this.speech));
	}

	@Override
	public String toString() {
		return "Speech{" + "speech=" + Arrays.toString(this.speech) + '}';
	}

	// 返回结果元数据。TTS 场景暂无可用元数据，
	// 这里用匿名空实现（空对象模式）代替返回 null，避免调用方判空
	@Override
	public ResultMetadata getMetadata() {
		return new ResultMetadata() {
		};
	}

}
