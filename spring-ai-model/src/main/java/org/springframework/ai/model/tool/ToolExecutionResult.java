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
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.Generation;

/**
 * The result of a tool execution.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】一次工具执行的结果封装。
 *
 * <p>
 * 关键内容有两块：
 * <ul>
 * <li>{@link #conversationHistory()}：执行工具后<b>更新过的完整会话历史</b>，
 * 末尾依次追加了模型的 AssistantMessage（含 tool_calls）和工具返回的 ToolResponseMessage；
 * ChatModel 会拿它发起下一轮请求。</li>
 * <li>{@link #returnDirect()}：是否把工具结果<b>直接返回给调用方</b>，而不再回传给大模型总结。</li>
 * </ul>
 *
 * <p>
 * 三个常量用于 returnDirect 场景下构造 {@link Generation} 的元数据：{@code FINISH_REASON}
 * 作为结束原因标记，{@code METADATA_TOOL_ID} / {@code METADATA_TOOL_NAME}
 * 则把工具调用 ID 和工具名写入生成结果的元数据，方便调用方追溯。
 *
 * <p>
 * 默认实现为 {@link DefaultToolExecutionResult}。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Thomas Vitale；{@code @since} 1.0.0。
 */
public interface ToolExecutionResult {

	// 【中文说明】returnDirect 场景下写入 ChatGenerationMetadata 的结束原因标记
	String FINISH_REASON = "returnDirect";

	// 【中文说明】元数据 key：本次工具调用的 ID（与模型返回的 tool_call id 对应）
	String METADATA_TOOL_ID = "toolId";

	// 【中文说明】元数据 key：被调用的工具名称
	String METADATA_TOOL_NAME = "toolName";

	/**
	 * The history of messages exchanged during the conversation, including the tool
	 * execution result.
	 */
	/**
	 * 【中文说明】返回包含工具执行结果在内的完整会话历史。
	 *
	 * <p>
	 * 顺序为：原有历史消息 → AssistantMessage（携带 tool_calls）→ ToolResponseMessage（工具返回值）。
	 * @return 更新后的消息列表
	 */
	List<Message> conversationHistory();

	/**
	 * Whether the tool execution result should be returned directly or passed back to the
	 * model.
	 */
	/**
	 * 【中文说明】工具结果是否直接返回给调用方。
	 *
	 * <p>
	 * {@code true} 表示跳过「再问一次大模型」的环节，直接把工具输出当作最终结果；
	 * {@code false}（默认）表示把工具结果回传给模型，由模型继续生成自然语言回复。
	 * @return 默认返回 {@code false}
	 */
	default boolean returnDirect() {
		return false;
	}

	/**
	 * Create a default {@link ToolExecutionResult} builder.
	 */
	/**
	 * 【中文说明】获取默认实现 {@link DefaultToolExecutionResult} 的 Builder。
	 */
	static DefaultToolExecutionResult.Builder builder() {
		return DefaultToolExecutionResult.builder();
	}

	/**
	 * Build a list of {@link Generation} from the tool execution result, useful for
	 * sending the tool execution result to the client directly.
	 */
	/**
	 * 【中文说明】把工具执行结果转换成 {@link Generation} 列表，主要服务于 returnDirect 场景。
	 *
	 * <p>
	 * 由于此时不再让大模型生成回复，就需要「伪造」一份生成结果返回给客户端：
	 * 取会话历史最后一条 ToolResponseMessage，将其中每个工具响应包装为一条
	 * AssistantMessage，并在元数据里带上 toolId / toolName 及 {@code returnDirect} 结束原因。
	 * @param toolExecutionResult 工具执行结果
	 * @return 生成结果列表；若最后一条消息不是 ToolResponseMessage 则返回空列表
	 */
	static List<Generation> buildGenerations(ToolExecutionResult toolExecutionResult) {
		List<Message> conversationHistory = toolExecutionResult.conversationHistory();
		List<Generation> generations = new ArrayList<>();
		// 只有当会话历史的最后一条是工具响应消息时才有内容可转换（instanceof 模式匹配同时完成判空与转型）
		if (conversationHistory
			.get(conversationHistory.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
			toolResponseMessage.getResponses().forEach(response -> {
				// 把工具的原始返回值当作助手消息的文本内容
				AssistantMessage assistantMessage = new AssistantMessage(response.responseData());
				// 元数据中记录工具 ID、工具名，并标记结束原因为 returnDirect
				Generation generation = new Generation(assistantMessage,
						ChatGenerationMetadata.builder()
							.metadata(METADATA_TOOL_ID, response.id())
							.metadata(METADATA_TOOL_NAME, response.name())
							.finishReason(FINISH_REASON)
							.build());
				generations.add(generation);
			});
		}
		return generations;
	}

}
