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

package org.springframework.ai.chat.client;

import java.util.function.Consumer;

/**
 * Configuration options for the ChatClient request.
 *
 * Preset advisors parameters that can be passed as configuration options to the Advisor
 * context.
 *
 * @author Christian Tzolov
 */
/**
 * 聊天客户端请求的配置选项。
 *
 * 预置顾问参数，可作为配置选项传递给顾问上下文。
 *
 * @author Christian Tzolov
 */
public final class AdvisorParams {

	private AdvisorParams() {
	}

	/**
	 * Advisor parameter that enables provider-native structured output for all
	 * {@code entity()} calls on a prompt. When set, the JSON schema is delivered to the
	 * AI provider as an API-level constraint rather than appended as prompt text.
	 *
	 * <p>
	 * <b>Not enabled by default</b> because native structured output support varies
	 * across models and providers. Known limitations:
	 * <ul>
	 * <li><b>Ollama</b>: models with a built-in reasoning/thinking mode (e.g.
	 * {@code qwen3:8b}, {@code qwen3.5:9b}) may return plain text instead of JSON,
	 * causing deserialization failures.</li>
	 * <li><b>OpenAI</b>: the Structured Outputs API does not accept a top-level JSON
	 * array schema. Requesting a {@code List<T>} will fail; wrap the list in a container
	 * record or use the default prompt-based approach instead.</li>
	 * </ul>
	 *
	 * <p>
	 * For per-call control prefer
	 * {@link ChatClient.EntityParamSpec#useProviderStructuredOutput()}.
	 */
	/**
	 * 顾问参数，用于为提示词上所有 {@code entity()} 调用开启厂商原生结构化输出能力。
	 * 开启后，JSON Schema 将作为 API 层面约束下发给 AI 服务端，而非追加到提示词文本中。
	 *
	 * <p>
	 * <b>默认不开启</b>，因为不同模型与服务商对原生结构化输出的支持存在差异。已知限制：
	 * <ul>
	 * <li><b>Ollama</b>: 内置推理/思考模式的模型（例如 {@code qwen3:8b}、{@code qwen3.5:9b}）可能返回普通文本而非 JSON，进而导致反序列化失败。</li>
	 * <li><b>OpenAI</b>: 结构化输出 API 不支持顶层为 JSON 数组的 Schema。请求 {@code List<T>} 会报错；需要将数组包装到容器记录类中，或使用默认基于提示词的实现。</li>
	 * </ul>
	 *
	 * <p>
	 * 如果需要单调用粒度控制，请优先使用
	 * {@link ChatClient.EntityParamSpec#useProviderStructuredOutput()}。
	 */
	public static final Consumer<ChatClient.AdvisorSpec> ENABLE_NATIVE_STRUCTURED_OUTPUT = a -> a
		.param(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey(), true);

	/**
	 * Controls whether a
	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor} is
	 * automatically added to the chain. Auto-registration is enabled by default so that
	 * tools injected at runtime by another advisor are handled correctly even when no
	 * static tools are configured. No explicit
	 * {@link org.springframework.ai.chat.client.advisor.api.ToolAdvisor} must be present.
	 * Pass {@code false} to opt out:
	 *
	 * <pre>{@code
	 * client.prompt()
	 *     .tools(myTool)
	 *     .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
	 *     .call();
	 * }</pre>
	 */
	/**
	 * 控制是否自动向调用链中添加
	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}。
	 * 默认开启自动注册，即便没有配置静态工具，也可以正确处理由其他顾问在运行时注入的工具。
	 * 不能再显式配置 {@link org.springframework.ai.chat.client.advisor.api.ToolAdvisor}。
	 * 传入 {@code false} 可以关闭该自动注册：
	 *
	 * <pre>{@code
	 * client.prompt()
	 *     .tools(myTool)
	 *     .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
	 *     .call();
	 * }</pre>
	 */
	public static Consumer<ChatClient.AdvisorSpec> toolCallingAdvisorAutoRegister(boolean enabled) {
		return a -> a.param(ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER.getKey(), enabled);
	}

	/**
	 * @deprecated since 2.0.0 in favor of
	 * {@link #toolCallingAdvisorAutoRegister(boolean)}.
	 */
	@Deprecated(since = "2.0.0", forRemoval = true)
	public static Consumer<ChatClient.AdvisorSpec> toolCallAdvisorAutoRegister(boolean enabled) {
		return toolCallingAdvisorAutoRegister(enabled);
	}

}
