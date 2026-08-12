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

package org.springframework.ai.image.observation;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.util.Assert;

/**
 * Context used to store metadata for image model exchanges.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 图像模型调用的可观测性（Observation）上下文。
 * <p>
 * 基于 Micrometer Observation：模型调用开始时创建本对象，调用结束后把响应写回，
 * 供 {@link ImageModelObservationConvention} 从中提取标签（低基数/高基数 KeyValues）
 * 并生成 metrics 与 tracing span。
 * <p>
 * 关键内容：
 * <ul>
 * <li>请求 {@link ImagePrompt} —— 由父类 {@code ModelObservationContext} 持有。</li>
 * <li>响应 {@link ImageResponse} —— 调用完成后由调用方 setResponse 写入。</li>
 * <li>{@code AiOperationMetadata} —— 操作类型固定为 {@code image}，并记录服务商名称。</li>
 * </ul>
 * 构造方法为包级私有，只能通过 {@link #builder()} 创建，以便统一做参数校验。
 *
 * @since 1.0.0
 */
public class ImageModelObservationContext extends ModelObservationContext<ImagePrompt, ImageResponse> {

	// 包级私有构造方法：外部必须走 builder()，保证参数经过校验
	ImageModelObservationContext(ImagePrompt imagePrompt, String provider) {
		// 交由父类保存请求对象，并组装操作元数据：操作类型固定为 IMAGE，provider 为具体厂商标识
		super(imagePrompt,
				AiOperationMetadata.builder().operationType(AiOperationType.IMAGE.value()).provider(provider).build());
		// 参数校验：观测约定需要从 options 中读取模型名、尺寸等标签，因此这里强制 options 不能为 null
		Assert.notNull(imagePrompt.getOptions(), "image options cannot be null");
	}

	// 静态工厂：获取构建器实例
	public static Builder builder() {
		return new Builder();
	}

	// 返回操作类型常量 "image"，用于观测标签
	public String getOperationType() {
		return AiOperationType.IMAGE.value();
	}

	/**
	 * {@link ImageModelObservationContext} 的构建器。
	 * <p>
	 * 采用 Builder 模式集中校验必填项：{@code imagePrompt} 与 {@code provider} 都必须提供，
	 * 在 {@link #build()} 中通过 Assert 断言进行「快速失败」。
	 */
	public static final class Builder {

		// 待观测的图像请求，必填
		private @Nullable ImagePrompt imagePrompt;

		// AI 服务商标识（如 openai、stabilityai），必填且不能为空字符串
		private @Nullable String provider;

		// 私有构造：只能通过外部类的 builder() 获得
		private Builder() {
		}

		// 设置图像请求，链式返回 this
		public Builder imagePrompt(ImagePrompt imagePrompt) {
			this.imagePrompt = imagePrompt;
			return this;
		}

		// 设置服务商标识，链式返回 this
		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		// 构建上下文对象：先校验必填项，再创建实例
		public ImageModelObservationContext build() {
			// 必填校验：请求对象不可为 null
			Assert.notNull(this.imagePrompt, "imagePrompt cannot be null");
			// 必填校验：hasText 比 notNull 更严格，要求非 null 且含非空白字符
			Assert.hasText(this.provider, "provider cannot be null or empty");
			return new ImageModelObservationContext(this.imagePrompt, this.provider);
		}

	}

}
