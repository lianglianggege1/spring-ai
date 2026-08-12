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

package org.springframework.ai.tool;

import java.util.List;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具回调提供者：负责「从某种来源批量提供 {@link ToolCallback}」的工厂式抽象。
 *
 * <p>
 * 引入该接口是为了屏蔽工具的<b>来源差异</b>。同一个 ChatClient 可以同时接收来自多种渠道的工具：
 * <ul>
 * <li>{@code StaticToolCallbackProvider}：直接给定一组固定的回调；</li>
 * <li>{@code MethodToolCallbackProvider}：扫描对象上标注 {@code @Tool} 的方法；</li>
 * <li>MCP 等外部协议的 Provider：从远端服务动态拉取工具列表。</li>
 * </ul>
 *
 * <p>
 * 典型用法：把实现类注册为 Spring Bean，或在构建 ChatClient 时通过
 * {@code .defaultToolCallbacks(provider)} 传入。
 *
 * <p>
 * 接口内的两个 {@code from(...)} 静态工厂方法是便捷入口，均返回不可变的
 * {@link StaticToolCallbackProvider}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ToolCallbackProvider {

	/**
	 * 【中文说明】获取该提供者能提供的全部工具回调。
	 * <p>
	 * 注意返回类型是<b>数组</b>而非 List：这是为了与 Spring AI 中大量以可变参数
	 * {@code ToolCallback...} 接收工具的 API 保持一致，便于直接展开传递。
	 * @return 工具回调数组；无工具时应返回空数组而非 null
	 */
	ToolCallback[] getToolCallbacks();

	/**
	 * 【中文说明】静态工厂：由 List 快速创建一个静态（不可变）的提供者。
	 * <p>
	 * 使用 {@code ? extends ToolCallback} 通配符，因此可以直接传入
	 * {@code List<FunctionToolCallback>} 等子类型集合。
	 * @param toolCallbacks 工具回调列表，不可为 null 且不可含 null 元素
	 * @return 包装后的静态提供者
	 */
	static ToolCallbackProvider from(List<? extends ToolCallback> toolCallbacks) {
		return new StaticToolCallbackProvider(toolCallbacks);
	}

	/**
	 * 【中文说明】静态工厂：由可变参数快速创建一个静态（不可变）的提供者。
	 * @param toolCallbacks 工具回调，可传 0 个或多个
	 * @return 包装后的静态提供者
	 */
	static ToolCallbackProvider from(ToolCallback... toolCallbacks) {
		return new StaticToolCallbackProvider(toolCallbacks);
	}

}
