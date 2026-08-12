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

import org.springframework.util.Assert;

/**
 * A simple implementation of {@link ToolCallbackProvider} that maintains a static array
 * of {@link ToolCallback} objects. This provider is immutable after construction and
 * provides a straightforward way to supply a fixed set of tool callbacks to AI models.
 *
 * <p>
 * This implementation is thread-safe as it maintains an immutable array of callbacks that
 * is set during construction and cannot be modified afterwards.
 *
 * <p>
 * Example usage: <pre>{@code
 * ToolCallback callback1 = new MyFunctionCallback();
 * ToolCallback callback2 = new AnotherFunctionCallback();
 *
 * // Create provider with varargs constructor
 * ToolCallbackProvider provider1 = new StaticToolCallbackProvider(callback1, callback2);
 *
 * // Or create provider with List constructor
 * List<ToolCallback> callbacks = Arrays.asList(callback1, callback2);
 * ToolCallbackProvider provider2 = new StaticToolCallbackProvider(callbacks);
 * }</pre>
 *
 * @author Christian Tzolov
 * @since 1.0.0
 * @see ToolCallbackProvider
 * @see ToolCallback
 */
/**
 * 【中文说明】静态工具回调提供者：{@link ToolCallbackProvider} 最简单的实现， 内部只持有一个「构造后即固定」的 {@link ToolCallback}
 * 数组。
 *
 * <p>
 * 适用场景：工具集合在编译期/启动期就已确定，无需动态发现（与之相对的是从 MCP 服务端 动态拉取工具的场景）。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code toolCallbacks}：final 数组，构造完成后不再变更。</li>
 * </ul>
 *
 * <p>
 * 线程安全性：由于数组引用为 final 且构造后不修改，因此本类是线程安全的。 但要注意它<b>没有做防御性拷贝</b>（见
 * {@link #getToolCallbacks()} 说明），前提假设是 ToolCallback 本身不可变。
 *
 * <p>
 * 典型用法（对应上方英文示例）： <pre>{@code
 * ToolCallbackProvider provider = new StaticToolCallbackProvider(callback1, callback2);
 * // 或使用接口上的静态工厂
 * ToolCallbackProvider provider2 = ToolCallbackProvider.from(callback1, callback2);
 * }</pre>
 *
 * @author Christian Tzolov
 * @since 1.0.0
 * @see ToolCallbackProvider
 * @see ToolCallback
 */
public class StaticToolCallbackProvider implements ToolCallbackProvider {

	// final 数组：构造后不可重新赋值，保证提供者的不可变性与线程安全
	private final ToolCallback[] toolCallbacks;

	/**
	 * Constructs a new StaticToolCallbackProvider with the specified array of function
	 * callbacks.
	 * @param toolCallbacks the array of function callbacks to be provided by this
	 * provider. Must not be null, though an empty array is permitted.
	 * @throws IllegalArgumentException if the toolCallbacks array is null
	 */
	/**
	 * 【中文说明】可变参数构造器。
	 * <p>
	 * 校验说明：这里只用 {@code Assert.notNull} 校验数组本身非 null，<b>不</b>校验元素非
	 * null，也<b>不</b>做数组拷贝——直接持有传入数组的引用。 因此调用方若在构造后修改原数组，会影响到本对象（实践中应避免）。
	 * @param toolCallbacks 工具回调数组，不可为 null；允许为空数组
	 * @throws IllegalArgumentException 当数组为 null 时抛出
	 */
	public StaticToolCallbackProvider(ToolCallback... toolCallbacks) {
		// 仅校验数组引用非空；空数组是合法的（表示不提供任何工具）
		Assert.notNull(toolCallbacks, "ToolCallbacks must not be null");
		this.toolCallbacks = toolCallbacks;
	}

	/**
	 * Constructs a new StaticToolCallbackProvider with the specified list of function
	 * callbacks. The list is converted to an array internally.
	 * @param toolCallbacks the list of function callbacks to be provided by this
	 * provider. Must not be null and must not contain null elements.
	 * @throws IllegalArgumentException if the toolCallbacks list is null or contains null
	 * elements
	 */
	/**
	 * 【中文说明】List 构造器，内部转换为数组存储。
	 * <p>
	 * 与可变参数构造器的差异：此处使用 {@code Assert.noNullElements}，会<b>同时</b>校验 集合本身非 null
	 * 与集合内不含 null 元素，校验更严格； 且 {@code toArray} 会产生新数组，相当于做了一次防御性拷贝。
	 * @param toolCallbacks 工具回调列表，不可为 null 且不可含 null 元素
	 * @throws IllegalArgumentException 当列表为 null 或含 null 元素时抛出
	 */
	public StaticToolCallbackProvider(List<? extends ToolCallback> toolCallbacks) {
		// noNullElements：既校验集合非空，也校验元素均非 null
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		// 传入长度为 0 的数组是 JDK 推荐写法，由 JVM 按实际大小分配，性能更优
		this.toolCallbacks = toolCallbacks.toArray(new ToolCallback[0]);
	}

	/**
	 * Returns the array of function callbacks held by this provider.
	 * @return an array containing all function callbacks provided during construction.
	 * The returned array is a direct reference to the internal array, as the callbacks
	 * are expected to be immutable.
	 */
	/**
	 * 【中文说明】返回内部持有的工具回调数组。
	 * <p>
	 * 设计取舍：这里<b>直接返回内部数组引用</b>，没有做防御性拷贝。 官方注释给出的理由是「ToolCallback 被视为不可变对象」，因此共享引用是安全的、也更高效。
	 * 但需注意数组本身仍是可变的，调用方不应修改返回的数组内容。
	 * @return 构造时传入的全部工具回调
	 */
	@Override
	public ToolCallback[] getToolCallbacks() {
		return this.toolCallbacks;
	}

}
