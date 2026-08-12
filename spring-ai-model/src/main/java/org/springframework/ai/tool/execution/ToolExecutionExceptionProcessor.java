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

/**
 * A functional interface to process a {@link ToolExecutionException} by either converting
 * the error message to a String that can be sent back to the AI model or throwing an
 * exception to be handled by the caller.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具执行异常处理器：决定工具出错后「如何收场」。
 *
 * <p>
 * 面对 {@link ToolExecutionException}，实现类有两条互斥的出路：
 * <ol>
 * <li><b>转成字符串返回</b>：把错误信息作为工具执行结果回传给模型，让模型自行判断是重试、 换工具还是向用户解释——这能让对话继续下去；</li>
 * <li><b>抛出异常</b>：中断整个调用链，交由上层应用代码处理——适合不可恢复的严重错误。</li>
 * </ol>
 *
 * <p>
 * 这正是方法签名「返回 String，同时又允许抛异常」的原因：两种语义共用一个方法。
 *
 * <p>
 * 典型用法：注册一个 {@code ToolExecutionExceptionProcessor} 类型的 Spring Bean 即可覆盖
 * 默认行为；默认实现见 {@link DefaultToolExecutionExceptionProcessor}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see DefaultToolExecutionExceptionProcessor
 */
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {

	/**
	 * Convert an exception thrown by a tool to a String that can be sent back to the AI
	 * model or throw an exception to be handled by the caller.
	 */
	/**
	 * 【中文说明】处理工具执行异常。
	 * @param exception 被包装后的工具执行异常（内含出错工具的定义与原始根因）
	 * @return 要回传给模型的错误描述文本
	 * @throws RuntimeException 若实现选择「向上抛」而非「返回文本」
	 */
	String process(ToolExecutionException exception);

}
