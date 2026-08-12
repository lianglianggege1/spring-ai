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

import org.springframework.ai.model.ModelResult;

/**
 * 单条图像生成结果。是 {@link ModelResult} 在图像场景下的实现，
 * 一次图像请求若生成 N 张图，则 {@link ImageResponse} 中会包含 N 个本对象。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code image} —— 生成结果本体（URL 或 Base64）。</li>
 * <li>{@code imageGenerationMetadata} —— 该张图片的附加元数据（如修订后的提示词等，由各厂商扩展）。</li>
 * </ul>
 * 对象不可变（字段均为 final），线程安全。
 * <p>
 * 典型用法：{@code Image img = imageResponse.getResult().getOutput();}
 */
public class ImageGeneration implements ModelResult<Image> {

	// 空对象模式：作为无元数据时的默认值，避免 getMetadata() 返回 null 而使调用方被迫判空
	private static final ImageGenerationMetadata NONE = new ImageGenerationMetadata() {

	};

	// 本张图片对应的元数据，不会为 null（缺省为上面的 NONE 空实现）
	private final ImageGenerationMetadata imageGenerationMetadata;

	// 生成出来的图片本体
	private final Image image;

	// 便捷构造方法：不带元数据时，统一委托给全参构造并填入 NONE 空实现
	public ImageGeneration(Image image) {
		this(image, NONE);
	}

	// 全参构造方法：同时指定图片本体与其元数据
	public ImageGeneration(Image image, ImageGenerationMetadata imageGenerationMetadata) {
		this.image = image;
		this.imageGenerationMetadata = imageGenerationMetadata;
	}

	// 返回模型输出的图片本体，对应 ModelResult 契约中的「输出」
	@Override
	public Image getOutput() {
		return this.image;
	}

	// 返回该结果的元数据，永不为 null
	@Override
	public ImageGenerationMetadata getMetadata() {
		return this.imageGenerationMetadata;
	}

	@Override
	public String toString() {
		return "ImageGeneration{" + "imageGenerationMetadata=" + this.imageGenerationMetadata + ", image=" + this.image
				+ '}';
	}

}
