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
 * 【中文说明】音频转写（语音转文字，Speech-to-Text）抽象包。
 *
 * <p>
 * 本包定义了与厂商无关的转写模型契约，核心类型包括：
 * <ul>
 * <li>{@code TranscriptionModel}：转写模型接口，统一调用入口。</li>
 * <li>{@code AudioTranscriptionPrompt}：请求对象，封装音频 {@code Resource} 与选项。</li>
 * <li>{@code AudioTranscriptionOptions}：可移植的运行时配置（模型名等）。</li>
 * <li>{@code AudioTranscriptionResponse} / {@code AudioTranscription}：响应与单条结果。</li>
 * <li>{@code AudioTranscriptionResponseMetadata} / {@code AudioTranscriptionMetadata}：
 * 响应级与结果级元数据。</li>
 * </ul>
 *
 * <p>
 * 包上的 {@link org.jspecify.annotations.NullMarked @NullMarked} 表示：本包内所有类型默认不可为 null，
 * 只有显式标注 {@code @Nullable} 的位置才允许 null，便于静态分析工具做空安全检查。
 */
@NullMarked
package org.springframework.ai.audio.transcription;

import org.jspecify.annotations.NullMarked;
