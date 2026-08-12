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

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResponse;
import org.springframework.util.CollectionUtils;

/**
 * The image completion (e.g. imageGeneration) response returned by an AI provider.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Hyunjoon Choi
 */
/**
 * 图像生成响应对象，封装 AI 服务商返回的全部生成结果。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code imageGenerations} —— 生成结果列表。因为一次请求可通过
 * {@link ImageOptions#getN()} 要求生成多张图，所以这里是列表而非单值。</li>
 * <li>{@code imageResponseMetadata} —— 本次调用的响应级元数据（如创建时间、厂商原始字段）。</li>
 * </ul>
 * 该对象不可变：字段为 final，且构造时用 {@code List.copyOf} 做了防御性拷贝。
 * <p>
 * 典型用法：
 * <pre>{@code
 * ImageResponse resp = imageModel.call(new ImagePrompt("一只柴犬"));
 * String url = resp.getResult().getOutput().getUrl(); // 取第一张
 * }</pre>
 * 注意 {@link #getResult()} 在无结果时返回 null，使用前需判空。
 */
public class ImageResponse implements ModelResponse<ImageGeneration> {

	// 响应级元数据，永不为 null（无参构造时使用空的 ImageResponseMetadata）
	private final ImageResponseMetadata imageResponseMetadata;

	/**
	 * List of generate images returned by the AI provider.
	 */
	// AI 服务商返回的生成图片列表；构造时已转为不可变列表
	private final List<ImageGeneration> imageGenerations;

	/**
	 * Construct a new {@link ImageResponse} instance without metadata.
	 * @param generations the {@link List} of {@link ImageGeneration} returned by the AI
	 * provider.
	 */
	// 便捷构造方法：不带元数据，自动填入一个空的 ImageResponseMetadata，保证 getMetadata() 非空
	public ImageResponse(List<ImageGeneration> generations) {
		this(generations, new ImageResponseMetadata());
	}

	/**
	 * Construct a new {@link ImageResponse} instance.
	 * @param generations the {@link List} of {@link ImageGeneration} returned by the AI
	 * provider.
	 * @param imageResponseMetadata {@link ImageResponseMetadata} containing information
	 * about the use of the AI provider's API.
	 */
	// 全参构造方法：List.copyOf 做防御性拷贝并生成不可变列表，
	// 既防止外部后续修改影响本对象，也使响应对象天然线程安全（注意：入参含 null 元素会抛 NPE）
	public ImageResponse(List<ImageGeneration> generations, ImageResponseMetadata imageResponseMetadata) {
		this.imageResponseMetadata = imageResponseMetadata;
		this.imageGenerations = List.copyOf(generations);
	}

	/**
	 * The {@link List} of {@link ImageGeneration generated outputs}.
	 * <p>
	 * It is a {@link List} of {@link List lists} because the Prompt could request
	 * multiple output {@link ImageGeneration generations}.
	 * @return the {@link List} of {@link ImageGeneration generated outputs}.
	 */
	// 返回全部生成结果（不可变列表）；请求生成 N 张图时列表长度为 N
	@Override
	public List<ImageGeneration> getResults() {
		return this.imageGenerations;
	}

	/**
	 * @return Returns the first {@link ImageGeneration} in the generations list.
	 */
	// 便捷方法：取第一张生成结果（最常见场景 n=1）
	@Override
	public @Nullable ImageGeneration getResult() {
		// 空值处理：列表为空时返回 null 而不是抛越界异常，调用方需自行判空
		if (CollectionUtils.isEmpty(this.imageGenerations)) {
			return null;
		}
		return this.imageGenerations.get(0);
	}

	/**
	 * @return Returns {@link ImageResponseMetadata} containing information about the use
	 * of the AI provider's API.
	 */
	// 返回响应级元数据，永不为 null
	@Override
	public ImageResponseMetadata getMetadata() {
		return this.imageResponseMetadata;
	}

	@Override
	public String toString() {
		return "ImageResponse [" + "imageResponseMetadata=" + this.imageResponseMetadata + ", imageGenerations="
				+ this.imageGenerations + "]";
	}

	// 值相等语义：元数据与生成结果列表均相等才视为同一响应
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 类型不匹配（含 null）返回 false
		if (!(o instanceof ImageResponse that)) {
			return false;
		}
		// 逐字段做空安全比较
		return Objects.equals(this.imageResponseMetadata, that.imageResponseMetadata)
				&& Objects.equals(this.imageGenerations, that.imageGenerations);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.imageResponseMetadata, this.imageGenerations);
	}

}
