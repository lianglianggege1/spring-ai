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

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 采用流式编程接口（Fluent API）向 AI 模型发起无状态请求的客户端。
 * <p>
 * 核心设计理念：以 Builder 模式构建客户端实例，以链式调用配置每一次请求，
 * 支持同步（call）与流式（stream）两种调用方式，内置工具调用（Tool Calling）、
 * Advisor 拦截增强、结构化输出（Entity）等高级能力。
 * <p>
 * 典型使用流程：
 * <ol>
 *   <li>通过 {@link ChatClient#builder(ChatModel)} 创建构建器，配置默认行为</li>
 *   <li>调用 {@link #prompt()} 开启一次请求，设置 system/user 消息、工具、选项等</li>
 *   <li>调用 {@link ChatClientRequestSpec#call()} 同步获取结果，或 {@link ChatClientRequestSpec#stream()} 流式获取</li>
 * </ol>
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Arjen Poutsma
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ChatClient {

	/**
	 * 创建 ChatClient 实例，使用默认的空观测注册表（不启用可观测性）。
	 * @param chatModel 聊天模型实例，不能为空
	 * @return ChatClient 实例
	 */
	static ChatClient create(ChatModel chatModel) {
		return create(chatModel, ObservationRegistry.NOOP);
	}

	/**
	 * 创建 ChatClient 实例，指定观测注册表。
	 * @param chatModel 聊天模型实例，不能为空
	 * @param observationRegistry 观测注册表，用于集成 Micrometer 可观测性
	 * @return ChatClient 实例
	 */
	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry) {
		return create(chatModel, observationRegistry, null, null);
	}

	/**
	 * 创建 ChatClient 实例，完整指定观测注册表与自定义观测约定。
	 * @param chatModel 聊天模型实例，不能为空
	 * @param observationRegistry 观测注册表
	 * @param chatClientObservationConvention 可选的自定义客户端观测约定
	 * @param advisorObservationConvention 可选的自定义 Advisor 观测约定
	 * @return ChatClient 实例
	 */
	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention chatClientObservationConvention,
			@Nullable AdvisorObservationConvention advisorObservationConvention) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention)
			.build();
	}

	/**
	 * 创建 Builder 构建器，使用默认空观测注册表。
	 * @param chatModel 聊天模型实例
	 * @return Builder 构建器
	 */
	static Builder builder(ChatModel chatModel) {
		return builder(chatModel, ObservationRegistry.NOOP, null, null);
	}

	/**
	 * 创建 Builder 构建器，指定观测注册表与自定义观测约定。
	 * @param chatModel 聊天模型实例
	 * @param observationRegistry 观测注册表
	 * @param chatClientObservationConvention 可选的自定义客户端观测约定
	 * @param advisorObservationConvention 可选的自定义 Advisor 观测约定
	 * @return Builder 构建器
	 */
	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention chatClientObservationConvention,
			@Nullable AdvisorObservationConvention advisorObservationConvention) {
		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention,
				null);
	}

	/**
	 * 创建用于构建 {@link ChatClient} 的 {@link Builder} 构建器（最完整参数版本）。
	 * <p>
	 * 当 {@code toolCallingAdvisorBuilder} 为 {@code null} 时，会自动创建默认的
	 * {@link ToolCallingAdvisor}，其内部 {@code ToolCallingManager} 由传入的
	 * {@code observationRegistry} 驱动。
	 * <p>
	 * 当 {@code toolCallingAdvisorBuilder} 非空时，直接使用该构建器，调用方需自行配置
	 * 其内部的 {@code ToolCallingManager}（包括观测注册表），因为传入的
	 * {@code observationRegistry} 不会自动应用到自定义构建器上。
	 * @param chatModel 聊天模型，不能为空
	 * @param observationRegistry 客户端级别的观测注册表；当 toolCallingAdvisorBuilder 为 null 时，
	 *        也用于配置默认的 ToolCallingManager
	 * @param chatClientObservationConvention 可选的自定义客户端观测约定
	 * @param advisorObservationConvention 可选的自定义 Advisor 观测约定
	 * @param toolCallingAdvisorBuilder 可选的 ToolCallingAdvisor 构建器；为 null 时使用默认实现
	 * @return 新的 Builder 实例
	 */
	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention chatClientObservationConvention,
			@Nullable AdvisorObservationConvention advisorObservationConvention,
			ToolCallingAdvisor.@Nullable Builder<?> toolCallingAdvisorBuilder) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");

		return new DefaultChatClientBuilder(chatModel, observationRegistry, chatClientObservationConvention,
				advisorObservationConvention, toolCallingAdvisorBuilder);
	}

	/**
	 * 开启一个新的聊天请求，返回请求规范对象用于链式配置 prompt 内容。
	 * @return 请求规范对象
	 */
	ChatClientRequestSpec prompt();

	/**
	 * 开启一个新的聊天请求，并直接设置用户消息文本内容。
	 * @param content 用户消息文本
	 * @return 请求规范对象
	 */
	ChatClientRequestSpec prompt(String content);

	/**
	 * 开启一个新的聊天请求，使用已构建好的 {@link Prompt} 对象。
	 * @param prompt 提示词对象
	 * @return 请求规范对象
	 */
	ChatClientRequestSpec prompt(Prompt prompt);

	/**
	 * 返回一个 {@link Builder}，基于当前客户端的默认配置克隆出新的构建器，
	 * 便于在已有配置基础上微调后创建新的 ChatClient 实例。
	 * @return 克隆后的构建器
	 */
	Builder mutate();

	/**
	 * 用户消息配置规范，用于设置用户侧提示词的文本、模板参数、多媒体附件及元数据。
	 * <p>
	 * 支持从字符串、Resource 资源加载文本，支持模板变量替换，支持图片/音频等多媒体输入。
	 */
	interface PromptUserSpec {

		/**
		 * 设置用户消息文本内容。
		 * @param text 用户消息文本
		 * @return 当前规范对象，用于链式调用
		 */
		PromptUserSpec text(String text);

		/**
		 * 从 Resource 资源加载用户消息文本，指定字符集。
		 * @param text 文本资源
		 * @param charset 字符集
		 * @return 当前规范对象
		 */
		PromptUserSpec text(Resource text, Charset charset);

		/**
		 * 从 Resource 资源加载用户消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前规范对象
		 */
		PromptUserSpec text(Resource text);

		/**
		 * 批量设置模板变量参数，用于替换提示词模板中的占位符。
		 * @param p 参数键值对
		 * @return 当前规范对象
		 */
		PromptUserSpec params(Map<String, Object> p);

		/**
		 * 设置单个模板变量参数。
		 * @param k 参数名
		 * @param v 参数值
		 * @return 当前规范对象
		 */
		PromptUserSpec param(String k, Object v);

		/**
		 * 添加多媒体附件（图片、音频等）。
		 * @param media 多媒体对象数组
		 * @return 当前规范对象
		 */
		PromptUserSpec media(Media... media);

		/**
		 * 从 URL 添加多媒体附件，指定 MIME 类型。
		 * @param mimeType MIME 类型
		 * @param url 资源 URL
		 * @return 当前规范对象
		 */
		PromptUserSpec media(MimeType mimeType, URL url);

		/**
		 * 从 Resource 添加多媒体附件，指定 MIME 类型。
		 * @param mimeType MIME 类型
		 * @param resource 资源
		 * @return 当前规范对象
		 */
		PromptUserSpec media(MimeType mimeType, Resource resource);

		/**
		 * 批量设置用户消息元数据。
		 * @param metadata 元数据键值对
		 * @return 当前规范对象
		 */
		PromptUserSpec metadata(Map<String, Object> metadata);

		/**
		 * 设置单个用户消息元数据项。
		 * @param k 元数据键
		 * @param v 元数据值
		 * @return 当前规范对象
		 */
		PromptUserSpec metadata(String k, Object v);

	}

	/**
	 * 系统消息配置规范，用于设置系统侧提示词的文本、模板参数及元数据。
	 * <p>
	 * 系统消息用于定义 AI 的角色、行为规范、输出格式等全局约束，在每次请求中都会携带。
	 */
	interface PromptSystemSpec {

		/**
		 * 设置系统消息文本内容。
		 * @param text 系统消息文本
		 * @return 当前规范对象
		 */
		PromptSystemSpec text(String text);

		/**
		 * 从 Resource 资源加载系统消息文本，指定字符集。
		 * @param text 文本资源
		 * @param charset 字符集
		 * @return 当前规范对象
		 */
		PromptSystemSpec text(Resource text, Charset charset);

		/**
		 * 从 Resource 资源加载系统消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前规范对象
		 */
		PromptSystemSpec text(Resource text);

		/**
		 * 批量设置模板变量参数。
		 * @param p 参数键值对
		 * @return 当前规范对象
		 */
		PromptSystemSpec params(Map<String, Object> p);

		/**
		 * 设置单个模板变量参数。
		 * @param k 参数名
		 * @param v 参数值
		 * @return 当前规范对象
		 */
		PromptSystemSpec param(String k, Object v);

		/**
		 * 批量设置系统消息元数据。
		 * @param metadata 元数据键值对
		 * @return 当前规范对象
		 */
		PromptSystemSpec metadata(Map<String, Object> metadata);

		/**
		 * 设置单个系统消息元数据项。
		 * @param k 元数据键
		 * @param v 元数据值
		 * @return 当前规范对象
		 */
		PromptSystemSpec metadata(String k, Object v);

	}

	/**
	 * Advisor（增强器/拦截器）配置规范，用于注册 Advisor 并传递上下文参数。
	 * <p>
	 * Advisor 是 ChatClient 的扩展机制，可在请求前后插入自定义逻辑（如日志、重试、
	 * 安全过滤、记忆注入等），类似于 Spring MVC 的 Interceptor。
	 */
	interface AdvisorSpec {

		/**
		 * 设置单个 Advisor 上下文参数。
		 * @param k 参数名
		 * @param v 参数值
		 * @return 当前规范对象
		 */
		AdvisorSpec param(String k, Object v);

		/**
		 * 批量设置 Advisor 上下文参数。
		 * @param p 参数键值对
		 * @return 当前规范对象
		 */
		AdvisorSpec params(Map<String, Object> p);

		/**
		 * 注册多个 Advisor 实例。
		 * @param advisors Advisor 实例数组
		 * @return 当前规范对象
		 */
		AdvisorSpec advisors(Advisor... advisors);

		/**
		 * 注册 Advisor 列表。
		 * @param advisors Advisor 实例列表
		 * @return 当前规范对象
		 */
		AdvisorSpec advisors(List<Advisor> advisors);

	}

	/**
	 * 结构化输出（Entity）调用的可选行为配置，多个选项可组合使用。
	 * <p>
	 * 用于控制将模型响应反序列化为 Java 对象时的行为，包括是否使用模型原生结构化输出、
	 * 是否进行 Schema 校验与自动重试等。
	 */
	interface EntityParamSpec {

		/**
		 * 启用模型提供商原生结构化输出能力：将 JSON Schema 作为 API 级约束传递给模型，
		 * 而非拼接在 prompt 文本中。若底层 {@link ChatModel} 不支持结构化输出选项则无效。
		 *
		 * <p>
		 * <b>默认不启用</b>，因为不同模型/提供商对原生结构化输出的支持程度差异较大。
		 * 已知限制：
		 * <ul>
		 * <li><b>Ollama</b>：内置推理/思考模式的模型（如 qwen3:8b、qwen3.5:9b）
		 * 可能返回纯文本而非 JSON，导致反序列化失败。建议配合 {@link #validateSchema()}
		 * 使用实现自动重试，或切换为非推理模型（如 llama3.1:latest）。</li>
		 * <li><b>OpenAI</b>：Structured Outputs API 不支持顶层 JSON 数组 Schema，
		 * 启用此选项后请求 List&lt;T&gt; 会失败。请将列表包装为容器 record，
		 * 或使用默认的基于 prompt 的方式。</li>
		 * </ul>
		 * @return 当前规范对象
		 */
		EntityParamSpec useProviderStructuredOutput();

		/**
		 * 启用 Schema 校验：将模型返回的 JSON 与目标实体 Schema 进行校验，
		 * 校验失败时携带错误信息自动重试，最多重试 maxRepeatAttempts 次（默认 3 次）。
		 * <p>
		 * 注意：流式调用（stream）不支持此选项。
		 * @return 当前规范对象
		 */
		EntityParamSpec validateSchema();

	}

	/**
	 * 同步调用响应规范，提供将模型响应反序列化为实体对象、获取原始响应对象、
	 * 获取纯文本内容等多种响应消费方式。
	 */
	interface CallResponseSpec {

		/**
		 * 将响应反序列化为指定泛型类型的实体，可通过 consumer 配置结构化输出选项。
		 * @param type 目标参数化类型
		 * @param entityParamSpecConsumer 配置结构化输出选项（如原生输出、Schema 校验）
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(ParameterizedTypeReference<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 将响应反序列化为指定泛型类型的实体，使用默认配置。
		 * @param type 目标参数化类型
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(ParameterizedTypeReference<T> type);

		/**
		 * 使用自定义转换器将响应反序列化为实体，可通过 consumer 配置结构化输出选项。
		 * @param structuredOutputConverter 结构化输出转换器，负责解析与 Schema 解析
		 * @param entityParamSpecConsumer 配置结构化输出选项
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 使用自定义转换器将响应反序列化为实体，使用默认配置。
		 * @param structuredOutputConverter 结构化输出转换器
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter);

		/**
		 * 将响应反序列化为指定 Class 类型的实体，可通过 consumer 配置结构化输出选项。
		 * @param type 目标类
		 * @param entityParamSpecConsumer 配置结构化输出选项
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(Class<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 将响应反序列化为指定 Class 类型的实体，使用默认配置。
		 * @param type 目标类
		 * @param <T> 实体类型
		 * @return 反序列化后的实体，响应为空时返回 null
		 */
		<T> @Nullable T entity(Class<T> type);

		/**
		 * 返回完整的 {@link ChatClientResponse} 响应对象，包含模型输出、使用量统计、
		 * 工具调用信息等全部元数据。
		 * @return ChatClientResponse 响应对象
		 */
		ChatClientResponse chatClientResponse();

		/**
		 * 返回底层 {@link ChatResponse} 原始响应对象。
		 * @return ChatResponse 原始响应，响应为空时返回 null
		 */
		@Nullable ChatResponse chatResponse();

		/**
		 * 直接返回模型输出的纯文本内容。
		 * @return 纯文本内容，响应为空时返回 null
		 */
		@Nullable String content();

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和反序列化后的实体对象，可通过 consumer 配置结构化输出选项。
		 * @param type 目标类
		 * @param entityParamSpecConsumer 配置结构化输出选项
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和反序列化后的实体对象，使用默认配置。
		 * @param type 目标类
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type);

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和反序列化后的泛型实体，可通过 consumer 配置结构化输出选项。
		 * @param type 目标参数化类型
		 * @param entityParamSpecConsumer 配置结构化输出选项
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和反序列化后的泛型实体集合，使用默认配置。
		 * @param type 目标参数化类型
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type);

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和使用自定义转换器反序列化的实体，可通过 consumer 配置结构化输出选项。
		 * @param structuredOutputConverter 结构化输出转换器
		 * @param entityParamSpecConsumer 配置结构化输出选项
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * 返回 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 原始响应
		 * 和使用自定义转换器反序列化的实体，使用默认配置。
		 * @param structuredOutputConverter 结构化输出转换器
		 * @param <T> 实体类型
		 * @return 包含原始响应和实体的 ResponseEntity
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter);

	}

	/**
	 * 流式调用响应规范，以响应式 {@link Flux} 形式返回流式输出，支持逐 token 接收模型响应。
	 */
	interface StreamResponseSpec {

		/**
		 * 以 Flux 形式返回流式 {@link ChatClientResponse}，每个元素代表一次流式输出片段。
		 * @return 流式响应 Flux
		 */
		Flux<ChatClientResponse> chatClientResponse();

		/**
		 * 以 Flux 形式返回流式 {@link ChatResponse} 原始响应对象。
		 * @return 流式原始响应 Flux
		 */
		Flux<ChatResponse> chatResponse();

		/**
		 * 以 Flux 形式返回流式纯文本内容，每个元素代表一段文本片段。
		 * @return 流式文本 Flux
		 */
		Flux<String> content();

	}

	/**
	 * 聊天客户端请求规范，是每次请求的核心配置入口。
	 * <p>
	 * 提供完整的请求配置能力：消息列表、模型选项、工具注册、Advisor 增强、
	 * 系统/用户提示词、模板渲染器等。配置完成后通过 {@link #call()} 同步调用
	 * 或 {@link #stream()} 流式调用触发请求。
	 */
	interface ChatClientRequestSpec {

		/**
		 * 返回一个 {@link Builder}，基于当前请求的配置克隆出新的构建器，
		 * 便于在已有请求配置基础上微调后创建新的 ChatClient 实例。
		 * @return 克隆后的构建器
		 */
		Builder mutate();

		/**
		 * 通过 Consumer 回调配置 Advisor（增强器）。
		 * @param consumer Advisor 配置回调
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);

		/**
		 * 注册多个 Advisor 实例。
		 * @param advisors Advisor 实例数组
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec advisors(Advisor... advisors);

		/**
		 * 注册 Advisor 列表。
		 * @param advisors Advisor 实例列表
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec advisors(List<Advisor> advisors);

		/**
		 * 设置请求的消息列表（可包含 system、user、assistant、tool 等多种角色消息）。
		 * @param messages 消息数组
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec messages(Message... messages);

		/**
		 * 设置请求的消息列表。
		 * @param messages 消息列表
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec messages(List<Message> messages);

		/**
		 * 通过 Builder 自定义配置模型选项（如温度、最大 token 数、top_p 等）。
		 * @param customizer ChatOptions.Builder 自定义器
		 * @param <B> Builder 类型
		 * @return 当前请求规范对象
		 */
		<B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B customizer);

		/**
		 * 为本次请求注册一个或多个工具。方法接受异构的工具表示形式，
		 * 自动将每个元素路由到对应的内部列表：
		 *
		 * <ul>
		 * <li>{@link ToolCallback} — 直接注册为回调。</li>
		 * <li>{@link ToolCallbackProvider} — 直接注册为提供者；其回调在请求时惰性解析。</li>
		 * <li>{@code ToolCallback[]} 或 {@code ToolCallbackProvider[]} — 数组中每个元素按上述规则注册。</li>
		 * <li>{@link Collection} — 遍历集合，每个元素按相同规则分发。</li>
		 * <li>其他任意对象 — 视为 {@code @Tool} 注解的 POJO；为其中每个
		 * {@link org.springframework.ai.tool.annotation.Tool} 注解的方法生成一个 {@link ToolCallback}。</li>
		 * </ul>
		 *
		 * <p>
		 * 支持混合调用：
		 *
		 * <pre>{@code
		 * chatClient.prompt()
		 *     .tools(new DateTimeTools(), existingCallback, myProvider)
		 *     .toolContext(Map.of("tenantId", "acme"))
		 *     .call().content();
		 * }</pre>
		 *
		 * <p>
		 * 此处注册的工具仅对本次请求有效。若需注册对所有请求都生效的工具，
		 * 请使用 {@link Builder#defaultTools(Object...)}。
		 * @param tools 要注册的工具对象；不能为 null，且不能包含 null 元素
		 * @return 当前规范对象，用于链式调用
		 * @throws IllegalArgumentException 若 tools 为 null、包含 null 元素，
		 *         或 POJO 参数没有 {@code @Tool} 注解的方法
		 */
		ChatClientRequestSpec tools(Object... tools);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #tools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #tools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #tools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		/**
		 * 设置工具调用上下文，传递给工具回调的运行时参数（如租户 ID、用户信息等）。
		 * @param toolContext 工具上下文键值对
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec toolContext(Map<String, Object> toolContext);

		/**
		 * 设置系统消息文本内容。
		 * @param text 系统消息文本
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec system(String text);

		/**
		 * 从 Resource 资源加载系统消息文本，指定字符集。
		 * @param textResource 文本资源
		 * @param charset 字符集
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec system(Resource textResource, Charset charset);

		/**
		 * 从 Resource 资源加载系统消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec system(Resource text);

		/**
		 * 通过 Consumer 回调配置系统消息。
		 * @param consumer 系统消息配置回调
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);

		/**
		 * 设置用户消息文本内容。
		 * @param text 用户消息文本
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec user(String text);

		/**
		 * 从 Resource 资源加载用户消息文本，指定字符集。
		 * @param text 文本资源
		 * @param charset 字符集
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec user(Resource text, Charset charset);

		/**
		 * 从 Resource 资源加载用户消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec user(Resource text);

		/**
		 * 通过 Consumer 回调配置用户消息。
		 * @param consumer 用户消息配置回调
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);

		/**
		 * 设置自定义模板渲染器，用于渲染提示词模板中的占位符。
		 * @param templateRenderer 模板渲染器
		 * @return 当前请求规范对象
		 */
		ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer);

		/**
		 * 执行同步调用，返回响应规范对象用于消费结果。
		 * @return 同步响应规范
		 */
		CallResponseSpec call();

		/**
		 * 执行流式调用，返回响应式 Flux 响应规范对象。
		 * @return 流式响应规范
		 */
		StreamResponseSpec stream();

	}

	/**
	 * ChatClient 的可变构建器，用于创建 {@link ChatClient} 实例并配置全局默认行为。
	 * <p>
	 * 可配置的默认项包括：默认 Advisor、默认模型选项、默认用户/系统提示词、
	 * 默认工具、默认工具上下文、默认模板渲染器等。
	 * <p>
	 * 每次通过 {@link #prompt()} 创建的请求都会继承这些默认配置，
	 * 请求级别的配置会覆盖对应的默认值。
	 */
	interface Builder {

		/**
		 * 设置默认 Advisor 数组。
		 * @param advisors Advisor 实例数组
		 * @return 当前构建器
		 */
		Builder defaultAdvisors(Advisor... advisors);

		/**
		 * 通过 Consumer 回调配置默认 Advisor。
		 * @param advisorSpecConsumer Advisor 配置回调
		 * @return 当前构建器
		 */
		Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);

		/**
		 * 设置默认 Advisor 列表。
		 * @param advisors Advisor 实例列表
		 * @return 当前构建器
		 */
		Builder defaultAdvisors(List<Advisor> advisors);

		/**
		 * 设置默认模型选项。
		 * @param chatOptions 模型选项构建器
		 * @return 当前构建器
		 */
		Builder defaultOptions(ChatOptions.Builder chatOptions);

		/**
		 * 设置默认用户消息文本。
		 * @param text 用户消息文本
		 * @return 当前构建器
		 */
		Builder defaultUser(String text);

		/**
		 * 从 Resource 设置默认用户消息文本，指定字符集。
		 * @param text 文本资源
		 * @param charset 字符集
		 * @return 当前构建器
		 */
		Builder defaultUser(Resource text, Charset charset);

		/**
		 * 从 Resource 设置默认用户消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前构建器
		 */
		Builder defaultUser(Resource text);

		/**
		 * 通过 Consumer 回调配置默认用户消息。
		 * @param userSpecConsumer 用户消息配置回调
		 * @return 当前构建器
		 */
		Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);

		/**
		 * 设置默认系统消息文本。
		 * @param text 系统消息文本
		 * @return 当前构建器
		 */
		Builder defaultSystem(String text);

		/**
		 * 从 Resource 设置默认系统消息文本，指定字符集。
		 * @param text 文本资源
		 * @param charset 字符集
		 * @return 当前构建器
		 */
		Builder defaultSystem(Resource text, Charset charset);

		/**
		 * 从 Resource 设置默认系统消息文本，使用默认字符集。
		 * @param text 文本资源
		 * @return 当前构建器
		 */
		Builder defaultSystem(Resource text);

		/**
		 * 通过 Consumer 回调配置默认系统消息。
		 * @param systemSpecConsumer 系统消息配置回调
		 * @return 当前构建器
		 */
		Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);

		/**
		 * 设置默认模板渲染器。
		 * @param templateRenderer 模板渲染器
		 * @return 当前构建器
		 */
		Builder defaultTemplateRenderer(TemplateRenderer templateRenderer);

		/**
		 * 注册一个或多个默认工具，对由此 Builder 构建的所有请求都生效。
		 * 接受与 {@link ChatClientRequestSpec#tools(Object...)} 相同的异构工具表示形式，
		 * 应用相同的自动分发规则：
		 *
		 * <ul>
		 * <li>{@link ToolCallback} — 直接注册为回调。</li>
		 * <li>{@link ToolCallbackProvider} — 直接注册为提供者；其回调在请求时惰性解析。</li>
		 * <li>{@code ToolCallback[]} 或 {@code ToolCallbackProvider[]} — 数组中每个元素按上述规则注册。</li>
		 * <li>{@link Collection} — 遍历集合，每个元素按相同规则分发。</li>
		 * <li>其他任意对象 — 视为 {@code @Tool} 注解的 POJO；为其中每个
		 * {@link org.springframework.ai.tool.annotation.Tool} 注解的方法生成一个 {@link ToolCallback}。</li>
		 * </ul>
		 *
		 * <p>
		 * 默认工具在由此 Builder 构建的所有 ChatClient 实例的所有请求中共享。
		 * 如果某次请求通过 {@link ChatClientRequestSpec#tools(Object...)} 也提供了工具，
		 * 则该次请求的运行时工具会完全覆盖默认工具。
		 *
		 * <p>
		 * 注意：由于默认工具是共享的，请不要注册仅应在特定上下文中可用的工具，
		 * 避免工具泄露风险。
		 * @param tools 要注册的工具对象；不能为 null，且不能包含 null 元素
		 * @return 当前构建器，用于链式调用
		 * @throws IllegalArgumentException 若 tools 为 null、包含 null 元素，
		 *         或 POJO 参数没有 {@code @Tool} 注解的方法
		 */
		Builder defaultTools(Object... tools);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #defaultTools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(ToolCallback... toolCallbacks);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #defaultTools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks);

		/**
		 * @deprecated 自 2.0.0 起废弃，请使用 {@link #defaultTools(Object...)} 替代。将在 3.0.0 移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		/**
		 * 设置默认工具调用上下文。
		 * @param toolContext 工具上下文键值对
		 * @return 当前构建器
		 */
		Builder defaultToolContext(Map<String, Object> toolContext);

		/**
		 * 克隆当前构建器，生成一个独立的新构建器实例，两者互不影响。
		 * @return 克隆后的构建器
		 */
		Builder clone();

		/**
		 * 构建并返回 ChatClient 实例。
		 * @return ChatClient 实例
		 */
		ChatClient build();

	}

}
