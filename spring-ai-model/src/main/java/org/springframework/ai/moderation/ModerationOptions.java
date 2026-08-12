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

package org.springframework.ai.moderation;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelOptions;

/**
 * Represents the options for moderation.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】内容审核的可选参数接口。
 *
 * <p>
 * 继承自通用标记接口 {@code ModelOptions}，用于承载调用审核模型时的运行时配置。
 * 当前公共层只抽象出「模型名称」一项，各厂商实现可在自己的子类中扩展更多私有参数。
 *
 * <p>
 * 它会被放进 {@link ModerationPrompt} 一并传给模型；若为 null 则使用实现方的默认配置。
 * 构建实例请使用 {@link ModerationOptionsBuilder}。
 */
public interface ModerationOptions extends ModelOptions {

	// 中文：获取指定使用的审核模型名称（如 OpenAI 的 text-moderation-latest）。
	// 标注 @Nullable 表示允许不指定，此时由具体实现决定默认模型
	@Nullable String getModel();

}
