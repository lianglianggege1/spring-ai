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

package org.springframework.ai.tool.augment;

import java.util.Arrays;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * @author Christian Tzolov
 */
/**
 * 【中文说明】{@code AugmentedToolCallbackProvider}：{@link ToolCallbackProvider} 的<b>装饰器</b>，
 * 把被委托 provider 产出的<b>每一个</b> {@link ToolCallback} 都包装成 {@link AugmentedToolCallback}。
 *
 * <p>
 * <b>用途：</b>批量为一组工具统一追加相同的「增强参数」。例如给所有工具都加上一个
 * 「调用理由」字段，让模型每次调用时都必须说明动机，业务侧统一收集用于审计或可观测。
 * </p>
 *
 * <p>
 * <b>关键字段：</b>
 * </p>
 * <ul>
 * <li>{@code delegate} —— 被装饰的原始 provider，工具的真正来源</li>
 * <li>{@code argumentType} —— 增强参数的 record 类型</li>
 * <li>{@code argumentConsumer} —— 增强参数的消费回调</li>
 * <li>{@code removeExtraArgumentsAfterProcessing} —— 处理后是否从入参中剔除增强字段</li>
 * </ul>
 *
 * <p>
 * <b>两个构造器：</b>一个接收现成的 {@code ToolCallbackProvider}；
 * 另一个接收普通业务对象，内部先用 {@code MethodToolCallbackProvider} 扫描其 {@code @Tool}
 * 方法，再走同一套装饰逻辑（构造器链式委托）。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * var provider = AugmentedToolCallbackProvider.<Reasoning>builder()
 *         .toolObject(new WeatherTools())
 *         .argumentType(Reasoning.class)
 *         .argumentConsumer(event -> log.info("理由: {}", event.arguments()))
 *         .build();
 * }</pre>
 *
 * @author Christian Tzolov
 * @see AugmentedToolCallback
 */
public class AugmentedToolCallbackProvider<T extends Record> implements ToolCallbackProvider {

	// 中文：被装饰的原始 provider，工具回调的真正来源。
	private final ToolCallbackProvider delegate;

	// 中文：处理后是否从入参 JSON 中剔除增强字段。
	private final boolean removeExtraArgumentsAfterProcessing;

	// 中文：增强参数的消费回调，会被传递给每一个 AugmentedToolCallback。
	private final Consumer<AugmentedArgumentEvent<T>> argumentConsumer;

	// 中文：增强参数的 record 类型。
	private final Class<T> argumentType;

	// 中文：便捷构造器 —— 直接传入带 @Tool 注解的业务对象。
	// 内部先用 MethodToolCallbackProvider 扫描出工具，再委托给下面的主构造器（this(...) 链式调用）。
	public AugmentedToolCallbackProvider(Object toolObject, Class<T> argumentType,
			Consumer<AugmentedArgumentEvent<T>> argumentConsumer, boolean removeExtraArgumentsAfterProcessing) {
		this(MethodToolCallbackProvider.builder().toolObjects(toolObject).build(), argumentType, argumentConsumer,
				removeExtraArgumentsAfterProcessing);
	}

	// 中文：主构造器 —— 传入现成的 provider 作为委托对象。
	public AugmentedToolCallbackProvider(ToolCallbackProvider delegate, Class<T> argumentType,
			Consumer<AugmentedArgumentEvent<T>> argumentConsumer, boolean removeExtraArgumentsAfterProcessing) {
		this.delegate = delegate;
		this.argumentType = argumentType;
		this.argumentConsumer = argumentConsumer;
		this.removeExtraArgumentsAfterProcessing = removeExtraArgumentsAfterProcessing;
	}

	// 中文：核心方法 —— 取出被委托 provider 的全部工具，逐个包装成 AugmentedToolCallback。
	// 所有工具共享同一份 argumentType / argumentConsumer 配置，实现"批量统一增强"。
	@Override
	public ToolCallback[] getToolCallbacks() {

		return Arrays.stream(this.delegate.getToolCallbacks())
			.map(toolCallback -> new AugmentedToolCallback<T>(toolCallback, this.argumentType, this.argumentConsumer,
					this.removeExtraArgumentsAfterProcessing))
			.toArray(ToolCallback[]::new);

	}

	/**
	 * Creates a new builder instance
	 * @param <T> the argument type
	 * @return a new builder
	 */
	// 中文：静态泛型工厂方法。调用时通常需显式指定类型参数，
	// 如 AugmentedToolCallbackProvider.<Reasoning>builder()，否则 T 无法被推断。
	public static <T extends Record> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Builder for {@link AugmentedToolCallbackProvider}.
	 */
	/**
	 * 【中文说明】{@code AugmentedToolCallbackProvider} 的构建器。
	 *
	 * <p>
	 * <b>必填项：</b>{@code argumentType}、{@code argumentConsumer}。
	 * </p>
	 *
	 * <p>
	 * <b>互斥约束（重要）：</b>{@code delegate} 与 {@code toolObject} <b>二选一，且必须选其一</b>；
	 * 同时设置或都不设置都会在 {@code build()} 时抛出 {@code IllegalStateException}。
	 * </p>
	 *
	 * <p>
	 * 注意 {@code removeExtraArgumentsAfterProcessing} 在 Builder 中默认为 {@code true}，
	 * 与 {@code AugmentedToolCallback} 字段声明处的默认值 {@code false} 不同 ——
	 * 走 Builder 路径时默认会剔除增强字段。
	 * </p>
	 */
	public static class Builder<T extends Record> {

