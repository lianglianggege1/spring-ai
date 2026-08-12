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

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Service responsible for managing the tool calling process for a chat model.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用流程的统一管理器，是 Spring AI 工具调用机制的<b>核心抽象</b>。
 *
 * <p>
 * 它把「工具调用」这件事拆成了两个职责清晰的阶段，供各家 ChatModel 复用：
 * <ol>
 * <li><b>调用前</b>：{@link #resolveToolDefinitions(ToolCallingChatOptions)} —— 从选项中解析出工具定义
 * （名称 / 描述 / 入参 Schema），由 ChatModel 转换成各厂商 API 所需的 tools 字段发给大模型。</li>
 * <li><b>调用后</b>：{@link #executeToolCalls(Prompt, ChatResponse)} —— 当模型返回 tool_calls 时，
 * 真正执行本地工具，并把结果拼回会话历史，供下一轮请求使用。</li>
 * </ol>
 *
 * <p>
 * 典型的多轮工具调用循环（由 ChatModel 内部驱动）大致如下：
 *
 * <pre>{@code
 * ChatResponse response = doCall(prompt);
 * while (response 中包含 tool_calls) {
 *     ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
 *     if (result.returnDirect()) { // 工具结果直接返回给调用方，不再问模型
 *         break;
 *     }
 *     prompt = new Prompt(result.conversationHistory(), prompt.getOptions());
 *     response = doCall(prompt);
 * }
 * }</pre>
 *
 * <p>
 * 默认实现为 {@link DefaultToolCallingManager}。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Thomas Vitale；{@code @since} 1.0.0。
 */
public interface ToolCallingManager {

	/**
	 * Resolve the tool definitions from the model's tool calling options.
	 */
	/**
	 * 【中文说明】从聊天选项中解析出工具定义列表。
	 *
	 * <p>
	 * 这一步只取「元信息」（工具名、描述、入参 JSON Schema），不涉及任何工具的实际执行；
	 * ChatModel 拿到后会将其序列化进请求体，告诉大模型「你有哪些工具可用」。
	 * @param chatOptions 携带工具配置的聊天选项
	 * @return 工具定义列表；未配置工具时为空列表
	 */
	List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions);

	/**
	 * Execute the tool calls requested by the model.
	 */
	/**
	 * 【中文说明】执行大模型在响应中请求的工具调用。
	 *
	 * <p>
	 * 实现需要：定位到对应的 ToolCallback → 执行 → 捕获并转换异常 → 把工具结果封装为
	 * ToolResponseMessage 追加到会话历史。
	 * @param prompt 本次请求的 Prompt（其中含历史消息与工具选项）
	 * @param chatResponse 模型返回的、包含 tool_calls 的响应
	 * @return 工具执行结果，含更新后的会话历史与 returnDirect 标记
	 */
	ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);

	/**
	 * Create a default {@link ToolCallingManager} builder.
	 */
	/**
	 * 【中文说明】获取默认实现 {@link DefaultToolCallingManager} 的 Builder。
	 */
	static DefaultToolCallingManager.Builder builder() {
		return DefaultToolCallingManager.builder();
	}

}
