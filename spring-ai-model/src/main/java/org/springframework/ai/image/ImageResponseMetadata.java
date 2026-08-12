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

import org.springframework.ai.model.MutableResponseMetadata;

/**
 * Represents metadata associated with an image response. It provides additional
 * information about the generative response from an AI model, including the creation
 * timestamp of the generated image.
 *
 * @author Mark Pollack
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 图像响应的元数据（响应级，区别于单张图片的 {@link ImageGenerationMetadata}）。
 * <p>
 * 继承自 {@link MutableResponseMetadata}，因此除了固定的 {@code created} 字段外，
 * 还可以像 Map 一样通过 {@code put/get} 存放各厂商返回的任意扩展字段。
 * <p>
 * 关键字段：{@code created} —— 图片生成时间戳（毫秒）。无参构造时取当前系统时间，
 * 各厂商实现通常改用服务端返回的 created 值。
 * <p>
 * 典型用法：{@code imageResponse.getMetadata().getCreated()}
 *
 * @since 1.0.0
 */
public class ImageResponseMetadata extends MutableResponseMetadata {

	// 生成时间戳（毫秒），构造后不可变
	private final Long created;

	// 无参构造：默认以「当前系统时间」作为创建时间，用于服务端未返回时间的场景
	public ImageResponseMetadata() {
		this(System.currentTimeMillis());
	}

	// 全参构造：由调用方（通常是各厂商适配层）传入服务端返回的时间戳
	public ImageResponseMetadata(Long created) {
		this.created = created;
	}

	// 获取生成时间戳（毫秒）
	public Long getCreated() {
		return this.created;
	}

}
