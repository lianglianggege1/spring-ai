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

import org.springframework.ai.model.ModelOptions;

/**
 * Options for audio transcription.
 *
 * @author Piotr Olaszewski
 */
/**
 * 【中文说明】音频转写请求的可移植配置项接口。
 *
 * <p>
 * 用途：继承自 {@link ModelOptions}，定义各厂商转写模型共有的最小配置集合。它会被封装进
 * {@code AudioTranscriptionPrompt} 一起传给模型，用于在“运行时”覆盖客户端默认配置。
 *
 * <p>
 * 关键方法：{@link #getModel()} 返回要使用的模型名称（如 whisper-1）。各厂商实现通常在此基础上扩展
 * 语言、温度、响应格式、时间戳粒度等专有选项。
 *
 * <p>
 * 典型用法：调用方构造厂商专属的 Options 实现，传入 {@code new AudioTranscriptionPrompt(resource,
 * options)}。
 *
 * @author Piotr Olaszewski
 */
public interface AudioTranscriptionOptions extends ModelOptions {

	// 返回本次转写要使用的模型标识（模型名/部署名）
	String getModel();

}
