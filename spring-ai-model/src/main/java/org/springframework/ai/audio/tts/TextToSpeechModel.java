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

import org.springframework.ai.model.Model;
import org.springframework.ai.model.ModelResult;

/**
 * Interface for the text to speech model.
 *
 * @author Alexandros Pappas
 * @author Sebastien Deleuze
 */
/**
 * 文本转语音（TTS）模型的统一抽象接口。
 * <p>
 * 它同时继承两个接口：
 * <ul>
 * <li>{@link Model}{@code <TextToSpeechPrompt, TextToSpeechResponse>} —— 提供同步调用能力。</li>
 * <li>{@link StreamingTextToSpeechModel} —— 提供流式（边合成边返回音频块）调用能力。</li>
 * </ul>
 * 因此一个实现类天然同时具备同步与流式两种调用方式，调用方按场景选择：
 * 短文本用 {@code call}，长文本或需低延迟播放用 {@code stream}。
 * <p>
 * 典型用法：{@code byte[] audio = ttsModel.call("你好，世界");}
 */
public interface TextToSpeechModel extends Model<TextToSpeechPrompt, TextToSpeechResponse>, StreamingTextToSpeechModel {

	// 便捷默认方法：直接传文本、直接拿音频字节，省去手动构造 Prompt 与逐层取值
	default byte[] call(String text) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
		ModelResult<byte[]> result = call(prompt).getResult();
		// 空值处理：无任何结果时返回空数组而非 null，避免调用方 NPE
		if (result == null) {
			return new byte[0];
		}
		byte[] output = result.getOutput();
		// 二次兜底：结果存在但音频为空时同样返回空数组
		return (output != null) ? output : new byte[0];
	}

	// 核心抽象方法：同步调用 TTS 模型，由各厂商实现
	@Override
	TextToSpeechResponse call(TextToSpeechPrompt prompt);

	/**
	 * Gets the options for this model.
	 * @return the options
	 * @since 2.0.0
	 */
	// 获取该模型的默认选项。默认实现返回一份「全空」配置，
	// 各厂商实现通常覆写此方法以返回自己配置的默认模型名、音色等
	default TextToSpeechOptions getOptions() {
		return TextToSpeechOptions.builder().build();
	}

	/**
	 * @deprecated use {@link #getOptions()} instead.
	 */
	// 已废弃的旧方法名，仅为兼容保留，内部直接委托给 getOptions()；
	// forRemoval = true 表示未来版本会移除，新代码请勿使用
	@Deprecated(forRemoval = true)
	default TextToSpeechOptions getDefaultOptions() {
		return getOptions();
	}

}
