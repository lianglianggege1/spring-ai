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

package org.springframework.ai.model.observation;

import java.util.List;
import java.util.function.Consumer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * An {@link ObservationHandler} that logs errors using a {@link Tracer}.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】ErrorLoggingObservationHandler 是一个 Micrometer 观测处理器，
 * 用于在 AI 调用<b>发生异常时输出带链路追踪信息的错误日志</b>。
 *
 * <p>
 * 核心价值：普通的 logger.error 打出来的日志与分布式链路是脱节的。本类在记录日志前，
 * 先通过 {@code tracer.withSpan(...)} 把出错时的 Span 重新置为当前 Span，
 * 这样日志框架的 MDC 中就能带上 traceId / spanId，实现<b>日志与链路的关联</b>，
 * 排查问题时可以从一条错误日志直接跳到完整调用链。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code tracer} —— 链路追踪器，用于恢复 Span 上下文；</li>
 * <li>{@code supportedContextTypes} —— 白名单，只处理列表中指定类型的观测上下文，
 * 避免对全应用所有观测都生效；</li>
 * <li>{@code errorConsumer} —— 错误处理策略，采用策略模式外置，
 * 默认是打 error 日志，使用者也可替换为上报告警等自定义行为。</li>
 * </ul>
 *
 * <p>
 * 典型用法：注册为 Spring Bean，Micrometer 会自动纳入 ObservationRegistry。
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
// 【中文】@SuppressWarnings 说明：实现的是原始类型（raw type）的 ObservationHandler
// 而非带泛型的版本，故需抑制 rawtypes 警告；"null" 则用于抑制空安全检查告警。
@SuppressWarnings({ "rawtypes", "null" })
public class ErrorLoggingObservationHandler implements ObservationHandler {

	// 【中文】日志器。使用 Apache Commons Logging 是 Spring 框架内部的一贯做法（便于适配多种日志实现）。
	private static final Log logger = LogFactory.getLog(ErrorLoggingObservationHandler.class);

	// 【中文】链路追踪器，用于在记录日志时恢复出错点的 Span 上下文。
	private final Tracer tracer;

	// 【中文】支持处理的上下文类型白名单。只有 context 是其中某个类的实例时本处理器才会介入。
	private final List<Class<? extends Observation.Context>> supportedContextTypes;

	// 【中文】错误处理回调（策略模式）。把"发现错误后做什么"抽象为 Consumer，
	// 使本类既能打日志，也能被定制成上报监控、发送告警等。
	private final Consumer<Context> errorConsumer;

	// 【中文】便捷构造器：使用默认的错误处理策略——直接打印 error 级别日志。
	public ErrorLoggingObservationHandler(Tracer tracer,
			List<Class<? extends Observation.Context>> supportedContextTypes) {
		// 委托给全参构造器，默认 Consumer 输出上下文中记录的异常
		this(tracer, supportedContextTypes, context -> logger.error("Traced Error: ", context.getError()));
	}

	// 【中文】全参构造器：允许自定义错误处理行为。
	public ErrorLoggingObservationHandler(Tracer tracer,
			List<Class<? extends Observation.Context>> supportedContextTypes, Consumer<Context> errorConsumer) {

		// 参数校验：三个依赖都是必需的，任一为 null 都会导致运行期空指针，故在构造时即拦截
		Assert.notNull(tracer, "Tracer must not be null");
		Assert.notNull(supportedContextTypes, "SupportedContextTypes must not be null");
		Assert.notNull(errorConsumer, "ErrorConsumer must not be null");

		this.tracer = tracer;
		this.supportedContextTypes = supportedContextTypes;
		this.errorConsumer = errorConsumer;
	}

	// 【中文】Micrometer 回调：判断本处理器是否要处理该观测上下文。
	// 只有当 context 是白名单中任一类型的实例时才返回 true。
	// 空值处理：context 为 null 直接返回 false，不做处理。
	@Override
	public boolean supportsContext(Context context) {
		return (context == null) ? false : this.supportedContextTypes.stream().anyMatch(clz -> clz.isInstance(context));
	}

	// 【中文】Micrometer 回调：观测过程中出现异常时触发。
	@Override
	public void onError(Context context) {
		// 空值处理（第一重）：防御性判空
		if (context != null) {
			// 从观测上下文中取出链路追踪上下文
			TracingContext tracingContext = context.get(TracingContext.class);
			// 空值处理（第二重）：若未启用链路追踪则拿不到 TracingContext，
			// 此时<b>不记录任何日志</b>直接返回——这是本类值得注意的行为：
			// 它的定位是"带链路信息的错误日志"，没有链路信息就不属于它的职责范围。
			if (tracingContext != null) {
				// 关键技巧：用 try-with-resources 把出错时的 Span 恢复为当前 Span，
				// 使 errorConsumer 内部打日志时能自动带上 traceId/spanId；
				// 作用域结束时自动还原原有上下文，不会污染后续调用。
				try (var val = this.tracer.withSpan(tracingContext.getSpan())) {
					// 执行外部注入的错误处理策略（默认为打印 error 日志）
					this.errorConsumer.accept(context);
				}
			}
		}
	}

}
