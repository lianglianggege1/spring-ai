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

import org.springframework.ai.model.ModelRequest;
import org.springframework.core.io.Resource;

/**
 * Represents an audio transcription prompt for an AI model. It implements the
 * {@link ModelRequest} interface and provides the necessary information required to
 * interact with an AI model, including the audio resource and model options.
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
/**
 * 【中文说明】音频转写请求对象（Prompt），是调用转写模型的输入载体。
 *
 * <p>
 * 用途：实现 {@link ModelRequest ModelRequest&lt;Resource&gt;}，把“待识别的音频资源”与“模型选项”打包成
 * 一次模型调用所需的完整请求。注意其泛型指令类型是 {@link Resource}（二进制音频），而不是文本字符串。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code audioResource}：音频文件资源，final 不可变；支持 mp3、mp4、mpeg、mpga、m4a、wav、webm 等格式。</li>
 * <li>{@code modelOptions}：可选的转写配置，标注了 {@link Nullable}，允许为 null，此时使用客户端默认配置。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * var prompt = new AudioTranscriptionPrompt(new ClassPathResource("speech.mp3"), options);
 * AudioTranscriptionResponse resp = transcriptionModel.call(prompt);
 * }</pre>
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
public class AudioTranscriptionPrompt implements ModelRequest<Resource> {

	// 待转写的音频资源（可来自 classpath、文件系统、URL 等），构造后不可变
	private final Resource audioResource;

	// 可选的模型配置；为 null 时表示沿用客户端/厂商的默认选项
	private @Nullable AudioTranscriptionOptions modelOptions;

	/**
	 * Construct a new AudioTranscriptionPrompt given the resource representing the audio
	 * file. The following input file types are supported: mp3, mp4, mpeg, mpga, m4a, wav,
	 * and webm.
	 * @param audioResource resource of the audio file.
	 */
	// 便捷构造器：只传音频资源，modelOptions 保持 null（走默认配置）
	public AudioTranscriptionPrompt(Resource audioResource) {
		this.audioResource = audioResource;
	}

	/**
	 * Construct a new AudioTranscriptionPrompt given the resource representing the audio
	 * file. The following input file types are supported: mp3, mp4, mpeg, mpga, m4a, wav,
	 * and webm.
	 * @param audioResource resource of the audio file.
	 * @param modelOptions
	 */
	// 完整构造器：音频资源 + 运行时选项；此处不做非空校验，选项允许为 null
	public AudioTranscriptionPrompt(Resource audioResource, @Nullable AudioTranscriptionOptions modelOptions) {
		this.audioResource = audioResource;
		this.modelOptions = modelOptions;
	}

	@Override
	// ModelRequest 契约方法：这里的“指令”就是音频资源本身
	public Resource getInstructions() {
		return this.audioResource;
	}

	@Override
	// 返回本次请求的选项，可能为 null，调用方需自行处理默认值合并
	public @Nullable AudioTranscriptionOptions getOptions() {
		return this.modelOptions;
	}

}
