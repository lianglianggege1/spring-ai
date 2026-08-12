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

import org.springframework.ai.model.Model;

/**
 * 图像生成模型的统一抽象接口（文生图）。
 * <p>
 * 它是 {@link Model} 泛型接口在图像模态上的特化：输入 {@link ImagePrompt}，输出 {@link ImageResponse}。
 * 各厂商（OpenAI DALL·E、Stability AI、Azure OpenAI、ZhiPuAI 等）实现本接口，
 * 使上层业务代码可以面向同一套 API 编程、按需替换底层模型。
 * <p>
 * 标注了 {@code @FunctionalInterface}：接口中只有 {@link #call(ImagePrompt)} 一个抽象方法，
 * 因此可以直接用 Lambda 表达式实现，便于测试时打桩（mock）。
 * <p>
 * 典型用法：{@code ImageResponse resp = imageModel.call(new ImagePrompt("一只宇航员猫"));}
 */
@FunctionalInterface
public interface ImageModel extends Model<ImagePrompt, ImageResponse> {

	// 唯一抽象方法：同步调用图像模型，传入提示词与选项，返回包含若干张生成图片的响应
	ImageResponse call(ImagePrompt request);

}
