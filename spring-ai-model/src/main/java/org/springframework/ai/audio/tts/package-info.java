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

/**
 * 文本转语音（Text-To-Speech，TTS）模块的核心抽象包。
 * <p>
 * 主要类型：
 * <ul>
 * <li>{@link org.springframework.ai.audio.tts.TextToSpeechModel} —— 同步 TTS 模型接口，
 * 同时继承 {@link org.springframework.ai.audio.tts.StreamingTextToSpeechModel} 具备流式能力。</li>
 * <li>{@link org.springframework.ai.audio.tts.TextToSpeechPrompt} /
 * {@link org.springframework.ai.audio.tts.TextToSpeechMessage} —— 请求与待朗读文本。</li>
 * <li>{@link org.springframework.ai.audio.tts.TextToSpeechOptions} 及其默认实现
 * {@link org.springframework.ai.audio.tts.DefaultTextToSpeechOptions} —— 可移植的合成选项。</li>
 * <li>{@link org.springframework.ai.audio.tts.TextToSpeechResponse} /
 * {@link org.springframework.ai.audio.tts.Speech} —— 响应与音频字节结果。</li>
 * </ul>
 * 整体调用链：TextToSpeechPrompt → TextToSpeechModel.call()/stream() → TextToSpeechResponse → Speech → byte[]。
 * <p>
 * 包上的 {@code @NullMarked}（JSpecify）表示本包内类型默认非空，仅显式标注 {@code @Nullable} 处可为 null。
 */
@NullMarked
package org.springframework.ai.audio.tts;

import org.jspecify.annotations.NullMarked;
