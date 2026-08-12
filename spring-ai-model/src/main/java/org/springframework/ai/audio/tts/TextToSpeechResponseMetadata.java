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

import org.springframework.ai.model.MutableResponseMetadata;

/**
 * Metadata associated with an audio transcription response.
 *
 * @author Alexandros Pappas
 */
/**
 * 语音合成响应的元数据。
 * <p>
 * 本类自身不新增任何字段，完全复用父类 {@link MutableResponseMetadata} 的能力：
 * 后者提供类似 Map 的可变键值存储（{@code put}/{@code get}），
 * 供各厂商实现按需塞入自有信息（如请求 ID、音频时长、限流配额等）。
 * <p>
 * 之所以仍单独定义一个空类，是为了在类型层面标识「这是 TTS 的响应元数据」，
 * 便于泛型约束与后续扩展（将来新增固定字段时无需改动调用方签名）。
 * <p>
 * 典型用法：{@code response.getMetadata().get("request-id")}
 */
public class TextToSpeechResponseMetadata extends MutableResponseMetadata {

}
