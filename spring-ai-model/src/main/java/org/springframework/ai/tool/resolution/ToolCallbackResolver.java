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

import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.ToolCallback;

/**
 * A resolver for {@link ToolCallback} instances.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具回调解析器：根据「工具名称」查找对应的 {@link ToolCallback}。
 *
 * <p>
 * 与 {@code ToolCallbackProvider}（批量提供工具）的区别在于视角不同：Provider 是 <b>push</b>——一次性把工具列表交出去；Resolver 是
 * <b>pull</b>——按名字<b>按需</b>查找。
 *
 * <p>
 * 使用场景：模型返回工具调用请求时只给出工具名（如 {@code "getWeather"}）， 框架需要凭这个名字反查出真正可执行的
 * {@link ToolCallback}。这也使得工具可以延迟 创建、甚至来自 Spring 容器中尚未实例化的 Bean。
 *
 * <p>
 * 内置实现：{@link StaticToolCallbackResolver}（静态注册表）、
 * {@link DelegatingToolCallbackResolver}（串联多个解析器）。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ToolCallbackResolver {

	/**
	 * Resolve the {@link ToolCallback} for the given tool name.
	 */
	/**
	 * 【中文说明】按名称解析工具回调。
	 * <p>
	 * 返回值标注了 {@code @Nullable}：<b>找不到时应返回 null 而不是抛异常</b>。 这是该契约的关键点——
	 * {@link DelegatingToolCallbackResolver} 正是依赖「返回 null」 来判断是否需要继续尝试下一个解析器。
	 * @param toolName 工具名称，不可为 null 或空串
	 * @return 匹配的工具回调；未找到时返回 null
	 */
	@Nullable ToolCallback resolve(String toolName);

}
