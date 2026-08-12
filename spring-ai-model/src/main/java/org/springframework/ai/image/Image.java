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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * 图像数据载体。表示一张由图像模型生成的图片。
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code url} —— 图片的可访问地址（通常由服务端托管，可能有有效期）。</li>
 * <li>{@code b64Json} —— 图片的 Base64 编码内容（直接内联返回，无需二次下载）。</li>
 * </ul>
 * 两个字段都是可空的：不同模型/不同请求参数（如 OpenAI 的 {@code response_format}）
 * 决定返回 URL 还是 Base64，通常二者只会有其一被填充，使用前需判空。
 * <p>
 * 典型用法：{@code imageResponse.getResult().getOutput().getUrl()}
 */
public class Image {

	/**
	 * The URL where the image can be accessed.
	 */
	// 图片可访问的 URL 地址；当模型以 Base64 形式返回时该字段为 null
	private @Nullable String url;

	/**
	 * Base64 encoded image string.
	 */
	// 图片的 Base64 编码字符串；当模型以 URL 形式返回时该字段为 null
	private @Nullable String b64Json;

	// 构造方法：两个参数均可为 null，由具体模型实现决定填充哪一个
	public Image(@Nullable String url, @Nullable String b64Json) {
		this.url = url;
		this.b64Json = b64Json;
	}

	// 获取图片 URL，可能为 null（此时应改用 getB64Json()）
	public @Nullable String getUrl() {
		return this.url;
	}

	// 设置图片 URL
	public void setUrl(String url) {
		this.url = url;
	}

	// 获取图片的 Base64 编码内容，可能为 null（此时应改用 getUrl()）
	public @Nullable String getB64Json() {
		return this.b64Json;
	}

	// 设置图片的 Base64 编码内容
	public void setB64Json(String b64Json) {
		this.b64Json = b64Json;
	}

	@Override
	public String toString() {
		return "Image{" + "url='" + this.url + '\'' + ", b64Json='" + this.b64Json + '\'' + '}';
	}

	// 值相等语义：url 与 b64Json 全部相等才视为同一张图片
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用直接判定相等，快速返回
		if (this == o) {
			return true;
		}
		// instanceof 模式匹配：同时完成类型判断与变量绑定，null 也会走到这里返回 false
		if (!(o instanceof Image image)) {
			return false;
		}
		// 使用 Objects.equals 以安全处理两个字段可能为 null 的情况
		return Objects.equals(this.url, image.url) && Objects.equals(this.b64Json, image.b64Json);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.url, this.b64Json);
	}

}
