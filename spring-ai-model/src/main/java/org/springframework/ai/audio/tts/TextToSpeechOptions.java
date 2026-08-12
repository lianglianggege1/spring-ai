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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelOptions;

/**
 * Interface for text-to-speech model options. Defines the common, portable options that
 * should be supported by all implementations.
 *
 * @author Alexandros Pappas
 */
/**
 * 文本转语音（TTS）的「可移植」通用选项接口。
 * <p>
 * 只抽象各家 TTS 模型都普遍支持的公共参数，厂商特有参数由各自实现类扩展。
 * 所有 getter 均可返回 null，语义为「不指定」，此时使用服务端默认值。
 * <p>
 * 关键方法：
 * <ul>
 * <li>{@link #getModel()} —— 模型名称，如 tts-1、tts-1-hd。</li>
 * <li>{@link #getVoice()} —— 音色标识，如 alloy、nova。</li>
 * <li>{@link #getFormat()} —— 音频输出格式，如 mp3、wav、opus。</li>
 * <li>{@link #getSpeed()} —— 语速倍率，1.0 为正常语速。</li>
 * </ul>
 * 本接口内嵌 {@link Builder} 子接口，并通过静态方法 {@link #builder()} 暴露默认实现，
 * 这样调用方只依赖接口即可构建选项对象，无需接触 {@code DefaultTextToSpeechOptions}。
 * <p>
 * 典型用法：
 * <pre>{@code
 * TextToSpeechOptions options = TextToSpeechOptions.builder()
 *         .model("tts-1").voice("alloy").format("mp3").speed(1.0).build();
 * }</pre>
 */
public interface TextToSpeechOptions extends ModelOptions {

	/**
	 * Creates a new {@link TextToSpeechOptions.Builder} to create the default
	 * {@link TextToSpeechOptions}.
	 * @return Returns a new {@link TextToSpeechOptions.Builder}.
	 */
	// 接口静态工厂方法：对外只暴露 Builder 接口，内部指向默认实现，隐藏具体实现类
	static TextToSpeechOptions.Builder builder() {
		return new DefaultTextToSpeechOptions.Builder();
	}

	/**
	 * Returns the model to use for text-to-speech.
	 * @return The model name.
	 */
	// 获取模型名称；null 表示使用实现类配置的默认模型
	@Nullable String getModel();

	/**
	 * Returns the voice to use for text-to-speech.
	 * @return The voice identifier.
	 */
	// 获取音色标识；不同厂商取值不同，如 OpenAI 的 alloy/echo/nova
	@Nullable String getVoice();

	/**
	 * Returns the output format for the generated audio.
	 * @return The output format (e.g., "mp3", "wav").
	 */
	// 获取音频输出格式，如 mp3、wav、opus
	@Nullable String getFormat();

	/**
	 * Returns the speed of the generated speech.
	 * @return The speech speed.
	 */
	// 获取语速倍率，1.0 为正常语速；有效范围由具体模型限定
	@Nullable Double getSpeed();

	/**
	 * Builder for {@link TextToSpeechOptions}.
	 */
	/**
	 * {@link TextToSpeechOptions} 的构建器接口。
	 * <p>
	 * 定义为接口而非具体类，使各厂商可提供自己的构建器实现（并扩展专有参数），
	 * 同时上层代码始终面向该接口编程，保持可移植性。所有方法返回 Builder 以支持链式调用。
	 */
	interface Builder {

		/**
		 * Sets the model to use for text-to-speech.
		 * @param model The model name.
		 * @return This builder.
		 */
		// 设置模型名称，链式返回自身
		Builder model(String model);

		/**
		 * Sets the voice to use for text-to-speech.
		 * @param voice The voice identifier.
		 * @return This builder.
		 */
		// 设置音色标识，链式返回自身
		Builder voice(String voice);

		/**
		 * Sets the output format for the generated audio.
		 * @param format The output format (e.g., "mp3", "wav").
		 * @return This builder.
		 */
		// 设置音频输出格式，链式返回自身
		Builder format(String format);

		/**
		 * Sets the speed of the generated speech.
		 * @param speed The speech speed.
		 * @return This builder.
		 */
		// 设置语速倍率，链式返回自身
		Builder speed(Double speed);

		/**
		 * Builds the {@link TextToSpeechOptions}.
		 * @return The {@link TextToSpeechOptions}.
		 */
		// 完成构建，返回不可变的选项对象
		TextToSpeechOptions build();

	}

}
