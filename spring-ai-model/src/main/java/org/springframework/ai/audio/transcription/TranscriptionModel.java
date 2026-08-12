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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.Model;
import org.springframework.core.io.Resource;

/**
 * A transcription model is a type of AI model that converts audio to text. This is also
 * known as Speech-to-Text.
 *
 * @author Mudabir Hussain
 * @since 1.0.0
 */
/**
 * 【中文说明】语音转文字（Speech-to-Text）模型的统一抽象接口。
 *
 * <p>
 * 用途：继承 {@link Model Model&lt;AudioTranscriptionPrompt, AudioTranscriptionResponse&gt;}，
 * 为各厂商（如 OpenAI Whisper、Azure、阿里云等）的转写能力提供统一的调用入口，使上层代码与具体厂商解耦。
 *
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #call(AudioTranscriptionPrompt)}：核心抽象方法，唯一需要厂商实现的方法，入参为完整
 * Prompt，返回完整响应（含元数据）。</li>
 * <li>{@link #transcribe(Resource)} / {@link #transcribe(Resource, AudioTranscriptionOptions)}：
 * 两个 default 便捷方法，属于“模板方法”式设计——它们内部帮你组装 Prompt 并抽取纯文本，
 * 实现类无需重写即可获得这两个能力。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * String text = transcriptionModel.transcribe(new FileSystemResource("a.mp3"));
 * }</pre>
 *
 * @author Mudabir Hussain
 * @since 1.0.0
 */
public interface TranscriptionModel extends Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> {

	/**
	 * Transcribes the audio from the given prompt.
	 * @param transcriptionPrompt The prompt containing the audio resource and options.
	 * @return The transcription response.
	 */
	// 唯一的抽象方法：由各厂商实现真正的转写调用逻辑
	AudioTranscriptionResponse call(AudioTranscriptionPrompt transcriptionPrompt);

	/**
	 * A convenience method for transcribing an audio resource.
	 * @param resource The audio resource to transcribe.
	 * @return The transcribed text.
	 */
	// 便捷重载：不带选项，直接委托给下面的两参重载并传入 null（表示使用默认配置）
	default String transcribe(Resource resource) {
		return this.transcribe(resource, null);
	}

	/**
	 * A convenience method for transcribing an audio resource with the given options.
	 * @param resource The audio resource to transcribe.
	 * @param options The transcription options.
	 * @return The transcribed text.
	 */
	// 便捷方法：封装“组装 Prompt -> 调用 call -> 抽取文本”三步，直接返回纯文本
	default String transcribe(Resource resource, @Nullable AudioTranscriptionOptions options) {
		// 将资源与选项打包成标准请求对象
		AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
		AudioTranscription result = this.call(prompt).getResult();
		// 空值保护：某些实现可能返回空结果，此时降级为空字符串而不是抛 NPE
		return result != null ? result.getOutput() : "";
	}

}
