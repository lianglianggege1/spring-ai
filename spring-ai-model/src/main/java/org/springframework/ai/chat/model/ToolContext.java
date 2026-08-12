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

package org.springframework.ai.chat.model;

import java.util.Collections;
import java.util.Map;

/**
 * Represents the context for tool execution in a function calling scenario.
 *
 * <p>
 * This class encapsulates a map of contextual information that can be passed to tools
 * (functions) when they are called. It provides an immutable view of the context to
 * ensure thread-safety and prevent modification after creation.
 * </p>
 *
 * <p>
 * The context is typically populated from the {@code toolContext} field of
 * {@code ToolCallingChatOptions} and is used in the function execution process.
 * </p>
 *
 * <p>
 * The context map can contain any information that is relevant to the tool execution.
 * </p>
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】ToolContext 表示"工具调用（function calling）时的上下文"，用于在调用工具时传递额外的业务信息。
 *
 * <p>
 * 解决的问题：模型只会按函数签名生成参数，但工具的真实执行往往还需要一些"模型不该知道、也不该由模型决定"
 * 的数据，例如当前登录用户 id、租户 id、请求追踪 id、数据库连接等。这些数据就通过 ToolContext 旁路传入。
 *
 * <p>
 * 关键设计：
 * <ul>
 * <li>类被声明为 {@code final}，且字段为 {@code final}，构造时用
 * {@code Collections.unmodifiableMap} 包装 —— 整体是不可变对象，天然线程安全。</li>
 * <li>内容通常来自 {@code ToolCallingChatOptions} 的 {@code toolContext} 字段。</li>
 * <li>Map 的值类型是 Object，可放任意业务对象，取用时需要调用方自行强转。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * // 定义工具时接收 ToolContext
 * public String getOrder(String orderId, ToolContext ctx) {
 *     String userId = (String) ctx.getContext().get("userId");
 *     // ...
 * }
 * }</pre>
 */
public final class ToolContext {

	// 中文说明：上下文数据。构造时已被包装为不可修改视图，因此对外暴露也不会被篡改
	private final Map<String, Object> context;

	/**
	 * Constructs a new ToolContext with the given context map.
	 * @param context A map containing the tool context information. This map is wrapped
	 * in an unmodifiable view to prevent changes.
	 */
	// 中文说明：唯一构造器。注意这里用的是 unmodifiableMap（只读视图）而非深拷贝，
	// 意味着若外部仍持有原 map 的引用并修改它，本对象看到的内容也会随之变化；
	// 但通过 ToolContext 本身无法修改内容，对下游工具而言是只读的。
	// 另外：入参为 null 时这里会直接抛 NPE（未做空值兜底），调用方需保证传入非 null。
	public ToolContext(Map<String, Object> context) {
		this.context = Collections.unmodifiableMap(context);
	}

	/**
	 * Returns the immutable context map.
	 * @return An unmodifiable view of the context map.
	 */
	// 中文说明：获取只读上下文 Map。对返回值执行 put/remove 会抛 UnsupportedOperationException。
	public Map<String, Object> getContext() {
		return this.context;
	}

}
