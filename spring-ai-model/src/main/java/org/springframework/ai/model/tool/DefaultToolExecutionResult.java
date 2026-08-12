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

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ToolExecutionResult}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolExecutionResult} 的默认实现，使用 Java 16+ 的 <b>record</b> 定义。
 *
 * <p>
 * record 自动为两个组件生成了 final 字段、访问器方法（{@code conversationHistory()}、
 * {@code returnDirect()}）以及 equals / hashCode / toString，因此恰好满足
 * {@link ToolExecutionResult} 接口的方法签名，无需手写实现。
 *
 * <p>
 * 两个组件的含义：
 * <ul>
 * <li>{@code conversationHistory}：追加了 AssistantMessage 与 ToolResponseMessage 之后的完整会话历史；</li>
 * <li>{@code returnDirect}：工具结果是否直接返回调用方，不再回传给大模型。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code ToolExecutionResult.builder().conversationHistory(messages).returnDirect(true).build()}。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Thomas Vitale；{@code @since} 1.0.0。
 */
public record DefaultToolExecutionResult(List<Message> conversationHistory,
		boolean returnDirect) implements ToolExecutionResult {

	/**
	 * 【中文说明】record 的<b>紧凑构造器（compact constructor）</b>，用于集中做参数校验。
	 *
	 * <p>
	 * 这里校验会话历史既不能为 null，也不能包含 null 元素——因为后续会直接遍历该列表
	 * 构造下一轮 Prompt，提前失败可以避免难以定位的 NPE。
	 */
	public DefaultToolExecutionResult {
		// 参数校验：列表本身非空，且不含 null 元素
		Assert.notNull(conversationHistory, "conversationHistory cannot be null");
		Assert.noNullElements(conversationHistory, "conversationHistory cannot contain null elements");
	}

	// 【中文说明】创建 Builder 的静态工厂方法
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@link DefaultToolExecutionResult} 的 Builder。
	 *
	 * <p>
	 * 构造器为 private，只能通过 {@code DefaultToolExecutionResult.builder()} 或
	 * {@code ToolExecutionResult.builder()} 获取实例，从而收敛创建入口。
	 */
	public static final class Builder {

		// 【中文说明】会话历史，默认空列表（而非 null），保证 build() 时能通过校验
		private List<Message> conversationHistory = List.of();

		// 【中文说明】是否直接返回工具结果，基本类型默认 false
		private boolean returnDirect;

		// 【中文说明】私有构造器：强制通过静态 builder() 方法创建
		private Builder() {
		}

		// 【中文说明】设置执行工具后的完整会话历史
		public Builder conversationHistory(List<Message> conversationHistory) {
			this.conversationHistory = conversationHistory;
			return this;
		}

		// 【中文说明】设置是否将工具结果直接返回给调用方
		public Builder returnDirect(boolean returnDirect) {
			this.returnDirect = returnDirect;
			return this;
		}

		// 【中文说明】构建 record 实例，参数校验在紧凑构造器中完成
		public DefaultToolExecutionResult build() {
			return new DefaultToolExecutionResult(this.conversationHistory, this.returnDirect);
		}

	}

}
