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

package org.springframework.ai.tool.execution;

import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * An exception thrown when a tool execution fails.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具执行异常：当某个工具在执行过程中抛出异常时，框架会把原始异常<b>包装</b>成本异常向上抛出。
 *
 * <p>
 * 包装的价值在于携带了 {@link ToolDefinition}——即「是哪个工具出错了」。 仅有原始异常无法定位工具来源，而
 * {@link ToolExecutionExceptionProcessor} 在决定 「向模型返回错误文本」还是「继续向上抛」时，往往需要这个信息。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code toolDefinition}：出错工具的定义，可据此拿到工具名称。</li>
 * </ul>
 *
 * <p>
 * 继承 {@link RuntimeException} 属于非受检异常，因此不会污染 {@code ToolCallback#call} 的方法签名。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see ToolExecutionExceptionProcessor
 */
public class ToolExecutionException extends RuntimeException {

	// 出错工具的定义，用于定位「是哪个工具失败了」
	private final ToolDefinition toolDefinition;

	/**
	 * 【中文说明】构造异常。
	 * <p>
	 * 注意：这里把 {@code cause.getMessage()} 作为本异常的 message， 即<b>直接沿用原始异常的错误描述</b>而不做额外包装，
	 * 这样后续处理器取到的 message 就是根因的描述，便于直接反馈给模型。 同时把 cause 传入父类以保留完整的异常链与堆栈。
	 * @param toolDefinition 出错工具的定义
	 * @param cause 工具抛出的原始异常
	 */
	public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
		// 沿用根因的 message，并保留异常链
		super(cause.getMessage(), cause);
		this.toolDefinition = toolDefinition;
	}

	/**
	 * 【中文说明】获取出错工具的定义（可进一步取出工具名称等信息）。
	 * @return 工具定义
	 */
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

}
