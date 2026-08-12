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
import java.util.Optional;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationDocumentation;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ToolCallingManager}.
 *
 * @author Thomas Vitale
 * @author chabinhwang
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolCallingManager} 的默认实现，是工具调用真正落地执行的地方。
 *
 * <p>
 * 三个关键协作组件（均可通过 Builder 定制）：
 * <ul>
 * <li>{@code toolCallbackResolver}：当 Prompt 选项里找不到对应工具时，按<b>工具名</b>兜底解析
 * （例如从 Spring 容器、MCP Server 中查找）。</li>
 * <li>{@code toolExecutionExceptionProcessor}：工具执行抛出 {@code ToolExecutionException} 时的处理策略，
 * 决定是把错误信息作为「工具结果」返回给模型自我修正，还是直接向上抛出。</li>
 * <li>{@code observationRegistry} + {@code observationConvention}：Micrometer 可观测性，
 * 为每次工具调用生成一条 observation（含工具名、入参、结果）。</li>
 * </ul>
 *
 * <p>
 * 执行主流程见 {@link #executeToolCalls(Prompt, ChatResponse)}：
 * 找到含 tool_calls 的 Generation → 构建 ToolContext → 逐个执行工具 → 拼装新的会话历史。
 *
 * <p>
 * 类被声明为 {@code final}，不支持继承扩展，定制请通过替换上述协作组件实现。
 *
 * <p>
 * 对应英文 javadoc 标签：{@code @author} Thomas Vitale、chabinhwang、Christian Tzolov；
 * {@code @since} 1.0.0。
 */
public final class DefaultToolCallingManager implements ToolCallingManager {

	private static final Log logger = LogFactory.getLog(DefaultToolCallingManager.class);

	// @formatter:off

	// 【中文说明】默认可观测性注册表：NOOP 表示不做任何埋点，零开销
	private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY
			= ObservationRegistry.NOOP;

	// 【中文说明】默认的观测约定，决定 observation 的名称与标签（低基数/高基数 KeyValues）
	private static final ToolCallingObservationConvention DEFAULT_OBSERVATION_CONVENTION
			= new DefaultToolCallingObservationConvention();

	// 【中文说明】默认工具解析器：委托列表为空，即默认不做任何兜底解析
	private static final ToolCallbackResolver DEFAULT_TOOL_CALLBACK_RESOLVER
			= new DelegatingToolCallbackResolver(List.of());

	// 【中文说明】默认异常处理器：通常把工具异常信息转成字符串返回给模型，让模型有机会自行纠正
	private static final ToolExecutionExceptionProcessor DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR
			= DefaultToolExecutionExceptionProcessor.builder().build();

	// 【中文说明】以下两个常量拼成一条告警文案：提示「找不到工具」可能是大模型改写/截断了工具名
	private static final String POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_START = "LLM may have adapted the tool name '";
	private static final String POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_END
			= "', especially if the name was truncated due to length limits. If this is the case, you can customize the prefixing and processing logic using McpToolNamePrefixGenerator";


	// @formatter:on

	// 【中文说明】Micrometer 可观测性注册表，用于记录每次工具调用
	private final ObservationRegistry observationRegistry;

	// 【中文说明】工具解析器：按工具名兜底查找 ToolCallback（选项中未直接携带时使用）
	private final ToolCallbackResolver toolCallbackResolver;

	// 【中文说明】工具执行异常的处理策略
	private final ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	// 【中文说明】观测约定，非 final，可通过 setObservationConvention 在运行期替换
	private ToolCallingObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	/**
	 * 【中文说明】全参构造器，三个协作组件均不允许为 null。
	 *
	 * <p>
	 * 注意第三个校验的错误信息里写的是 {@code toolCallExceptionConverter}，
	 * 这是历史命名遗留，实际对应的是 {@code toolExecutionExceptionProcessor} 参数。
	 */
	public DefaultToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
		// 参数校验：三个依赖组件缺一不可
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		Assert.notNull(toolCallbackResolver, "toolCallbackResolver cannot be null");
		Assert.notNull(toolExecutionExceptionProcessor, "toolCallExceptionConverter cannot be null");

		this.observationRegistry = observationRegistry;
		this.toolCallbackResolver = toolCallbackResolver;
		this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
	}

	/**
	 * 【中文说明】从聊天选项中提取工具定义列表。
	 *
	 * <p>
	 * 实现很直接：取出 {@code toolCallbacks}，映射为各自的 {@code ToolDefinition}。
	 * 这一步<b>不会</b>触发 toolCallbackResolver 的解析，也不会执行任何工具。
	 */
	@Override
	public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
		Assert.notNull(chatOptions, "chatOptions cannot be null");

		// 空值处理：未配置工具时退化为空列表，保证后续 stream 操作安全
		List<ToolCallback> toolCallbacks = new ArrayList<>(
				!CollectionUtils.isEmpty(chatOptions.getToolCallbacks()) ? chatOptions.getToolCallbacks() : List.of());

		// 只取元信息（名称/描述/入参 Schema），供 ChatModel 发送给大模型
		return toolCallbacks.stream().map(ToolCallback::getToolDefinition).toList();
	}

	/**
	 * 【中文说明】执行模型请求的工具调用，整体分四步：
	 *
	 * <ol>
	 * <li>从 ChatResponse 的多个 Generation 中，找出<b>第一个</b>携带 tool_calls 的结果；</li>
	 * <li>构建 ToolContext（把选项中的 toolContext 透传给工具）；</li>
	 * <li>逐个执行工具调用，得到 ToolResponseMessage 与聚合后的 returnDirect 标记；</li>
	 * <li>把 AssistantMessage 与 ToolResponseMessage 追加到会话历史，封装成 ToolExecutionResult 返回。</li>
	 * </ol>
	 *
	 * <p>
	 * 若响应中根本没有工具调用请求，说明调用方使用姿势有误，直接抛出 {@link IllegalStateException}。
	 */
	@Override
	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
		Assert.notNull(prompt, "prompt cannot be null");
		Assert.notNull(chatResponse, "chatResponse cannot be null");

		// 第 1 步：定位第一个包含 tool_calls 的生成结果（多结果场景只处理第一个）
		Optional<Generation> toolCallGeneration = chatResponse.getResults()
			.stream()
			.filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
			.findFirst();

		// 防御性判断：调用本方法前应先用 ToolExecutionEligibilityChecker 确认确实需要执行工具
		if (toolCallGeneration.isEmpty()) {
			throw new IllegalStateException("No tool call requested by the chat model");
		}

		AssistantMessage assistantMessage = toolCallGeneration.get().getOutput();

		// 第 2 步：构建工具上下文（不会发送给大模型，仅在本地工具执行时可见）
		ToolContext toolContext = buildToolContext(prompt, assistantMessage);

		// 第 3 步：真正执行工具调用
		InternalToolExecutionResult internalToolExecutionResult = executeToolCall(prompt, assistantMessage,
				toolContext);

		// 第 4 步：拼装「原历史 + 助手消息 + 工具响应」，供下一轮请求使用
		List<Message> conversationHistory = buildConversationHistoryAfterToolExecution(prompt.getInstructions(),
				assistantMessage, internalToolExecutionResult.toolResponseMessage());

		return ToolExecutionResult.builder()
			.conversationHistory(conversationHistory)
			.returnDirect(internalToolExecutionResult.returnDirect())
			.build();
	}

	/**
	 * 【中文说明】从 Prompt 选项中提取工具上下文并包装为 {@link ToolContext}。
	 *
	 * <p>
	 * 只有当选项确实是 {@link ToolCallingChatOptions} 且上下文非空时才复制其内容，
	 * 否则退化为空 Map，保证工具方法拿到的 ToolContext 永远不为 null。
	 */
	private static ToolContext buildToolContext(Prompt prompt, AssistantMessage assistantMessage) {
		// 默认空上下文，避免返回 null
		Map<String, Object> toolContextMap = Map.of();

		// 类型判断 + 非空判断：两者都满足才复制一份可变副本
		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
				&& !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {
			toolContextMap = new HashMap<>(toolCallingChatOptions.getToolContext());
		}

		return new ToolContext(toolContextMap);
	}

	/**
	 * Execute the tool call and return the response message.
	 */
	/**
	 * 【中文说明】遍历并执行模型请求的每一个工具调用，返回内部聚合结果。
	 *
	 * <p>
	 * 关键处理点：
	 * <ul>
	 * <li><b>工具查找顺序</b>：先在 Prompt 选项自带的 toolCallbacks 中按名称匹配，
	 * 找不到再回退到 {@code toolCallbackResolver} 兜底解析；仍找不到则抛异常。</li>
	 * <li><b>returnDirect 聚合</b>：多个工具之间取<b>逻辑与</b>，即只有所有工具都声明 returnDirect
	 * 才会直接返回结果，任一工具需要模型总结则整体回传给模型。</li>
	 * <li><b>异常处理</b>：{@code ToolExecutionException} 会交给 processor 处理（通常转成错误文本），
	 * 而不是直接中断整条链路。</li>
	 * <li><b>可观测性</b>：每次工具执行都包在一个 observation 内，并正确挂接父 observation。</li>
	 * </ul>
	 */
	private InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
			ToolContext toolContext) {
		// 取出本次请求可用的工具回调；选项类型不符或为空时保持空列表，后续走 resolver 兜底
		List<ToolCallback> toolCallbacks = List.of();
		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
			if (!CollectionUtils.isEmpty(toolCallingChatOptions.getToolCallbacks())) {
				toolCallbacks = toolCallingChatOptions.getToolCallbacks();
			}
		}

		// 收集每个工具调用的响应
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		// 用 Boolean 包装类型而非 boolean：null 表示「尚未处理任何工具」，便于区分首次赋值与后续聚合
		Boolean returnDirect = null;

		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {

			if (logger.isDebugEnabled()) {
				logger.debug("Executing tool call: " + toolCall.name());
			}

			String toolName = toolCall.name();
			String toolInputArguments = toolCall.arguments();

			// Handle the possible null parameter situation in streaming mode.
			// 【中文说明】空值处理：流式模式下模型可能返回空的参数串，
			// 这里统一兜底为空 JSON 对象 "{}"，避免下游 JSON 反序列化失败
			final String finalToolInputArguments;
			if (!StringUtils.hasText(toolInputArguments)) {
				if (logger.isWarnEnabled()) {
					logger.warn("Tool call arguments are null or empty for tool: " + toolName
							+ ". Using empty JSON object as default.");
				}
				finalToolInputArguments = "{}";
			}
			else {
				finalToolInputArguments = toolInputArguments;
			}

			// 【中文说明】工具查找：优先用 Prompt 选项中显式注册的工具，
			// 未命中时（orElseGet 惰性求值）才调用 resolver 兜底解析
			ToolCallback toolCallback = toolCallbacks.stream()
				.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
				.findFirst()
				.orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

			// 【中文说明】仍未找到工具：先给出「大模型可能改写/截断了工具名」的排查提示，再快速失败
			if (toolCallback == null) {
				if (logger.isWarnEnabled()) {
					logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_START + toolName
							+ POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_END);
				}
				throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
			}

			// 【中文说明】聚合 returnDirect：首个工具直接取值，其后与已有值做「逻辑与」，
			// 即只要有一个工具需要模型继续处理，整体就不能直接返回
			if (returnDirect == null) {
				returnDirect = toolCallback.getToolMetadata().returnDirect();
			}
			else {
				returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
			}

			// In streaming/reactive mode the parent observation is propagated through the
			// Reactor context captured in ToolCallReactiveContextHolder. In blocking mode
			// that holder is never populated, so fall back to the observation currently
			// in scope on the calling thread to keep the observation hierarchy intact.
			// 【中文说明】获取父 observation，用于保持调用链层级完整：
			// - 流式/响应式场景：父 observation 通过 Reactor Context 传递，从
			// ToolCallReactiveContextHolder 中取；
			// - 阻塞式场景：该 Holder 不会被填充，于是回退到当前线程 ThreadLocal 中正在进行的
			// observation。
			Observation parent = ToolCallReactiveContextHolder.getContext()
				.getOrDefault(ObservationThreadLocalAccessor.KEY, this.observationRegistry.getCurrentObservation());

			// 【中文说明】组装观测上下文：记录工具定义、元数据、调用 ID、类型与入参
			ToolCallingObservationContext observationContext = ToolCallingObservationContext.builder()
				.toolDefinition(toolCallback.getToolDefinition())
				.toolMetadata(toolCallback.getToolMetadata())
				.toolCallId(toolCall.id())
				.toolType(toolCall.type())
				.toolCallArguments(finalToolInputArguments)
				.build();

			// 【中文说明】在 observation 包裹下执行工具：
			// 自定义约定优先，缺省时使用 DEFAULT_OBSERVATION_CONVENTION
			String toolCallResult = ToolCallingObservationDocumentation.TOOL_CALL
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
						this.observationRegistry)
				.parentObservation(parent)
				.observe(() -> {
					String toolResult;
					try {
						// 真正执行工具逻辑
						toolResult = toolCallback.call(finalToolInputArguments, toolContext);
					}
					catch (ToolExecutionException ex) {
						// 工具异常不直接向上抛，交由处理器决定（默认转成错误文本回传给模型）
						toolResult = this.toolExecutionExceptionProcessor.process(ex);
					}
					// 把结果写回观测上下文，供指标/追踪采集
					observationContext.setToolCallResult(toolResult);
					return toolResult;
				});

			// 空值处理：结果为 null 时用空字符串占位，保证 ToolResponse 内容非空
			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName,
					toolCallResult != null ? toolCallResult : ""));
		}

		// 若一个工具都没执行（returnDirect 仍为 null），默认按 false 处理
		return new InternalToolExecutionResult(ToolResponseMessage.builder().responses(toolResponses).build(),
				Objects.requireNonNullElse(returnDirect, false));
	}

	/**
	 * 【中文说明】构造工具执行后的会话历史：在原有消息之后依次追加
	 * AssistantMessage（含 tool_calls）与 ToolResponseMessage（工具返回值）。
	 *
	 * <p>
	 * 顺序不可颠倒——大多数厂商 API 要求 tool 角色的消息必须紧跟在发起调用的 assistant 消息之后。
	 */
	private List<Message> buildConversationHistoryAfterToolExecution(List<Message> previousMessages,
			AssistantMessage assistantMessage, ToolResponseMessage toolResponseMessage) {
		// 拷贝一份新列表，不修改传入的原始历史
		List<Message> messages = new ArrayList<>(previousMessages);
		messages.add(assistantMessage);
		messages.add(toolResponseMessage);
		return messages;
	}

	// 【中文说明】运行期替换观测约定，用于自定义 observation 的名称与标签
	public void setObservationConvention(ToolCallingObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	// 【中文说明】创建 Builder 的静态工厂方法
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】内部使用的私有 record，仅用于在方法间传递「工具响应消息 + returnDirect 标记」
	 * 这一对值，避免定义额外的公开类型。
	 */
	private record InternalToolExecutionResult(ToolResponseMessage toolResponseMessage, boolean returnDirect) {
	}

	/**
	 * 【中文说明】{@link DefaultToolCallingManager} 的 Builder。
	 *
	 * <p>
	 * 三个字段均预置了合理的默认值（无埋点、空解析器、默认异常处理器），
	 * 因此 {@code DefaultToolCallingManager.builder().build()} 即可得到一个可用实例。
	 * 私有构造器使其只能经由 {@code builder()} 创建。
	 */
	public final static class Builder {

		// 【中文说明】默认不做可观测性埋点
		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

		// 【中文说明】默认解析器不含任何委托，等于不做兜底解析
		private ToolCallbackResolver toolCallbackResolver = DEFAULT_TOOL_CALLBACK_RESOLVER;

		// 【中文说明】默认的工具异常处理策略
		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor = DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR;

		// 【中文说明】私有构造器：强制通过 builder() 创建
		private Builder() {
		}

		// 【中文说明】设置可观测性注册表
		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		// 【中文说明】设置工具兜底解析器（如按名称从 Spring 容器 / MCP 中查找）
		public Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		// 【中文说明】设置工具执行异常的处理策略
		public Builder toolExecutionExceptionProcessor(
				ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
			return this;
		}

		// 【中文说明】构建实例，非空校验在构造器中统一完成
		public DefaultToolCallingManager build() {
			return new DefaultToolCallingManager(this.observationRegistry, this.toolCallbackResolver,
					this.toolExecutionExceptionProcessor);
		}

	}

}
