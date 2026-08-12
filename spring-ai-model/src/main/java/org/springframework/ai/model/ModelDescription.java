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

package org.springframework.ai.model;

/**
 * Describes an AI model's basic characteristics. Provides methods to retrieve the model's
 * name, description, and version.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】ModelDescription 描述一个 AI 模型的<b>基本身份信息</b>：名称、说明、版本。
 *
 * <p>
 * 用途：主要服务于可观测性与展示场景——例如在链路追踪中标注实际使用的模型名，
 * 或在管理界面列出可选模型。它描述的是"模型是什么"，而 {@link ModelOptions} 描述的是
 * "本次调用怎么调"，二者职责不同。
 *
 * <p>
 * 关键设计：只有 {@link #getName()} 是必须实现的抽象方法，其余两个都提供了返回空串的
 * default 实现。这样各厂商枚举类（如 OpenAiApi.ChatModel）只需实现最核心的 getName，
 * 就能满足接口契约，降低了实现成本。
 *
 * <p>
 * 典型用法：各厂商的模型枚举实现本接口，例如
 * {@code enum ChatModel implements ChatModelDescription { GPT_4O("gpt-4o"), ... }}。
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public interface ModelDescription {

	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	// 【中文】返回模型名称（唯一必须实现的方法），通常就是调用 API 时传给服务端的
	// model 参数值，如 "gpt-4o"、"text-embedding-3-small"。
	String getName();

	/**
	 * Returns the description of the model.
	 * @return the description of the model
	 */
	// 【中文】返回模型的文字说明。default 方法返回空串，实现类可按需覆盖。
	// 采用 default 而非抽象方法，是为了让实现类不必被迫实现非核心信息。
	default String getDescription() {
		return "";
	}

	/**
	 * Returns the version of the model.
	 * @return the version of the model
	 */
	// 【中文】返回模型版本号。同样是可选的 default 方法，默认空串表示"未提供版本信息"。
	default String getVersion() {
		return "";
	}

}
