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

package org.springframework.ai.chat.metadata;

import org.jspecify.annotations.Nullable;

/**
 * Abstract Data Type (ADT) encapsulating metadata on the usage of an AI provider's API
 * per AI request.
 *
 * @author John Blum
 * @author Ilayaperumal Gopinathan
 * @since 0.7.0
 */
/**
 * 【中文说明】token 用量（Usage）抽象接口，封装一次 AI 请求所消耗的 token 统计信息。
 *
 * <p>
 * 用途：调用大模型是按 token 计费的，本接口把各厂商五花八门的用量字段统一成三个核心指标：
 * <ul>
 * <li>{@link #getPromptTokens()}——输入（提示词）消耗的 token 数；</li>
 * <li>{@link #getCompletionTokens()}——输出（模型生成内容）消耗的 token 数；</li>
 * <li>{@link #getTotalTokens()}——两者之和，默认实现直接相加。</li>
 * </ul>
 * 另有 {@link #getNativeUsage()} 用于拿到厂商原始用量对象（含框架未抽象的特殊字段），
 * 以及两个与 prompt 缓存相关的默认方法。
 *
 * <p>
 * 典型用法：{@code chatResponse.getMetadata().getUsage().getTotalTokens()}，
 * 常用于成本核算、配额控制与埋点监控。
 *
 * <p>
 * 主要实现：{@link DefaultUsage}（普通实现）、{@link EmptyUsage}（全为 0 的空对象）。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum、Ilayaperumal Gopinathan；@since 0.7.0。
 */
public interface Usage {

	/**
	 * Returns the number of tokens used in the {@literal prompt} of the AI request.
	 * @return an {@link Integer} with the number of tokens used in the {@literal prompt}
	 * of the AI request.
	 * @see #getCompletionTokens()
	 */
	// 【中文】输入 token 数：发送给模型的提示词（含系统提示、历史消息、本轮问题）所占的 token。
	Integer getPromptTokens();

	/**
	 * Returns the number of tokens returned in the {@literal generation (aka completion)}
	 * of the AI's response.
	 * @return an {@link Integer} with the number of tokens returned in the
	 * {@literal generation (aka completion)} of the AI's response.
	 * @see #getPromptTokens()
	 */
	// 【中文】输出 token 数（completion，即"补全"）：模型生成的回复内容所占的 token。
	// 通常输出 token 的单价高于输入 token，是成本优化的重点关注项。
	Integer getCompletionTokens();

	/**
	 * Return the total number of tokens from both the {@literal prompt} of an AI request
	 * and {@literal generation} of the AI's response.
	 * @return the total number of tokens from both the {@literal prompt} of an AI request
	 * and {@literal generation} of the AI's response.
	 * @see #getPromptTokens()
	 * @see #getCompletionTokens()
	 */
	// 【中文】总 token 数 = 输入 + 输出，提供 default 实现，实现类通常无需重写。
	default Integer getTotalTokens() {
		// 【中文】空值处理：部分厂商可能不返回某一项，这里用三元运算把 null 归一为 0，
		// 避免下面 Integer 自动拆箱相加时抛出 NullPointerException。
		Integer promptTokens = getPromptTokens();
		promptTokens = promptTokens != null ? promptTokens : 0;
		Integer completionTokens = getCompletionTokens();
		completionTokens = completionTokens != null ? completionTokens : 0;
		// 【中文】两个 Integer 相加会先自动拆箱为 int、再自动装箱为 Integer 返回。
		return promptTokens + completionTokens;
	}

	/**
	 * Return the usage data from the underlying model API response.
	 * @return the object of type inferred by the API response.
	 */
	// 【中文】返回厂商 API 原始的用量对象（"逃生舱口"设计）。
	// 统一抽象难免有损失，若需要框架未覆盖的厂商特有字段（如推理 token 数、各类明细），
	// 可取出该对象强转为具体厂商类型后读取。代价是代码将与特定厂商耦合。
	@Nullable Object getNativeUsage();

	/**
	 * Returns the number of input tokens read from the prompt cache, if the provider
	 * supports prompt caching. Cached tokens are tokens that were previously processed
	 * and stored by the provider, reducing cost and latency for repeated prompt prefixes.
	 * @return the number of cached input tokens read, or {@code null} if the provider
	 * does not support prompt caching or no cache hit occurred.
	 * @since 2.0.0
	 */
	// 【中文】从 prompt 缓存中"命中读取"的输入 token 数（@since 2.0.0）。
	// 提示词缓存：厂商会缓存重复出现的提示词前缀（如很长的系统提示词），命中后可显著降低费用与延迟。
	// 默认实现返回 null，表示"不支持该特性或本次未命中"——这样已有实现类无需改动即可编译通过（接口演进的兼容手法）。
	default @Nullable Long getCacheReadInputTokens() {
		return null;
	}

	/**
	 * Returns the number of input tokens written to the prompt cache, if the provider
	 * supports prompt caching. Cache writes occur when new prompt content is cached for
	 * the first time.
	 * @return the number of input tokens written to cache, or {@code null} if the
	 * provider does not support prompt caching or no cache write occurred.
	 * @since 2.0.0
	 */
	// 【中文】"写入"prompt 缓存的输入 token 数（@since 2.0.0）：
	// 即首次把新的提示词内容存进缓存时所统计的 token。部分厂商对缓存写入单独计价。
	// 同样默认返回 null 表示不支持该特性。
	default @Nullable Long getCacheWriteInputTokens() {
		return null;
	}

}
