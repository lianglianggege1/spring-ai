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

package org.springframework.ai.chat.prompt;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Implementation of {@link ChatOptions.Builder} to create {@link DefaultChatOptions}.
 */
/**
 * 【中文说明】{@link ChatOptions.Builder} 的默认实现，用于构建 {@link DefaultChatOptions}。
 *
 * <p>
 * 类声明中的 {@code <B extends DefaultChatOptionsBuilder<B>>} 延续了接口的**递归泛型**
 * 设计，配合 {@link #self()} 方法，使各厂商的子类 Builder 在调用父类方法后仍能返回子类类型，
 * 从而保证链式调用不"退化"。
 *
 * <p>
 * 关键字段：8 个与 ChatOptions 对应的参数，均为 {@code protected}——刻意放宽可见性，
 * 方便子类直接读写以及 {@link #combineWith(ChatOptions.Builder)} 访问另一个实例的字段。
 *
 * <p>
 * 使用注意：Builder 非线程安全；{@code stopSequences} 在 setter 中做了拷贝，其余为直接赋值。
 */
public class DefaultChatOptionsBuilder<B extends DefaultChatOptionsBuilder<B>> implements ChatOptions.Builder<B> {

	// 【中文】以下 8 个字段均为 protected，供子类与 combineWith 直接访问；初始为 null 表示"未设置"。
	protected @Nullable String model;

	protected @Nullable Double frequencyPenalty;

	protected @Nullable Integer maxTokens;

	protected @Nullable Double presencePenalty;

	protected @Nullable List<String> stopSequences;

	protected @Nullable Double temperature;

	protected @Nullable Integer topK;

	protected @Nullable Double topP;

	// 【中文】公开的无参构造器，子类可通过 super() 隐式调用。
	public DefaultChatOptionsBuilder() {
	}

	// 【中文】克隆构建器，便于从一份基础配置派生多份变体。
	@Override
	public B clone() {
		try {
			// 【中文】先用 Object#clone 做**浅拷贝**（逐字段复制引用）。
			B copy = (B) super.clone();
			// 【中文】关键补救：浅拷贝会让新旧对象共享同一个 stopSequences 列表，
			// 因此这里单独为它新建 ArrayList，避免改动副本时影响原对象（其余字段都是不可变类型，无需处理）。
			copy.stopSequences = this.stopSequences == null ? null : new ArrayList<>(this.stopSequences);
			return copy;
		}
		catch (CloneNotSupportedException e) {
			// 【中文】本类实现了 Cloneable（继承自 ChatOptions.Builder），理论上不会走到这里；
			// 包装成非受检异常抛出，只是为了满足编译器对受检异常的要求。
			throw new RuntimeException(e);
		}
	}

	// 【中文】递归泛型的配套方法：把 this 强转为子类类型 B 返回。
	// 所有配置方法都 return self()，从而让子类链式调用时保持子类类型。
	// 转换的安全性依赖"子类正确地把自身作为 B 传入"这一约定，编译器无法校验，故加注解抑制告警。
	@SuppressWarnings("unchecked")
	protected B self() {
		return (B) this;
	}

	// 【中文】以下各 setter 结构一致：赋值后 return self() 以支持链式调用；
	// 允许传 null，表示"取消设置该参数"。
	@Override
	public B model(@Nullable String model) {
		this.model = model;
		return self();
	}

	@Override
	public B frequencyPenalty(@Nullable Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
		return self();
	}

	@Override
	public B maxTokens(@Nullable Integer maxTokens) {
		this.maxTokens = maxTokens;
		return self();
	}

	@Override
	public B presencePenalty(@Nullable Double presencePenalty) {
		this.presencePenalty = presencePenalty;
		return self();
	}

	// 【中文】设置停止序列。注意参数名是 stop 而字段名是 stopSequences。
	@Override
	public B stopSequences(@Nullable List<String> stop) {
		// 【中文】非 null 时做防御性拷贝，防止调用方后续修改传入的列表影响本构建器；
		// 传 null 则直接清空该配置。
		if (stop != null) {
			this.stopSequences = new ArrayList<>(stop);
		}
		else {
			this.stopSequences = null;
		}
		return self();
	}

	@Override
	public B temperature(@Nullable Double temperature) {
		this.temperature = temperature;
		return self();
	}

	@Override
	public B topK(@Nullable Integer topK) {
		this.topK = topK;
		return self();
	}

	@Override
	public B topP(@Nullable Double topP) {
		this.topP = topP;
		return self();
	}

	/**
	 * 【中文说明】把另一个构建器 {@code other} 的配置合并进当前构建器。
	 *
	 * <p>
	 * 合并规则：<b>other 中非 null 的值覆盖当前值</b>，other 中为 null 的项则保留当前值。
	 * 典型用途是"运行时参数覆盖全局默认参数"。
	 *
	 * <p>
	 * 特别注意 {@code stopSequences} 的规则与其它字段**不同**：它不是覆盖而是**追加合并**
	 * （两个列表拼接），详见方法内注释。
	 */
	@Override
	public B combineWith(ChatOptions.Builder<?> other) {
		// 【中文】类型守卫：只有当 other 也是 DefaultChatOptionsBuilder 时才能访问其字段进行合并；
		// 否则（如完全自定义的 Builder 实现）静默跳过、不做任何合并。
		if (other instanceof DefaultChatOptionsBuilder<?> that) {
			// 【中文】以下每个字段都遵循"非 null 才覆盖"的模式。
			if (that.model != null) {
				this.model = that.model;
			}
			if (that.frequencyPenalty != null) {
				this.frequencyPenalty = that.frequencyPenalty;
			}
			if (that.maxTokens != null) {
				this.maxTokens = that.maxTokens;
			}
			if (that.presencePenalty != null) {
				this.presencePenalty = that.presencePenalty;
			}
			// 【中文】stopSequences 是唯一的例外：采用"合并"而非"覆盖"策略。
			// 理由是停止序列本质上是一组约束条件，双方的约束应同时生效。
			if (that.stopSequences != null) {
				if (this.stopSequences == null) {
					// 【中文】当前没有值：直接拷贝一份对方的列表。
					this.stopSequences = new ArrayList<>(that.stopSequences);
				}
				else {
					// 【中文】双方都有值：新建列表把两边拼起来，而不是原地 addAll——
					// 因为 this.stopSequences 可能是不可变列表，原地修改会抛异常。
					// 注意此处不去重，重复的停止词会同时保留。
					List<String> merged = new ArrayList<>(this.stopSequences);
					merged.addAll(that.stopSequences);
					this.stopSequences = merged;
				}
			}
			if (that.temperature != null) {
				this.temperature = that.temperature;
			}
			if (that.topK != null) {
				this.topK = that.topK;
			}
			if (that.topP != null) {
				this.topP = that.topP;
			}
		}
		return self();
	}

	// 【中文】完成构建，生成不可变的 DefaultChatOptions。
	// 注意本方法未加 @Override（接口中确有 build()，此处属于可省略注解的情况），也不做任何参数校验——
	// 所有参数都是可选的，全部为 null 时表示"完全使用厂商默认配置"。
	public ChatOptions build() {
		return new DefaultChatOptions(this.model, this.frequencyPenalty, this.maxTokens, this.presencePenalty,
				this.stopSequences, this.temperature, this.topK, this.topP);
	}

}
