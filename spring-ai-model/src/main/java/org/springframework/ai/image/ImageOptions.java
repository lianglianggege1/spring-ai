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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelOptions;

/**
 * ImageOptions represent the common options, portable across different image generation
 * models.
 */
/**
 * 图像生成的「可移植」通用选项接口。
 * <p>
 * 这里只抽象出各家图像模型都普遍支持的公共参数；厂商特有参数（如 DALL·E 的 quality、
 * Stability 的 cfgScale/steps）由各自的子接口/实现类扩展。所有 getter 均可返回 null，
 * 表示「不指定该参数」，此时使用模型服务端的默认值。
 * <p>
 * 关键方法一览：
 * <ul>
 * <li>{@link #getN()} —— 一次请求生成几张图。</li>
 * <li>{@link #getModel()} —— 使用的模型名称/版本。</li>
 * <li>{@link #getWidth()} / {@link #getHeight()} —— 输出图片的宽高（像素）。</li>
 * <li>{@link #getResponseFormat()} —— 返回格式，常见取值为 {@code url} 或 {@code b64_json}，
 * 它决定了最终 {@link Image} 中被填充的是 url 还是 b64Json 字段。</li>
 * <li>{@link #getStyle()} —— 画面风格（如 natural、vivid）。</li>
 * </ul>
 * 通用实现见 {@link ImageOptionsBuilder} 构建出的默认对象。
 */
public interface ImageOptions extends ModelOptions {

	// 生成图片的数量；null 表示交由模型默认值决定（通常为 1）
	@Nullable Integer getN();

	// 使用的模型名称/版本，例如 "dall-e-3"；null 表示使用实现类配置的默认模型
	@Nullable String getModel();

	// 输出图片宽度（像素）；需与模型支持的尺寸组合匹配，否则服务端会报错
	@Nullable Integer getWidth();

	// 输出图片高度（像素）；通常必须与 width 组成模型允许的固定尺寸
	@Nullable Integer getHeight();

	// 返回格式：url（返回链接）或 b64_json（返回 Base64 内容）
	@Nullable String getResponseFormat();

	// 画面风格，取值由具体模型定义，如 "vivid"、"natural"
	@Nullable String getStyle();

}
