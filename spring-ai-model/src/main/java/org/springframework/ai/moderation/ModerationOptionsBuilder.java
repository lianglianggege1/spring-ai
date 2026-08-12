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

package org.springframework.ai.moderation;

import org.jspecify.annotations.Nullable;

/**
 * A builder class for creating instances of ModerationOptions. Use the builder() method
 * to obtain a new instance of ModerationOptionsBuilder. Use the withModel() method to set
 * the model for moderation. Use the build() method to build the ModerationOptions
 * instance.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ModerationOptions} 的建造器（Builder 模式）。
 *
 * <p>
 * 使用方式：{@code ModerationOptionsBuilder.builder().model("text-moderation-latest").build();}
 *
 * <p>
 * 设计要点：
 * <ul>
 * <li>类声明为 {@code final} 且构造器私有，只能通过静态工厂 {@link #builder()} 创建，
 * 防止被继承或随意 new</li>
 * <li>真正的实现类 {@code ModerationModelOptionsImpl} 是私有内部类，对外只暴露
 * {@link ModerationOptions} 接口，隐藏实现细节</li>
 * <li>{@code build()} 直接返回内部持有的同一个 options 对象，因此<b>不要在 build 之后
 * 复用同一个 Builder 继续修改</b>，否则会影响已经构建出去的实例</li>
 * </ul>
 */
public final class ModerationOptionsBuilder {

	// 中文：被构建的目标对象。Builder 全程操作它，build() 时原样返回
	private final ModerationModelOptionsImpl options = new ModerationModelOptionsImpl();

	// 中文：私有构造器，强制走 builder() 静态工厂
	private ModerationOptionsBuilder() {

	}

	// 中文：创建建造器实例的唯一入口
	public static ModerationOptionsBuilder builder() {
		return new ModerationOptionsBuilder();
	}

	// 中文：设置审核模型名称；返回 this 以支持链式调用
	public ModerationOptionsBuilder model(String model) {
		this.options.setModel(model);
		return this;
	}

	// 中文：完成构建，以接口类型返回，调用方无法接触到内部实现类
	public ModerationOptions build() {
		return this.options;
	}

	/**
	 * 【中文说明】{@link ModerationOptions} 的私有内部实现类。
	 *
	 * <p>
	 * 仅供外层 Builder 使用，对包外完全不可见，这样将来调整字段不会破坏公开 API。
	 */
	private class ModerationModelOptionsImpl implements ModerationOptions {

		// 中文：审核模型名称，允许为 null（表示使用实现方默认模型）
		private @Nullable String model;

		@Override
		public @Nullable String getModel() {
			return this.model;
		}

		// 中文：包内可见的写入方法，由外层 Builder 的 model() 调用
		public void setModel(String model) {
			this.model = model;
		}

	}

}
