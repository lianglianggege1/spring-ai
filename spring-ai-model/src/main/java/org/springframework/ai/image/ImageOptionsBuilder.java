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

/**
 * {@link ImageOptions} 的通用构建器（Builder 模式）。
 * <p>
 * 用于以链式调用的方式装配一份「可移植」的图像生成选项，避免使用多参数构造方法。
 * 设计要点：
 * <ul>
 * <li>类被声明为 {@code final} 且构造方法为 {@code private}，只能通过静态工厂
 * {@link #builder()} 创建，杜绝继承与随意实例化。</li>
 * <li>内部持有一个私有静态内部类 {@code DefaultImageModelOptions} 作为真实数据载体，
 * {@link #build()} 直接返回它，对外只暴露 {@link ImageOptions} 接口，隐藏实现细节。</li>
 * <li>每个 setter 方法返回 {@code this}，从而支持链式调用。</li>
 * <li>未设置的属性保持 null，表示「不指定」，交由模型服务端使用默认值。</li>
 * </ul>
 * 典型用法：
 * <pre>{@code
 * ImageOptions options = ImageOptionsBuilder.builder()
 *         .model("dall-e-3")
 *         .n(1)
 *         .width(1024).height(1024)
 *         .responseFormat("url")
 *         .build();
 * }</pre>
 */
public final class ImageOptionsBuilder {

	// 被逐步填充的选项实例；build() 时直接返回该对象（注意：构建器不可重复复用于构建多份独立配置）
	private final DefaultImageModelOptions options = new DefaultImageModelOptions();

	// 私有构造方法：强制通过静态工厂 builder() 获取实例
	private ImageOptionsBuilder() {

	}

	// 静态工厂方法：创建一个全新的构建器
	public static ImageOptionsBuilder builder() {
		return new ImageOptionsBuilder();
	}

	// 设置生成图片数量，返回 this 以支持链式调用
	public ImageOptionsBuilder n(Integer n) {
		this.options.setN(n);
		return this;
	}

	// 设置模型名称/版本
	public ImageOptionsBuilder model(String model) {
		this.options.setModel(model);
		return this;
	}

	// 设置返回格式：url 或 b64_json
	public ImageOptionsBuilder responseFormat(String responseFormat) {
		this.options.setResponseFormat(responseFormat);
		return this;
	}

	// 设置输出图片宽度（像素），通常需与 height 组成模型支持的尺寸
	public ImageOptionsBuilder width(Integer width) {
		this.options.setWidth(width);
		return this;
	}

	// 设置输出图片高度（像素）
	public ImageOptionsBuilder height(Integer height) {
		this.options.setHeight(height);
		return this;
	}

	// 设置画面风格，取值由具体模型定义
	public ImageOptionsBuilder style(String style) {
		this.options.setStyle(style);
		return this;
	}

	// 完成构建，以 ImageOptions 接口类型返回，屏蔽内部实现类
	public ImageOptions build() {
		return this.options;
	}

	/**
	 * {@link ImageOptions} 的默认实现，仅作为构建器的内部数据容器。
	 * <p>
	 * 声明为 private static 内部类：外部无法直接 new，只能经由
	 * {@link ImageOptionsBuilder#build()} 以接口类型获得，从而保证实现可自由演进。
	 * 所有字段默认为 null，语义为「该参数不指定」。
	 */
	private static class DefaultImageModelOptions implements ImageOptions {

		// 生成图片数量
		private @Nullable Integer n;

		// 模型名称/版本
		private @Nullable String model;

		// 图片宽度（像素）
		private @Nullable Integer width;

		// 图片高度（像素）
		private @Nullable Integer height;

		// 返回格式：url 或 b64_json
		private @Nullable String responseFormat;

		// 画面风格
		private @Nullable String style;

		@Override
		public @Nullable Integer getN() {
			return this.n;
		}

		public void setN(Integer n) {
			this.n = n;
		}

		@Override
		public @Nullable String getModel() {
			return this.model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		@Override
		public @Nullable String getResponseFormat() {
			return this.responseFormat;
		}

		public void setResponseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
		}

		@Override
		public @Nullable Integer getWidth() {
			return this.width;
		}

		public void setWidth(Integer width) {
			this.width = width;
		}

		@Override
		public @Nullable Integer getHeight() {
			return this.height;
		}

		public void setHeight(Integer height) {
			this.height = height;
		}

		@Override
		public @Nullable String getStyle() {
			return this.style;
		}

		public void setStyle(String style) {
			this.style = style;
		}

	}

}
