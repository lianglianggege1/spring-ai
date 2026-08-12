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

package org.springframework.ai.model.tool.internal;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * This class bridges blocking Tools call and the reactive context. When calling tools, it
 * captures the context in a thread local, making it available to re-inject in a nested
 * reactive call.
 *
 * @author Daniel Garnier-Moiroux
 * @since 1.1.0
 */
/**
 * 【中文说明】ToolCallReactiveContextHolder 用于在<b>阻塞式工具调用</b>与
 * <b>响应式上下文</b>之间搭桥。
 *
 * <p>
 * 要解决的问题：Reactor 的 {@code Context} 是随响应式流传递的，<b>不会</b>自动跨越
 * 阻塞调用边界。当模型触发工具（Tool/Function Calling）时，工具方法通常是普通的阻塞
 * Java 方法；如果该方法内部又发起了新的响应式调用，原先流中携带的上下文
 * （如 Spring Security 的认证信息、租户标识等）就丢失了。
 *
 * <p>
 * 解决思路：在进入工具调用前，把当前 Reactor 上下文<b>暂存到 ThreadLocal</b>；
 * 工具内部发起嵌套响应式调用时再从 ThreadLocal 取出并重新注入，从而实现上下文透传。
 *
 * <p>
 * 关键字段 {@code context}：{@code ThreadLocal<ContextView>}，
 * 用 {@code withInitial(Context::empty)} 提供初始值，保证 {@link #getContext()}
 * <b>永远不会返回 null</b>——未设置时返回空上下文，调用方无需判空。
 *
 * <p>
 * 使用注意（重要）：ThreadLocal 必须成对使用，用完务必调用 {@link #clearContext()}，
 * 否则在线程池环境下会造成内存泄漏或上下文串用。
 *
 * <p>
 * 本类位于 {@code internal} 包，属于框架内部 API，不保证向后兼容，业务代码不应直接依赖。
 *
 * @author Daniel Garnier-Moiroux
 * @since 1.1.0
 */
public final class ToolCallReactiveContextHolder {

	// 【中文】线程本地变量，存放当前线程绑定的 Reactor 上下文。
	// withInitial(Context::empty) 的作用：未显式设置时返回空上下文而非 null，
	// 这是一种优雅的空值处理，让调用方免于判空。
	private static final ThreadLocal<ContextView> context = ThreadLocal.withInitial(Context::empty);

	// 【中文】私有构造器 + final 类：标准静态工具类，禁止实例化与继承。
	private ToolCallReactiveContextHolder() {
		// prevent instantiation
	}

	// 【中文】把响应式上下文绑定到当前线程。通常在进入工具调用之前由框架调用。
	public static void setContext(ContextView contextView) {
		context.set(contextView);
	}

	// 【中文】读取当前线程绑定的响应式上下文。
	// 得益于 withInitial，此处保证返回非 null（未设置时为空上下文）。
	public static ContextView getContext() {
		return context.get();
	}

	// 【中文】清除当前线程的绑定。
	// 使用 remove() 而非 set(null) 很关键：remove 会真正移除 ThreadLocalMap 中的条目，
	// 避免线程复用（线程池）时残留旧上下文，也防止内存泄漏。
	// 调用方应在 finally 块中调用本方法以确保一定执行。
	public static void clearContext() {
		context.remove();
	}

}
