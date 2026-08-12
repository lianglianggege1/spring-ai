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

import org.springframework.ai.model.MutableResponseMetadata;

/**
 * Metadata associated with an audio transcription response.
 *
 * @author Piotr Olaszewski
 */
/**
 * 【中文说明】音频转写响应的元数据容器。
 *
 * <p>
 * 用途：直接继承 {@link MutableResponseMetadata}，自身不新增任何字段或方法，仅通过“具名子类”提供
 * 转写场景专属的类型，使 {@code AudioTranscriptionResponse#getMetadata()} 能返回更精确的类型。
 *
 * <p>
 * 关键特性：父类是可变（Mutable）的键值映射，各厂商实现可在调用后通过 {@code put(key, value)}
 * 塞入限流、请求 ID、原始响应等信息，读取时用 {@code get(key)}。
 *
 * <p>
 * 典型用法：{@code response.getMetadata().get("rate-limit")}
 *
 * @author Piotr Olaszewski
 */
public class AudioTranscriptionResponseMetadata extends MutableResponseMetadata {

}