		// 中文：被委托的 provider，与 toolObject 互斥。
		private @Nullable ToolCallbackProvider delegate;

		// 中文：注意这里默认是 true（剔除增强字段），与 AugmentedToolCallback 字段默认的 false 不同。
		private boolean removeExtraArgumentsAfterProcessing = true;

		// 中文：增强参数消费回调，必填。
		private @Nullable Consumer<AugmentedArgumentEvent<T>> argumentConsumer;

		// 中文：增强参数 record 类型，必填。
		private @Nullable Class<T> argumentType;

		// 中文：业务工具对象，与 delegate 互斥。
		private @Nullable Object toolObject;

		/**
		 * Sets the delegate ToolCallbackProvider
		 * @param delegate the delegate provider
		 * @return this builder
		 */
		// 中文：设置被委托的 provider。与 toolObject 互斥，二者只能设置其一。
		public Builder<T> delegate(ToolCallbackProvider delegate) {
			this.delegate = delegate;
			return this;
		}

		/**
		 * Sets the tool object (alternative to delegate)
		 * @param toolObject the tool object
		 * @return this builder
		 */
		// 中文：设置业务工具对象（内部会自动用 MethodToolCallbackProvider 扫描其 @Tool 方法）。
		// 与 delegate 互斥，是"我只有一个普通业务类"场景下的便捷入口。
		public Builder<T> toolObject(Object toolObject) {
			this.toolObject = toolObject;
			return this;
		}

		/**
		 * Sets the argument type
		 * @param argumentType the class of the argument type
		 * @return this builder
		 */
		// 中文：设置增强参数的 record 类型，必填。其字段将被合并进每个工具的 inputSchema。
		public Builder<T> argumentType(Class<T> argumentType) {
			this.argumentType = argumentType;
			return this;
		}

		/**
		 * Sets the argument consumer
		 * @param argumentConsumer the consumer for arguments
		 * @return this builder
		 */
		// 中文：设置增强参数的消费回调，必填。每次工具被调用时都会收到一个 AugmentedArgumentEvent。
		public Builder<T> argumentConsumer(Consumer<AugmentedArgumentEvent<T>> argumentConsumer) {
			this.argumentConsumer = argumentConsumer;
			return this;
		}

		/**
		 * Sets whether to remove extra arguments after processing
		 * @param removeExtraArgumentsAfterProcessing true to remove extra arguments
		 * @return this builder
		 */
		// 中文：设置消费完增强参数后，是否把这些字段从入参 JSON 中剔除再转发给原工具。
		// 本 Builder 默认为 true（剔除），可显式设为 false 让原工具也能看到这些字段。
		public Builder<T> removeExtraArgumentsAfterProcessing(boolean removeExtraArgumentsAfterProcessing) {
			this.removeExtraArgumentsAfterProcessing = removeExtraArgumentsAfterProcessing;
			return this;
		}

		/**
		 * Builds the {@link AugmentedToolCallbackProvider} instance.
		 * @return the built instance
		 * @throws IllegalStateException if required fields are not set
		 */
		// 中文：完成构建。这里集中做了"必填校验 + 互斥校验"两类检查。
		public AugmentedToolCallbackProvider<T> build() {
			// 中文：必填校验一 —— 没有 argumentType 就无法生成增强字段的 schema。
			if (this.argumentType == null) {
				throw new IllegalStateException("argumentType is required");
			}
			// 中文：必填校验二 —— 没有消费者，增强参数提取出来也无处可用。
			if (this.argumentConsumer == null) {
				throw new IllegalStateException("argumentConsumer is required");
			}

			// 中文：互斥校验（其一）—— 两者都设置时语义冲突，无法确定以哪个为准，直接报错。
			if (this.delegate != null && this.toolObject != null) {
				throw new IllegalStateException("Cannot set both delegate and toolObject");
			}

			// 中文：互斥校验（其二）—— 两者都不设置则没有任何工具来源，同样报错。
			// 上下两段合起来构成严格的"恰好二选一"约束。
			if (this.delegate == null && this.toolObject == null) {
				throw new IllegalStateException("Either delegate or toolObject must be set");
			}

			// 中文：分支一 —— 走业务对象入口，内部会自动扫描 @Tool 方法。
			if (this.toolObject != null) {
				return new AugmentedToolCallbackProvider<>(this.toolObject, this.argumentType, this.argumentConsumer,
						this.removeExtraArgumentsAfterProcessing);
			}
			// 中文：分支二 —— 走现成 provider 入口。
			// 这里的 if 判断逻辑上是冗余的（前面校验已保证二者恰有其一非空），
			// 但静态空值检查工具 NullAway 无法推导出这一点，故补上以消除告警（源码注释亦如此说明）。
			else if (this.delegate != null) { // Redundant if condition to please NullAway
				return new AugmentedToolCallbackProvider<>(this.delegate, this.argumentType, this.argumentConsumer,
						this.removeExtraArgumentsAfterProcessing);
			}
			// 中文：理论上不可达的兜底分支，同样是为了让编译器/静态分析确信所有路径都有返回值。
			else {
				throw new IllegalStateException();
			}
		}

	}

}
