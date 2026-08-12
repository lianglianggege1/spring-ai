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

import org.springframework.ai.model.ResultMetadata;

/**
 * Metadata associated with an audio transcription result.
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
/**
 * 【中文说明】单条音频转写结果所关联的元数据接口。
 *
 * <p>
 * 用途：继承自 {@link ResultMetadata}，用于描述“某一条转写结果”的附加信息（如置信度、时间戳等，
 * 由各厂商实现自行扩展）。接口本身不声明任何方法，属于标记式/可扩展的元数据契约。
 *
 * <p>
 * 关键成员：
 * <ul>
 * <li>{@link #NULL}：共享的空元数据常量，采用“空对象模式（Null Object Pattern）”，
 * 让 {@code AudioTranscription#getMetadata()} 在没有元数据时也能返回非 null 值。</li>
 * <li>{@link #create()}：静态工厂方法，返回一个空的匿名实现。</li>
 * </ul>
 *
 * <p>
 * 典型用法：厂商模块实现本接口并携带自己的元数据字段；若无元数据则直接复用 {@code NULL}。
 *
 * @author Michael Lavelle
 * @author Piotr Olaszewski
 * @since 0.8.1
 */
public interface AudioTranscriptionMetadata extends ResultMetadata {

	// 空对象常量：接口字段隐式为 public static final，全局共享同一个实例，
	// 用于替代 null，避免调用方到处判空
	AudioTranscriptionMetadata NULL = AudioTranscriptionMetadata.create();

	/**
	 * Factory method used to construct a new {@link AudioTranscriptionMetadata}
	 * @return a new {@link AudioTranscriptionMetadata}
	 */
	// 静态工厂：由于接口无抽象方法，直接返回一个空的匿名实现类实例
	static AudioTranscriptionMetadata create() {
		return new AudioTranscriptionMetadata() {

		};
	}

}
