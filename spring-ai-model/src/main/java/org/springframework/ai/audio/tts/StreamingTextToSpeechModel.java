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

import reactor.core.publisher.Flux;

import org.springframework.ai.model.StreamingModel;

/**
 * Interface for the streaming text to speech model.
 *
 * @author Alexandros Pappas
 */
/**
 * 流式文本转语音（TTS）模型接口。
 * <p>
 * 与一次性返回完整音频的 {@link TextToSpeechModel#call} 不同，本接口基于 Reactor 的
 * {@link Flux} 分块返回音频数据：服务端合成一段就推送一段，可显著降低首帧延迟，
 * 适合长文本朗读、实时播报等场景。调用方可边接收边写入播放器或输出流。
 * <p>
 * 标注了 {@code @FunctionalInterface}：抽象方法只有 {@link #stream(TextToSpeechPrompt)} 一个，
 * 其余均为提供便捷重载的 default 方法，因此可用 Lambda 实现。
 * <p>
 * 典型用法：{@code ttsModel.stream("很长的一段文本").subscribe(chunk -> out.write(chunk));}
 */
@FunctionalInterface
public interface StreamingTextToSpeechModel extends StreamingModel<TextToSpeechPrompt, TextToSpeechResponse> {

	// 便捷默认方法：传入文本，直接得到音频字节块的流（使用模型默认选项）
	default Flux<byte[]> stream(String text) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
		// map 把每个响应块拆出音频字节；空值处理：结果或音频为 null 时以空数组代替，
		// 保证流不会因个别空块而向下游传递 null（Reactor 不允许 null 元素）
		return stream(prompt).map(response -> (response.getResult() == null || response.getResult().getOutput() == null)
				? new byte[0] : response.getResult().getOutput());
	}

	// 便捷默认方法的重载：在上一个方法基础上额外指定合成选项（音色、格式、语速等）
	default Flux<byte[]> stream(String text, TextToSpeechOptions options) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);
		// 同样做空值兜底，避免向流中发射 null
		return stream(prompt).map(response -> (response.getResult() == null || response.getResult().getOutput() == null)
				? new byte[0] : response.getResult().getOutput());
	}

	// 唯一抽象方法：流式调用 TTS 模型，由各厂商实现
	@Override
	Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt);

}
