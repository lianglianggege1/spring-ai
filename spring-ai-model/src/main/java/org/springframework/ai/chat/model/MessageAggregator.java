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

package org.springframework.ai.chat.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.PromptMetadata;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helper that for streaming chat responses, aggregate the chat response messages into a
 * single AssistantMessage. Job is performed in parallel to the chat response processing.
 *
 * @author Christian Tzolov
 * @author Alexandros Pappas
 * @author Thomas Vitale
 * @author Heonwoo Kim
 * @since 1.0.0
 */
/**
 * 【中文说明】MessageAggregator 是流式响应的"聚合器"：把 {@code Flux<ChatResponse>} 中一个个增量片段
 * 拼装成一条完整的 {@link AssistantMessage}，并在流结束时回调通知。
 *
 * <p>
 * 为什么需要它：流式调用时模型是逐 token 返回的，每个 ChatResponse 只带一小段文本；
 * 但可观测性（Observation）、日志、对话记忆（ChatMemory）等场景需要的是"完整的最终回复"。
 * 本类通过 Reactor 的钩子在不打断原始流的前提下，旁路完成聚合。
 *
 * <p>
 * 核心机制（关键点）：
 * <ul>
 * <li>不改变原始 Flux 的数据，只挂载 {@code doOnSubscribe} / {@code doOnNext} /
 * {@code doOnComplete} / {@code doOnError} 四个副作用钩子，因此聚合与下游消费是"并行"进行的，
 * 不会阻塞用户的流式输出。</li>
 * <li>用一组 {@link AtomicReference} 作为可变累加器。之所以不用普通局部变量，是因为 Lambda 要求
 * 捕获的局部变量必须是 final/effectively final，AtomicReference 提供了一个"可变的容器"。</li>
 * <li>订阅时（doOnSubscribe）重置所有累加器，完成时（doOnComplete）再次重置，
 * 保证同一个 Flux 被重复订阅时状态不串。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * return new MessageAggregator().aggregate(originalFlux, fullResponse -> {
 *     // 这里拿到的是拼接完成的完整响应，可用于记录日志/写入记忆
 * });
 * }</pre>
 */
public class MessageAggregator {

	private static final Log logger = LogFactory.getLog(MessageAggregator.class);

