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

package org.springframework.ai.image;

import org.springframework.ai.model.ResultMetadata;

/**
 * 单张生成图片的结果元数据接口。
 * <p>
 * 本接口是一个「标记式」扩展点：自身不声明任何方法，仅继承 {@link ResultMetadata}，
 * 用于在类型系统上把「图像结果的元数据」与其它模态（对话、嵌入等）的元数据区分开。
 * <p>
 * 各模型厂商可实现本接口以携带自有信息，例如 OpenAI DALL·E 会返回被模型改写后的
 * 提示词（revised prompt）。当没有元数据时，{@link ImageGeneration} 会使用一个空实现占位。
 */
public interface ImageGenerationMetadata extends ResultMetadata {

}
