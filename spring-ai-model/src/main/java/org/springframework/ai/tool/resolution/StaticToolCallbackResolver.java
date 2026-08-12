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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * A {@link ToolCallbackResolver} that resolves tool callbacks from a static registry.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】静态工具回调解析器：把一组固定的 {@link ToolCallback} 预先建成 「工具名 → 回调」的哈希表，解析时直接查表。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code toolCallbacks}：{@code Map<String, ToolCallback>}，key 取自
 * {@code toolCallback.getToolDefinition().name()}。</li>
 * </ul>
 *
 * <p>
 * 注意事项：由于用 Map 存储，<b>同名工具会相互覆盖</b>——后放入的会覆盖先放入的， 且不会有任何告警。因此需保证工具名称全局唯一。
 *
 * <p>
 * 线程安全性：内部使用非线程安全的 {@link HashMap}，但仅在构造器中写入、之后只读， 属于「安全发布后只读」的用法，因此实际使用中是安全的。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class StaticToolCallbackResolver implements ToolCallbackResolver {

	private static final Log logger = LogFactory.getLog(StaticToolCallbackResolver.class);

	// 工具名 -> 工具回调 的注册表，仅在构造器中填充，之后只读
	private final Map<String, ToolCallback> toolCallbacks = new HashMap<>();

	/**
	 * 【中文说明】构造解析器，将传入的工具列表按名称建立索引。
	 * @param toolCallbacks 工具回调列表，不可为 null 且不可含 null 元素
	 */
	public StaticToolCallbackResolver(List<ToolCallback> toolCallbacks) {
		// 两级校验：先确保列表本身非 null，再确保元素均非 null
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");

		// 以工具定义中的 name 作为 key 建立索引；重名工具会被静默覆盖
		toolCallbacks
			.forEach(toolCallback -> this.toolCallbacks.put(toolCallback.getToolDefinition().name(), toolCallback));
	}

	/**
	 * 【中文说明】从静态注册表中查表解析，未命中返回 null（符合接口契约）。
	 * @param toolName 工具名称
	 * @return 匹配的工具回调；未找到返回 null
	 */
	@Override
	public @Nullable ToolCallback resolve(String toolName) {
		// hasText：同时校验非 null、非空串、非纯空白
		Assert.hasText(toolName, "toolName cannot be null or empty");
		logger.debug("ToolCallback resolution attempt from static registry");
		// Map.get 未命中时自然返回 null，正好符合接口「找不到返回 null」的约定
		return this.toolCallbacks.get(toolName);
	}

}