	// 中文说明：聚合入口方法。
	// 参数 fluxChatResponse：原始的流式响应；
	// 参数 onAggregationComplete：流正常结束后的回调，入参是"拼装好的完整 ChatResponse"。
	// 返回值：原始流本身（挂载了副作用钩子），下游订阅者感知不到聚合逻辑的存在。
	public Flux<ChatResponse> aggregate(Flux<ChatResponse> fluxChatResponse,
			Consumer<ChatResponse> onAggregationComplete) {

		// Assistant Message
		// 中文说明：以下一组 AtomicReference 是"累加器"。使用 AtomicReference 而非普通变量，
		// 是为了绕开 Lambda 只能捕获 final 变量的限制，同时提供跨线程可见性。
		// messageTextContentRef：累加全部文本内容（含思考过程）
		AtomicReference<StringBuilder> messageTextContentRef = new AtomicReference<>(new StringBuilder());
		// thoughtsRef：单独累加"思考过程"片段（推理模型的 thinking 内容）
		AtomicReference<StringBuilder> thoughtsRef = new AtomicReference<>(new StringBuilder());
		// outputWithoutThoughtsRef：单独累加"去掉思考过程后的正式回答"
		AtomicReference<StringBuilder> outputWithoutThoughtsRef = new AtomicReference<>(new StringBuilder());
		// messageMetadataMapRef：合并所有片段的消息级元数据（注意初始值为 null，在 doOnSubscribe 里才赋值）
		AtomicReference<Map<String, Object>> messageMetadataMapRef = new AtomicReference<>();
		// toolCallsRef：收集流式返回的工具调用
		AtomicReference<List<ToolCall>> toolCallsRef = new AtomicReference<>(new ArrayList<>());

		// ChatGeneration Metadata
		// 中文说明：生成级元数据（主要是 finishReason），取最后一次非空的值；初始为 NULL 空对象
		AtomicReference<ChatGenerationMetadata> generationMetadataRef = new AtomicReference<>(
				ChatGenerationMetadata.NULL);

		// Usage
		// 中文说明：token 用量三件套。多数厂商只在最后一个 chunk 里返回用量，故采用"非 0 才覆盖"的策略
		AtomicReference<Integer> metadataUsagePromptTokensRef = new AtomicReference<>(0);
		AtomicReference<Integer> metadataUsageGenerationTokensRef = new AtomicReference<>(0);
		AtomicReference<Integer> metadataUsageTotalTokensRef = new AtomicReference<>(0);

		// 中文说明：Prompt 元数据与限流信息，均以"空对象"作为初始值，避免后续判空
		AtomicReference<PromptMetadata> metadataPromptMetadataRef = new AtomicReference<>(PromptMetadata.empty());
		AtomicReference<RateLimit> metadataRateLimitRef = new AtomicReference<>(new EmptyRateLimit());

		// 中文说明：请求 id 与模型名，初始为空串
		AtomicReference<String> metadataIdRef = new AtomicReference<>("");
		AtomicReference<String> metadataModelRef = new AtomicReference<>("");

		// 中文说明：【钩子一】订阅时重置所有累加器。
		// 这一步很关键——Flux 可能被多次订阅，若不重置，上一次订阅残留的内容会污染本次结果。
		return fluxChatResponse.doOnSubscribe(subscription -> {
			messageTextContentRef.set(new StringBuilder());
			thoughtsRef.set(new StringBuilder());
			outputWithoutThoughtsRef.set(new StringBuilder());
			messageMetadataMapRef.set(new HashMap<>());
			toolCallsRef.set(new ArrayList<>());
			metadataIdRef.set("");
			metadataModelRef.set("");
			metadataUsagePromptTokensRef.set(0);
			metadataUsageGenerationTokensRef.set(0);
			metadataUsageTotalTokensRef.set(0);
			metadataPromptMetadataRef.set(PromptMetadata.empty());
			metadataRateLimitRef.set(new EmptyRateLimit());

		// 中文说明：【钩子二】每收到一个增量片段就累加一次。整段逻辑都是"有值才覆盖/追加"的防御式写法。
		}).doOnNext(chatResponse -> {

			if (chatResponse.getResult() != null) {
				// 中文说明：保存最近一次"有效"的生成元数据。
				// 双重判断：既排除 null，也排除 NULL 空对象——因为多数中间 chunk 的 finishReason 是空的，
				// 只有最后一个 chunk 才带真正的结束原因，这样能确保最终留下的是有意义的那个。
				if (chatResponse.getResult().getMetadata() != null
						&& chatResponse.getResult().getMetadata() != ChatGenerationMetadata.NULL) {
					generationMetadataRef.set(chatResponse.getResult().getMetadata());
				}
				if (chatResponse.getResult().getOutput().getText() != null) {
					// 中文说明：主文本累加——所有片段（含思考内容）都会拼进 messageTextContentRef
					messageTextContentRef.get().append(chatResponse.getResult().getOutput().getText());
					var metadata = chatResponse.getResult().getOutput().getMetadata();
					// 中文说明：分支处理"推理模型"的思考过程。
					// 若片段元数据里带 isThought 标记，则按标记把内容分流到 thoughts / outputWithoutThoughts 两个缓冲区，
					// 便于调用方区分"思考过程"与"最终答案"。没有该标记的模型则不做分流（两个缓冲区都为空）。
					if (metadata != null && metadata.containsKey("isThought")) {
						var isThought = Boolean.parseBoolean(metadata.get("isThought").toString());
						if (isThought) {
							thoughtsRef.get().append(chatResponse.getResult().getOutput().getText());
						}
						else {
							outputWithoutThoughtsRef.get().append(chatResponse.getResult().getOutput().getText());
						}
					}
				}
				// 中文说明：合并消息级元数据。putAll 意味着后来的片段会覆盖同名 key（后者优先）
				if (chatResponse.getResult().getOutput().getMetadata() != null) {
					messageMetadataMapRef.get().putAll(chatResponse.getResult().getOutput().getMetadata());
				}
				AssistantMessage outputMessage = chatResponse.getResult().getOutput();
				// 中文说明：收集工具调用；用 CollectionUtils.isEmpty 同时覆盖 null 与空集合两种情况
				if (!CollectionUtils.isEmpty(outputMessage.getToolCalls())) {
					toolCallsRef.get().addAll(outputMessage.getToolCalls());
				}

			}
			if (chatResponse.getMetadata() != null) {
				if (chatResponse.getMetadata().getUsage() != null) {
					Usage usage = chatResponse.getMetadata().getUsage();
					// 中文说明：token 用量采用"大于 0 才覆盖，否则保留旧值"的策略。
					// 原因：流式响应中绝大多数 chunk 的 usage 是 0 或缺省，只有最后一个 chunk 带真实用量，
					// 若无条件覆盖会把已获取的用量重置为 0。
					metadataUsagePromptTokensRef.set(
							usage.getPromptTokens() > 0 ? usage.getPromptTokens() : metadataUsagePromptTokensRef.get());
					metadataUsageGenerationTokensRef.set(usage.getCompletionTokens() > 0 ? usage.getCompletionTokens()
							: metadataUsageGenerationTokensRef.get());
					metadataUsageTotalTokensRef
						.set(usage.getTotalTokens() > 0 ? usage.getTotalTokens() : metadataUsageTotalTokensRef.get());
				}
				// 中文说明：PromptMetadata 用 iterator().hasNext() 判断"是否真的有内容"，
				// 空的 PromptMetadata 不覆盖已有值（同样是"有值才覆盖"原则）
				if (chatResponse.getMetadata().getPromptMetadata() != null
						&& chatResponse.getMetadata().getPromptMetadata().iterator().hasNext()) {
					metadataPromptMetadataRef.set(chatResponse.getMetadata().getPromptMetadata());
				}
				RateLimit incomingRateLimit = chatResponse.getMetadata().getRateLimit();
				// 中文说明：限流信息同理——排除 null 与 EmptyRateLimit 占位对象，只保留真实数据
				if (incomingRateLimit != null && !(incomingRateLimit instanceof EmptyRateLimit)) {
					metadataRateLimitRef.set(incomingRateLimit);
				}
				// 中文说明：id / model 用 StringUtils.hasText 判断（非 null、非空、非纯空白），有值才覆盖
				if (StringUtils.hasText(chatResponse.getMetadata().getId())) {
					metadataIdRef.set(chatResponse.getMetadata().getId());
				}
				if (StringUtils.hasText(chatResponse.getMetadata().getModel())) {
					metadataModelRef.set(chatResponse.getMetadata().getModel());
				}
				// 中文说明：兼容部分厂商把工具调用放在响应级 metadata 的 "toolCalls" 键里的情况。
				// 这里用 instanceof 做类型检查后再强转，@SuppressWarnings("unchecked") 是因为
				// 泛型擦除导致无法校验 List 内部元素类型，属于已知且可接受的非受检转换。
				Object toolCallsFromMetadata = chatResponse.getMetadata().get("toolCalls");
				if (toolCallsFromMetadata instanceof List) {
					@SuppressWarnings("unchecked")
					List<ToolCall> toolCallsList = (List<ToolCall>) toolCallsFromMetadata;
					toolCallsRef.get().addAll(toolCallsList);
				}

			}
		// 中文说明：【钩子三】流正常结束时，把累加结果组装成完整的 ChatResponse 并回调，最后清空状态。
		}).doOnComplete(() -> {

			// 中文说明：用累加得到的 token 数构造最终用量对象
			var usage = new DefaultUsage(metadataUsagePromptTokensRef.get(), metadataUsageGenerationTokensRef.get(),
					metadataUsageTotalTokensRef.get());

			// 中文说明：组装最终的响应级元数据
			var chatResponseMetadata = ChatResponseMetadata.builder()
				.id(metadataIdRef.get())
				.model(metadataModelRef.get())
				.rateLimit(metadataRateLimitRef.get())
				.usage(usage)
				.promptMetadata(metadataPromptMetadataRef.get())
				.build();

			AssistantMessage finalAssistantMessage;
			var messageMetadata = messageMetadataMapRef.get();
			// 中文说明：仅当确实收集到思考内容时，才把 thoughts / outputWithoutThoughts 写入消息元数据，
			// 避免给非推理模型的响应塞入无意义的空字段。
			if (!thoughtsRef.get().isEmpty()) {
				messageMetadata.put("thoughts", thoughtsRef.get().toString());
				messageMetadata.put("outputWithoutThoughts", outputWithoutThoughtsRef.get().toString());
			}
			List<ToolCall> collectedToolCalls = toolCallsRef.get();

			// 中文说明：分支构建最终消息——有工具调用时才调用 .toolCalls(...)。
			// 这里区分两个分支而非统一传入空集合，是为了让"无工具调用"的消息保持干净的默认状态。
			if (!CollectionUtils.isEmpty(collectedToolCalls)) {

				finalAssistantMessage = AssistantMessage.builder()
					.content(messageTextContentRef.get().toString())
					.properties(messageMetadata)
					.toolCalls(collectedToolCalls)
					.build();
			}
			else {
				finalAssistantMessage = AssistantMessage.builder()
					.content(messageTextContentRef.get().toString())
					.properties(messageMetadata)
					.build();
			}
			// 中文说明：回调通知调用方——把聚合后的完整消息包装成单元素的 ChatResponse 交出去
			onAggregationComplete.accept(new ChatResponse(List.of(new Generation(finalAssistantMessage,

					generationMetadataRef.get())), chatResponseMetadata));

			// 中文说明：聚合完成后再次重置所有累加器，释放引用并为可能的重复订阅做准备
			messageTextContentRef.set(new StringBuilder());
			thoughtsRef.set(new StringBuilder());
			outputWithoutThoughtsRef.set(new StringBuilder());
			messageMetadataMapRef.set(new HashMap<>());
			toolCallsRef.set(new ArrayList<>());
			metadataIdRef.set("");
			metadataModelRef.set("");
			metadataUsagePromptTokensRef.set(0);
			metadataUsageGenerationTokensRef.set(0);
			metadataUsageTotalTokensRef.set(0);
			metadataPromptMetadataRef.set(PromptMetadata.empty());
			metadataRateLimitRef.set(new EmptyRateLimit());

		// 中文说明：【钩子四】流异常终止时只记录错误日志，不吞掉异常——异常仍会正常传递给下游订阅者。
		// 注意：出错时不会触发 onAggregationComplete 回调。
		}).doOnError(e -> logger.error("Aggregation Error", e));
	}

