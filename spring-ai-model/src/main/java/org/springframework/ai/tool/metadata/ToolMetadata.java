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

package org.springframework.ai.tool.metadata;

import java.lang.reflect.Method;

import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;

/**
 * Metadata about a tool specification and execution.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@code ToolMetadata}：工具的「执行期元数据」，描述框架该<b>如何处理</b>这个工具，
 * 而不是描述工具本身能做什么。
 *
 * <p>
 * <b>与 {@code ToolDefinition} 的区别（重要）：</b>
 * </p>
 * <ul>
 * <li>{@code ToolDefinition} —— 面向<b>模型</b>：名称/描述/入参 schema，会发给 LLM。</li>
 * <li>{@code ToolMetadata} —— 面向<b>框架</b>：控制调用行为的开关，不发给 LLM。</li>
 * </ul>
 *
 * <p>
 * <b>关键属性：</b>目前只有 {@link #returnDirect()} 一项，表示工具执行结果是否直接返回给调用方。
 * 接口给了 {@code false} 的默认实现，因此实现类可以完全不覆写。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * // 方式一：手动构建
 * ToolMetadata metadata = ToolMetadata.builder().returnDirect(true).build();
 *
 * // 方式二：从带 @Tool 注解的方法自动解析（最常用）
 * ToolMetadata metadata = ToolMetadata.from(method);
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see DefaultToolMetadata
 * @see org.springframework.ai.tool.definition.ToolDefinition
 */
public interface ToolMetadata {

	/**
	 * Whether the tool result should be returned directly or passed back to the model.
	 */
	// 中文：结果是否"直接返回"。
	// false（默认）：结果回灌给模型，由模型继续生成最终回答（多轮 tool-calling 的常规流程）；
	// true：结果直接返回给调用方，终止本轮与模型的交互。
	// 这里使用 default 方法提供默认值，实现类无需强制覆写。
	default boolean returnDirect() {
		return false;
	}

	/**
	 * Create a default {@link ToolMetadata} builder.
	 */
	// 中文：静态工厂，返回默认实现的 Builder，屏蔽具体实现类。
	static DefaultToolMetadata.Builder builder() {
		return DefaultToolMetadata.builder();
	}

	/**
	 * Create a default {@link ToolMetadata} instance from a {@link Method}.
	 */
	// 中文：便捷工厂 —— 直接从一个 Java 方法解析出元数据。
	// 内部通过 ToolUtils.getToolReturnDirect(method) 读取方法上 @Tool 注解的 returnDirect 属性；
	// 若方法没有 @Tool 注解则回退为 false。
	static ToolMetadata from(Method method) {
		// 中文：入参非空校验，快速失败，避免后续反射时抛出难以定位的 NPE。
		Assert.notNull(method, "method cannot be null");
		return DefaultToolMetadata.builder().returnDirect(ToolUtils.getToolReturnDirect(method)).build();
	}

}
