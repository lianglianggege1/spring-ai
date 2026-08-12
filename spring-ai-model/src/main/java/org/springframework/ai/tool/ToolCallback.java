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

package org.springframework.ai.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Represents a tool whose execution can be triggered by an AI model.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具回调（ToolCallback）：表示一个「可由 AI 模型触发执行」的工具，是 Spring AI
 * 工具调用（Tool Calling / Function Calling）体系中最核心的抽象。
 *
 * <p>
 * 一次典型的工具调用流程为：
 * <ol>
 * <li>把 {@link #getToolDefinition()} 描述的工具信息（名称、描述、入参 JSON Schema）随请求发送给模型；</li>
 * <li>模型决定调用某个工具，并返回该工具的名称与 JSON 格式的入参字符串；</li>
 * <li>框架据此找到对应的 ToolCallback，调用 {@link #call(String, ToolContext)} 执行；</li>
 * <li>把返回的字符串结果作为「工具消息」回传给模型，模型据此继续生成回答。</li>
 * </ol>
 *
 * <p>
 * 关键成员：
 * <ul>
 * <li>{@code logger}：接口级常量日志器，供默认方法输出提示（接口字段隐式为 public static final）。</li>
 * <li>{@link #getToolDefinition()}：必须实现，提供给模型「何时以及如何调用」的元信息。</li>
 * <li>{@link #getToolMetadata()}：可选，提供框架侧的处理策略（如结果是否直接返回给用户）。</li>
 * <li>{@link #call(String)}：必须实现，真正的执行逻辑。</li>
 * <li>{@link #call(String, ToolContext)}：可选重写，需要访问上下文（非模型可见的额外数据）时使用。</li>
 * </ul>
 *
 * <p>
 * 常见实现：基于 Lambda/Function 的 {@code FunctionToolCallback}、基于 {@code @Tool} 注解方法的
 * {@code MethodToolCallback}。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ToolCallback {

	// 接口中的字段隐式为 public static final，此处为所有实现类共享的日志器
	Log logger = LogFactory.getLog(ToolCallback.class);

	/**
	 * Definition used by the AI model to determine when and how to call the tool.
	 */
	/**
	 * 【中文说明】返回工具定义：包含工具名称、功能描述与入参的 JSON Schema。 这些信息会被序列化后随请求发送给模型，模型据此判断「何时调用、如何传参」。
	 * @return 工具定义，不可为 null
	 */
	ToolDefinition getToolDefinition();

	/**
	 * Metadata providing additional information on how to handle the tool.
	 */
	/**
	 * 【中文说明】返回工具元数据：描述框架应「如何处理」该工具的附加信息， 例如 {@code returnDirect}（结果是否跳过模型直接返回给调用方）。
	 * <p>
	 * 默认实现返回一份全默认值的元数据，实现类通常无需重写。
	 * @return 工具元数据，不可为 null
	 */
	default ToolMetadata getToolMetadata() {
		// 默认元数据：returnDirect = false，即结果仍要回传给模型继续推理
		return ToolMetadata.builder().build();
	}

	/**
	 * Execute tool with the given input and return the result to send back to the AI
	 * model.
	 */
	/**
	 * 【中文说明】执行工具的核心方法。
	 * @param toolInput 模型生成的入参，通常是符合入参 Schema 的 JSON 字符串
	 * @return 执行结果的字符串形式（一般为 JSON），将作为工具消息回传给模型
	 */
	String call(String toolInput);

	/**
	 * Execute tool with the given input and context, and return the result to send back
	 * to the AI model.
	 */
	/**
	 * 【中文说明】带工具上下文的执行方法。
	 * <p>
	 * {@link ToolContext} 用于携带「不希望暴露给模型」的额外数据（如当前登录用户、租户 ID、数据库连接等）， 由应用代码在发起请求时显式传入。
	 * <p>
	 * 注意：默认实现<b>会忽略</b> toolContext，直接委托给 {@link #call(String)}。 若工具确实需要使用上下文，实现类<b>必须重写本方法</b>。
	 * 为避免「传了上下文却静默失效」的坑，这里在检测到非空上下文时会打印一条 INFO 提示。
	 * @param toolInput 模型生成的入参 JSON 字符串
	 * @param toolContext 工具上下文，可为 null
	 * @return 执行结果的字符串形式
	 */
	default String call(String toolInput, @Nullable ToolContext toolContext) {
		// 上下文非空且确实携带了数据，说明调用方期望用到它，但默认实现并不会使用 —— 给出告警提示
		if (toolContext != null && !toolContext.getContext().isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("By default the tool context is not used,  "
						+ "override the method 'call(String toolInput, ToolContext toolcontext)' to support the use of tool context."
						+ "Review the ToolCallback implementation for " + getToolDefinition().name());
			}
		}
		// 默认行为：退化为不带上下文的调用
		return call(toolInput);
	}

}
