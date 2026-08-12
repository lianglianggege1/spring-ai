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

package org.springframework.ai.model.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptionsBuilder;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ToolCallingChatOptions}.
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolCallingChatOptions} 的默认实现，也是各厂商专属选项类的参考模板。
 *
 * <p>
 * 它继承 {@link DefaultChatOptions} 复用通用采样参数（model / temperature / maxTokens 等），
 * 并新增两个关键字段：
 * <ul>
 * <li>{@code toolCallbacks}：注册的工具回调列表；</li>
 * <li>{@code toolContext}：工具执行时透传的上下文数据。</li>
 * </ul>
 *
 * <p>
 * <b>不可变性设计</b>：两个字段均为 {@code final}，且在构造器中用 {@code List.copyOf} /
 * {@code Map.copyOf} 做了防御性拷贝，因此对象一旦创建就无法被外部篡改；
 * 需要修改时应通过 {@link #mutate()} 生成新对象。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ToolCallingChatOptions options = DefaultToolCallingChatOptions.builder()
 *         .temperature(0.7)
 *         .toolCallbacks(callbackA, callbackB)
 *         .toolContext("tenantId", "T001")
 *         .build();
 * }</pre>
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Thomas Vitale、Sebastien Deleuze、Christian Tzolov；
 * {@code @since} 1.0.0。
 */
public class DefaultToolCallingChatOptions extends DefaultChatOptions implements ToolCallingChatOptions {

	// 【中文说明】工具回调列表，构造时已转为不可变列表，可能为 null
	private final @Nullable List<ToolCallback> toolCallbacks;

	// 【中文说明】工具上下文，构造时已转为不可变 Map，可能为 null
	private final @Nullable Map<String, Object> toolContext;

	/**
	 * 【中文说明】受保护的全参构造器，由 Builder 调用；子类可复用以扩展自己的字段。
	 *
	 * <p>
	 * 注意此处对集合参数做了<b>防御性拷贝</b>（copyOf），既保证不可变，也隔离了外部后续修改。
	 */
	protected DefaultToolCallingChatOptions(@Nullable List<ToolCallback> toolCallbacks,
			@Nullable Map<String, Object> toolContext, @Nullable String model, @Nullable Double frequencyPenalty,
			@Nullable Integer maxTokens, @Nullable Double presencePenalty, @Nullable List<String> stopSequences,
			@Nullable Double temperature, @Nullable Integer topK, @Nullable Double topP) {
		// 通用采样参数交由父类 DefaultChatOptions 保存
		super(model, frequencyPenalty, maxTokens, presencePenalty, stopSequences, temperature, topK, topP);
		// 空值处理：为 null 时保持 null，非 null 时拷贝为不可变集合
		this.toolCallbacks = (toolCallbacks != null ? List.copyOf(toolCallbacks) : null);
		this.toolContext = (toolContext != null ? Map.copyOf(toolContext) : null);
	}

	// 【中文说明】返回不可变的工具回调列表（可能为 null）
	@Override
	public @Nullable List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	// 【中文说明】返回不可变的工具上下文（可能为 null）
	@Override
	public @Nullable Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	/**
	 * 【中文说明】把当前对象的所有字段回填到一个新的 Builder，实现「复制并修改」语义。
	 *
	 * <p>
	 * 由于本类不可变，任何改动都必须走 {@code mutate().xxx().build()} 生成新实例。
	 */
	@Override
	public ToolCallingChatOptions.Builder<?> mutate() {
		return DefaultToolCallingChatOptions.builder()
			.model(getModel())
			.frequencyPenalty(getFrequencyPenalty())
			.maxTokens(getMaxTokens())
			.presencePenalty(getPresencePenalty())
			.stopSequences(getStopSequences())
			.temperature(getTemperature())
			.topK(getTopK())
			.topP(getTopP())
			.toolCallbacks(getToolCallbacks())
			.toolContext(getToolContext());
	}

