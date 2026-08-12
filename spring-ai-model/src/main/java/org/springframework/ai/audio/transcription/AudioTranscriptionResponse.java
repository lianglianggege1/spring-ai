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

import java.util.List;

import org.springframework.ai.model.ModelResponse;
import org.springframework.util.Assert;

/**
 * A response containing an audio transcription result.
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
/**
 * 【中文说明】音频转写的响应对象，包装模型返回的转写结果与响应级元数据。
 *
 * <p>
 * 用途：实现 {@link ModelResponse ModelResponse&lt;AudioTranscription&gt;}。与 Chat 场景可能返回多个
 * generation 不同，转写场景只有一条结果，因此内部只持有单个 {@link AudioTranscription}，
 * {@link #getResults()} 用 {@code List.of(...)} 包装成单元素不可变列表以满足接口契约。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code transcript}：唯一的转写结果，final 且构造时强制非空。</li>
 * <li>{@code transcriptionResponseMetadata}：响应级元数据（如限流信息、原始响应等），final 且非空。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code String text = response.getResult().getOutput();}
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
public class AudioTranscriptionResponse implements ModelResponse<AudioTranscription> {

	// 唯一的转写结果，构造后不可变
	private final AudioTranscription transcript;

	// 响应级元数据（区别于结果级的 AudioTranscriptionMetadata）
	private final AudioTranscriptionResponseMetadata transcriptionResponseMetadata;

	// 便捷构造器：委托给全参构造器，元数据使用一个新建的空实例（而非共享单例，避免可变状态被串用）
	public AudioTranscriptionResponse(AudioTranscription transcript) {
		this(transcript, new AudioTranscriptionResponseMetadata());
	}

	// 全参构造器：两个参数都做非空断言，保证响应对象内部状态始终有效
	public AudioTranscriptionResponse(AudioTranscription transcript,
			AudioTranscriptionResponseMetadata transcriptionResponseMetadata) {
		// 参数校验：结果不允许为 null，违反则抛 IllegalArgumentException（快速失败）
		Assert.notNull(transcript, "AudioTranscription must not be null");
		// 参数校验：元数据同样不允许为 null
		Assert.notNull(transcriptionResponseMetadata, "AudioTranscriptionResponseMetadata must not be null");
		this.transcript = transcript;
		this.transcriptionResponseMetadata = transcriptionResponseMetadata;
	}

	@Override
	// 返回唯一的转写结果，保证非 null
	public AudioTranscription getResult() {
		return this.transcript;
	}

	@Override
	// 为兼容 ModelResponse 的“多结果”契约，将单个结果包装为不可变单元素列表
	public List<AudioTranscription> getResults() {
		return List.of(this.transcript);
	}

	@Override
	// 返回响应级元数据
	public AudioTranscriptionResponseMetadata getMetadata() {
		return this.transcriptionResponseMetadata;
	}

}
