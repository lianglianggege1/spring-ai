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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ToolExecutionExceptionProcessor}. Can be configured
 * with an allowlist of exceptions that will be unwrapped from the
 * {@link ToolExecutionException} and rethrown as is.
 *
 * @author Thomas Vitale
 * @author Daniel Garnier-Moiroux
 * @author YunKui Lu
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolExecutionExceptionProcessor} 的默认实现。
 *
 * <p>
 * 默认策略是「<b>吞掉异常并把错误信息返回给模型</b>」，从而让对话能够继续； 但通过两个开关可以改变这一行为：
 * <ul>
 * <li>{@code alwaysThrow}：为 true 时一律向上抛出，不再返回错误文本；</li>
 * <li>{@code rethrownExceptions}：异常「白名单」，命中的异常会被<b>拆包</b>（unwrap） 后以原始类型重新抛出。这对
 * Spring Security 的权限异常等特别有用—— 这类异常需要被上层的框架机制捕获，而不是变成一段告诉模型「你没权限」的文本。</li>
 * </ul>
 *
 * <p>
 * 典型用法： <pre>{@code
 * DefaultToolExecutionExceptionProcessor.builder()
 *         .alwaysThrow(false)
 *         .rethrowExceptions(List.of(AccessDeniedException.class))
 *         .build();
 * }</pre>
 *
 * @author Thomas Vitale
 * @author Daniel Garnier-Moiroux
 * @author YunKui Lu
 * @since 1.0.0
 */
public class DefaultToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {

	private static final Log logger = LogFactory.getLog(DefaultToolExecutionExceptionProcessor.class);

	// 默认不抛出异常，而是把错误信息返回给模型，保证对话流程不中断
	private static final boolean DEFAULT_ALWAYS_THROW = false;

	// 为 true 时：任何工具异常都直接向上抛，交给调用方处理
	private final boolean alwaysThrow;

	// 异常白名单：命中者会被拆包并以原始类型重新抛出
	private final List<Class<? extends RuntimeException>> rethrownExceptions;

	/**
	 * 【中文说明】便捷构造器：只设置 alwaysThrow，白名单为空。
	 * @param alwaysThrow 是否总是抛出异常
	 */
	public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow) {
		// 委托给全参构造器，白名单传空列表
		this(alwaysThrow, Collections.emptyList());
	}

	/**
	 * 【中文说明】全参构造器。
	 * @param alwaysThrow 是否总是抛出异常
	 * @param rethrownExceptions 需要拆包重抛的异常类型白名单
	 */
	public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow,
			List<Class<? extends RuntimeException>> rethrownExceptions) {
		this.alwaysThrow = alwaysThrow;
		// 包装为不可修改列表，防止构造后被外部篡改
		this.rethrownExceptions = Collections.unmodifiableList(rethrownExceptions);
	}

	/**
	 * 【中文说明】按以下顺序决策：白名单拆包重抛 → 非 RuntimeException 直接重抛 → alwaysThrow 重抛 → 否则返回错误文本。
	 * @param exception 工具执行异常
	 * @return 回传给模型的错误描述
	 */
	@Override
	public String process(ToolExecutionException exception) {
		Assert.notNull(exception, "exception cannot be null");
		Throwable cause = exception.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			// 白名单匹配：isAssignableFrom 意味着「子类也算命中」
			// 例如白名单里配置了 AccessDeniedException，其子类同样会被拆包重抛
			if (this.rethrownExceptions.stream().anyMatch(rethrown -> rethrown.isAssignableFrom(cause.getClass()))) {
				// 注意抛的是 runtimeException（原始根因）而非 exception（包装类），即「拆包」
				throw runtimeException;
			}
		}
		else {
			// If the cause is not a RuntimeException (e.g., IOException,
			// OutOfMemoryError), rethrow the tool exception.
			// 【中文】根因不是 RuntimeException（如 IOException 受检异常、OutOfMemoryError 等严重错误）时，
			// 认为不适合「转成文本喂给模型」，一律向上抛出包装异常，交由应用层处理
			throw exception;
		}

		// 全局开关：要求任何异常都向上抛
		if (this.alwaysThrow) {
			throw exception;
		}
		// 走到这里说明：根因是 RuntimeException、不在白名单、且未开启 alwaysThrow
		// —— 此时把错误信息作为工具结果返回给模型
		String message = exception.getMessage();
		// 空值处理：某些异常的 message 为 null 或空白，需构造一个可读的兜底描述，
		// 否则模型会收到一个空字符串而无从判断发生了什么
		if (message == null || message.isBlank()) {
			message = "Exception occurred in tool: " + exception.getToolDefinition().name() + " ("
					+ cause.getClass().getSimpleName() + ")";
		}
		// 异常被「吞掉」了，因此这里保留 debug 日志并附上完整堆栈，便于排查问题
		if (logger.isDebugEnabled()) {
			logger.debug("Exception thrown by tool: " + exception.getToolDefinition().name() + ". Message: " + message,
					exception);
		}
		return message;
	}

	/**
	 * 【中文说明】获取 Builder 实例，推荐用它来构造本处理器。
	 * @return 新的 Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】构建器：两个配置项相互独立、均为可选，未设置时使用默认值 （alwaysThrow=false、白名单为空）。
	 */
	public static final class Builder {

		// 默认值来自类常量 DEFAULT_ALWAYS_THROW（false）
		private boolean alwaysThrow = DEFAULT_ALWAYS_THROW;

		// 默认空白名单，表示不对任何异常做拆包重抛
		private List<Class<? extends RuntimeException>> exceptions = Collections.emptyList();

		/**
		 * Rethrow the {@link ToolExecutionException}
		 * @param alwaysThrow when true, throws; when false, returns the exception message
		 * @return the builder instance
		 */
		/**
		 * 【中文说明】设置是否总是抛出 {@link ToolExecutionException}。
		 * @param alwaysThrow true=抛出异常；false=返回异常信息文本给模型
		 * @return 当前 Builder，支持链式调用
		 */
		public Builder alwaysThrow(boolean alwaysThrow) {
			this.alwaysThrow = alwaysThrow;
			return this;
		}

		/**
		 * An allowlist of exceptions thrown by tools, which will be unwrapped and
		 * re-thrown without further processing.
		 * @param exceptions the list of exceptions
		 * @return the builder instance
		 */
		/**
		 * 【中文说明】设置异常白名单：名单内（含其子类）的异常会被从
		 * {@link ToolExecutionException} 中拆包，并以原始类型重新抛出，不再做任何后续处理。
		 * <p>
		 * 泛型限定为 {@code ? extends RuntimeException}，因此只能登记非受检异常。
		 * @param exceptions 异常类型列表
		 * @return 当前 Builder，支持链式调用
		 */
		public Builder rethrowExceptions(List<Class<? extends RuntimeException>> exceptions) {
			this.exceptions = exceptions;
			return this;
		}

		/**
		 * 【中文说明】构建最终的处理器实例。
		 * @return 配置完成的 DefaultToolExecutionExceptionProcessor
		 */
		public DefaultToolExecutionExceptionProcessor build() {
			return new DefaultToolExecutionExceptionProcessor(this.alwaysThrow, this.exceptions);
		}

	}

}