	/**
	 * 【中文说明】相等性判断：先比对运行时类型，再叠加父类（通用采样参数）的比较结果，
	 * 最后比较工具回调与工具上下文两个字段。
	 *
	 * <p>
	 * 这里用 {@code getClass() != o.getClass()} 而非 {@code instanceof}，
	 * 意味着子类实例与父类实例永远不相等，保证了 equals 的对称性。
	 */
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		// 严格类型比较：不同具体类型直接判定不等
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		// 先比较父类持有的通用采样参数
		if (!super.equals(o)) {
			return false;
		}
		DefaultToolCallingChatOptions that = (DefaultToolCallingChatOptions) o;
		return Objects.equals(this.toolCallbacks, that.toolCallbacks)
				&& Objects.equals(this.toolContext, that.toolContext);
	}

	// 【中文说明】哈希值需与 equals 保持一致，因此同样把父类 hashCode 纳入计算
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.toolCallbacks, this.toolContext);
	}

	// 【中文说明】创建 Builder 的静态工厂方法
	public static Builder<?> builder() {
		return new Builder<>();
	}

	/**
	 * Default implementation of {@link ToolCallingChatOptions.Builder}.
	 */
	/**
	 * 【中文说明】{@link ToolCallingChatOptions.Builder} 的默认实现。
	 *
	 * <p>
	 * 采用<b>递归泛型 {@code B extends Builder<B>}</b>，配合父类的 {@code self()} 方法
	 * 将 this 转型为 B 返回，使子类 Builder 扩展新方法后仍能与父类方法自由混合链式调用。
	 *
	 * <p>
	 * 需要特别注意各设置方法的语义差异：List 版 {@code toolCallbacks} 是<b>替换</b>，
	 * 可变参数版是<b>追加</b>；{@code toolContext(Map)} 是<b>合并</b>（putAll），传 null 才是清空。
	 */
	public static class Builder<B extends Builder<B>> extends DefaultChatOptionsBuilder<B>
			implements ToolCallingChatOptions.Builder<B> {

		// 【中文说明】构建过程中的可变工具回调列表
		protected @Nullable List<ToolCallback> toolCallbacks;

		// 【中文说明】构建过程中的可变工具上下文
		protected @Nullable Map<String, Object> toolContext;

		/**
		 * 【中文说明】深拷贝当前 Builder。
		 *
		 * <p>
		 * 对两个集合字段重新 new 了一份，属于<b>浅层元素、独立容器</b>的拷贝，
		 * 避免克隆出的 Builder 与原 Builder 共享同一个集合实例而相互影响。
		 */
		@Override
		public B clone() {
			B copy = super.clone();
			// 容器独立化；元素本身仍为共享引用
			copy.toolCallbacks = this.toolCallbacks == null ? null : new ArrayList<>(this.toolCallbacks);
			copy.toolContext = this.toolContext == null ? null : new HashMap<>(this.toolContext);
			return copy;
		}

		/**
		 * 【中文说明】以 List 形式设置工具回调：<b>整体替换</b>已有列表。
		 * @param toolCallbacks 新的工具回调列表；传 {@code null} 表示清空
		 */
		@Override
		public B toolCallbacks(@Nullable List<ToolCallback> toolCallbacks) {
			if (toolCallbacks != null) {
				// 拷贝一份可变列表，防止外部引用后续变更影响 Builder 内部状态
				this.toolCallbacks = new ArrayList<>(toolCallbacks);
			}
			else {
				// 显式传 null 表示清空
				this.toolCallbacks = null;
			}
			return self();
		}

		/**
		 * 【中文说明】以可变参数形式设置工具回调：<b>追加</b>到已有列表末尾。
		 *
		 * <p>
		 * 与 List 重载的「替换」语义不同，多次调用会不断累加，注意可能产生同名工具，
		 * 最终由 {@code ToolCallingChatOptions.validateToolCallbacks} 校验拦截。
		 */
		@Override
		public B toolCallbacks(ToolCallback... toolCallbacks) {
			// 参数校验：数组本身不允许为 null
			Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
			// 懒初始化，首次调用时创建列表
			if (this.toolCallbacks == null) {
				this.toolCallbacks = new ArrayList<>();
			}
			this.toolCallbacks.addAll(List.of(toolCallbacks));
			return self();
		}

		/**
		 * 【中文说明】批量设置工具上下文：非 null 时执行 {@code putAll} <b>合并</b>，null 时<b>清空</b>。
		 */
		@Override
		public B toolContext(@Nullable Map<String, Object> context) {
			if (context != null) {
				if (this.toolContext == null) {
					this.toolContext = new HashMap<>();
				}
				// 合并语义：同名 key 会被新值覆盖，其余 key 保留
				this.toolContext.putAll(context);
			}
			else {
				// 显式传 null 表示清空上下文
				this.toolContext = null;
			}
			return self();
		}

		/**
		 * 【中文说明】设置单个上下文键值对，key 必须为非空字符串、value 不可为 null。
		 */
		@Override
		public B toolContext(String key, Object value) {
			// 参数校验：key 需为非空白文本，value 不允许为 null
			Assert.hasText(key, "key cannot be null");
			Assert.notNull(value, "value cannot be null");
			if (this.toolContext == null) {
				this.toolContext = new HashMap<>();
			}
			this.toolContext.put(key, value);
			return self();
		}

		// 【中文说明】收口构建：把 Builder 中的可变状态交给不可变的选项对象
		@Override
		public ToolCallingChatOptions build() {
			return new DefaultToolCallingChatOptions(this.toolCallbacks, this.toolContext, this.model,
					this.frequencyPenalty, this.maxTokens, this.presencePenalty, this.stopSequences, this.temperature,
					this.topK, this.topP);
		}

		/**
		 * 【中文说明】把另一个 Builder 的配置合并进当前 Builder。
		 *
		 * <p>
		 * 合并规则：
		 * <ul>
		 * <li>先调用 {@code super.combineWith} 合并通用采样参数；</li>
		 * <li>只有当对方也是本 Builder 类型时（{@code instanceof} 模式匹配），才处理工具相关字段；</li>
		 * <li>{@code toolCallbacks} 走<b>列表拼接</b>（对方的追加在后）；</li>
		 * <li>{@code toolContext} 走<b>按 key 覆盖合并</b>，对方的值优先级更高。</li>
		 * </ul>
		 * 注意：对方字段为 null 时会跳过，即「未设置」不会覆盖掉当前已有的值。
		 */
		@Override
		public B combineWith(ChatOptions.Builder<?> other) {
			// 先合并父类负责的通用采样参数
			super.combineWith(other);
			// 类型不匹配时，仅保留父类的合并结果
			if (other instanceof Builder<?> that) {
				// 工具回调：拼接而非覆盖
				if (that.toolCallbacks != null) {
					if (this.toolCallbacks == null) {
						this.toolCallbacks = new ArrayList<>(that.toolCallbacks);
					}
					else {
						List<ToolCallback> merged = new ArrayList<>(this.toolCallbacks);
						merged.addAll(that.toolCallbacks);
						this.toolCallbacks = merged;
					}
				}
				// 工具上下文：同名 key 以对方（other）为准
				if (that.toolContext != null) {
					if (this.toolContext == null) {
						this.toolContext = new HashMap<>(that.toolContext);
					}
					else {
						Map<String, Object> merged = new HashMap<>(this.toolContext);
						merged.putAll(that.toolContext);
						this.toolContext = merged;
					}
				}
			}
			return self();
		}

	}

}