	/**
	 * 【中文说明】DefaultUsage 是 {@link Usage} 接口的默认 record 实现，用于承载 token 用量统计。
	 *
	 * <p>
	 * 三个分量：promptTokens（输入消耗）、completionTokens（输出消耗）、totalTokens（总计）。
	 *
	 * <p>
	 * 由于 record 自动生成的访问器是 {@code promptTokens()} 这种无 get 前缀的形式，
	 * 而 Usage 接口要求 {@code getPromptTokens()} 风格，因此下面三个 getter 只是简单转发，
	 * 属于适配样板代码。
	 */
	public record DefaultUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) implements Usage {

		// 中文说明：转发到 record 自动生成的访问器，适配 Usage 接口的 getXxx 命名规范
		@Override
		public Integer getPromptTokens() {
			return promptTokens();
		}

		// 中文说明：同上，返回输出（补全）消耗的 token 数
		@Override
		public Integer getCompletionTokens() {
			return completionTokens();
		}

		// 中文说明：同上，返回本次调用消耗的总 token 数
		@Override
		public Integer getTotalTokens() {
			return totalTokens();
		}

		// 中文说明：返回"原生用量"的 Map 形式。这里没有厂商私有字段，直接把三个标准字段放进 Map 返回。
		@Override
		public Map<String, Integer> getNativeUsage() {
			Map<String, Integer> usage = new HashMap<>();
			usage.put("promptTokens", promptTokens());
			usage.put("completionTokens", completionTokens());
			usage.put("totalTokens", totalTokens());
			return usage;
		}
	}

}
