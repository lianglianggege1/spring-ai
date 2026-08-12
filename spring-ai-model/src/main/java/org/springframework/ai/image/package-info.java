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
 * 图像生成（文生图）模块的核心抽象包。
 * <p>
 * 主要类型：
 * <ul>
 * <li>{@link org.springframework.ai.image.ImageModel} —— 图像模型统一接口，入口方法 call。</li>
 * <li>{@link org.springframework.ai.image.ImagePrompt} /
 * {@link org.springframework.ai.image.ImageMessage} —— 请求与提示词消息。</li>
 * <li>{@link org.springframework.ai.image.ImageOptions} /
 * {@link org.springframework.ai.image.ImageOptionsBuilder} —— 可移植的生成选项及其构建器。</li>
 * <li>{@link org.springframework.ai.image.ImageResponse} /
 * {@link org.springframework.ai.image.ImageGeneration} /
 * {@link org.springframework.ai.image.Image} —— 响应、单条结果与图片本体。</li>
 * <li>各类 Metadata —— 响应级与结果级元数据。</li>
 * </ul>
 * 整体调用链：ImagePrompt → ImageModel.call() → ImageResponse → ImageGeneration → Image。
 * <p>
 * 包上标注的 {@code @NullMarked}（JSpecify）表示：本包内所有类型默认「非空」，
 * 只有显式标注 {@code @Nullable} 的地方才允许为 null，便于静态分析工具做空指针检查。
 */
@NullMarked
package org.springframework.ai.image;

import org.jspecify.annotations.NullMarked;
