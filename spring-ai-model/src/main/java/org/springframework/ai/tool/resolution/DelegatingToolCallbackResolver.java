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

package org.springframework.ai.tool.resolution;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * A {@link ToolCallbackResolver} that delegates to a list of {@link ToolCallbackResolver}
 * instances.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】委派式工具回调解析器：内部持有一组 {@link ToolCallbackResolver}， 解析时按<b>列表顺序依次尝试</b>，返回第一个成功的结果。
 *
 * <p>
 * 这是典型的<b>责任链模式</b>，用于统一多种工具来源。例如同时存在： 通过 {@code @Tool} 注解声明的本地工具、以 Bean
 * 形式注册的函数式工具、 以及来自 MCP 服务端的远程工具——把它们各自的 Resolver 串起来即可。
 *
 * <p>
 * 关键点：<b>列表顺序即优先级</b>。若多个来源存在同名工具，排在前面的解析器胜出。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code toolCallbackResolvers}：被委派的解析器列表，按序尝试。</li>
 * </ul>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class DelegatingToolCallbackResolver implements ToolCallbackResolver {

	// 被委派的解析器列表，遍历顺序即优先级顺序
	private final List<ToolCallbackResolver> toolCallbackResolvers;

	/**
	 * 【中文说明】构造委派解析器。
	 * <p>
	 * 注意：此处直接持有传入列表的引用，<b>未做防御性拷贝</b>， 调用方在构造后修改原列表会影响本对象的行为。
	 * @param toolCallbackResolvers 被委派的解析器列表，不可为 null 且不可含 null 元素
	 */
	public DelegatingToolCallbackResolver(List<ToolCallbackResolver> toolCallbackResolvers) {
		Assert.notNull(toolCallbackResolvers, "toolCallbackResolvers cannot be null");
		Assert.noNullElements(toolCallbackResolvers, "toolCallbackResolvers cannot contain null elements");
		this.toolCallbackResolvers = toolCallbackResolvers;
	}

	/**
	 * 【中文说明】依次询问每个委派解析器，返回首个非 null 结果；全部未命中则返回 null。
	 * @param toolName 工具名称
	 * @return 匹配的工具回调；所有解析器都未找到时返回 null
	 */
	@Override
	public @Nullable ToolCallback resolve(String toolName) {
		Assert.hasText(toolName, "toolName cannot be null or empty");

		// 责任链核心：按顺序尝试，短路返回第一个命中的结果
		for (ToolCallbackResolver toolCallbackResolver : this.toolCallbackResolvers) {
			ToolCallback toolCallback = toolCallbackResolver.resolve(toolName);
			// 依赖子解析器「找不到返回 null」的契约来决定是否继续向后尝试
			if (toolCallback != null) {
				return toolCallback;
			}
		}
		// 所有来源均未找到该工具
		return null;
	}

}
