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
 * Client to perform stateless requests to an AI Model, using a fluent API.
 * <p>
 * Use {@link ChatClient#builder(ChatModel)} to prepare an instance.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Arjen Poutsma
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 采用流式编程接口，向AI模型发起无状态请求的客户端。
 * <p>
 * 可通过{@link ChatClient#builder(ChatModel)}构建实例。
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Arjen Poutsma
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ChatClient {

	/** 使用指定的 {@link ChatModel} 创建 {@link ChatClient} 实例。 */
	static ChatClient create(ChatModel chatModel) {
		return create(chatModel, ObservationRegistry.NOOP);
	}

	/** 使用指定的 {@link ChatModel} 和 {@link ObservationRegistry} 创建 {@link ChatClient} 实例。 */
	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry) {
		return create(chatModel, observationRegistry, null, null);
	}

	/**
	 * 使用完整配置创建 {@link ChatClient} 实例。
	 * @param chatModel 聊天模型
	 * @param observationRegistry 观测注册表
	 * @param chatClientObservationConvention 可选的自定义聊天客户端观测约定
	 * @param advisorObservationConvention 可选的自定义 Advisor 观测约定
	 * @return 新建的 {@link ChatClient} 实例
	 */
	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention chatClientObservationConvention,
			@Nullable AdvisorObservationConvention advisorObservationConvention) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention)
			.build();
	}

	/** 使用指定的 {@link ChatModel} 创建 {@link Builder} 构建器。 */
	static Builder builder(ChatModel chatModel) {
		return builder(chatModel, ObservationRegistry.NOOP, null, null);
	}

	/**
	 * 使用指定配置创建 {@link Builder} 构建器。
	 * @param chatModel 聊天模型
	 * @param observationRegistry 观测注册表
	 * @param chatClientObservationConvention 可选的自定义聊天客户端观测约定
	 * @param advisorObservationConvention 可选的自定义 Advisor 观测约定
	 * @return 新建的 {@link Builder} 实例
	 */
	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention chatClientObservationConvention,
			@Nullable AdvisorObservationConvention advisorObservationConvention) {
		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention,
				null);
	}

	/**
	 * Creates a {@link Builder} for constructing a {@link ChatClient}.
	 * <p>
	 * When {@code toolCallingAdvisorBuilder} is {@code null}, a default
	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor} is created
	 * with a {@link org.springframework.ai.model.tool.ToolCallingManager} backed by the
	 * supplied {@code observationRegistry}.
	 * <p>
	 * When {@code toolCallingAdvisorBuilder} is non-null it is used as-is. The caller is
	 * then responsible for configuring the builder's
	 * {@link org.springframework.ai.model.tool.ToolCallingManager}, including any
	 * {@link io.micrometer.observation.ObservationRegistry}, since the supplied
	 * {@code observationRegistry} will not be automatically applied to it.
	 * @param chatModel the chat model to use
	 * @param observationRegistry the observation registry for client-level observations;
	 * also used to configure the default {@code ToolCallingManager} when
	 * {@code toolCallingAdvisorBuilder} is {@code null}
	 * @param chatClientObservationConvention optional custom observation convention for
	 * the chat client
	 * @param advisorObservationConvention optional custom observation convention for
	 * advisors
	 * @param toolCallingAdvisorBuilder optional builder for the
	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}; when
	 * {@code null} a default is created
	 * @return a new {@link Builder}
	 */
	/**
	 * 创建用于构建{@link ChatClient}的{@link Builder}构建器。
	 * <p>
	 * 当{@code toolCallingAdvisorBuilder}为{@code null}时，会创建默认的
	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}，
	 * 其内部的{@link org.springframework.ai.model.tool.ToolCallingManager}
	 * 由传入的{@code observationRegistry}驱动。
	 * <p>
	 * 当{@code toolCallingAdvisorBuilder}非空时，直接使用该构建器。调用方需自行配置
	 * 其内部的{@link org.springframework.ai.model.tool.ToolCallingManager}
	 * （包括观测注册表），因为传入的{@code observationRegistry}
	 * 不会自动应用到该自定义构建器上。
	 * @param chatModel 要使用的聊天模型
	 * @param observationRegistry 客户端级别的观测注册表；当toolCallingAdvisorBuilder为null时，
	 *        也用于配置默认的ToolCallingManager
	 * @param chatClientObservationConvention 可选的自定义聊天客户端观测约定
	 * @param advisorObservationConvention 可选的自定义Advisor观测约定
	 * @param toolCallingAdvisorBuilder 可选的ToolCallingAdvisor构建器；为null时使用默认实现
	 * @return 新的Builder实例
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

	/** 创建一个新的聊天请求规范。 */
	ChatClientRequestSpec prompt();

	/** 使用指定的文本内容创建一个新的聊天请求规范。 */
	ChatClientRequestSpec prompt(String content);

	/** 使用指定的 {@link Prompt} 对象创建一个新的聊天请求规范。 */
	ChatClientRequestSpec prompt(Prompt prompt);

	/**
	 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
	 * settings are replicated from the default {@link ChatClientRequestSpec} of this
	 * client.
	 */
	/**
	 * 返回一个{@link ChatClient.Builder}，用于新建{@link ChatClient}实例，
	 * 该实例的配置基于当前客户端默认{@link ChatClientRequestSpec}进行复制生成。
	 */
	Builder mutate();

	/**
	 * Specification for configuring the user prompt message.
	 * 用于配置用户提示消息的规范接口。
	 */
	interface PromptUserSpec {

		/** 设置用户消息的文本内容。 */
		PromptUserSpec text(String text);

		/** 从资源文件读取文本内容，并指定字符集。 */
		PromptUserSpec text(Resource text, Charset charset);

		/** 从资源文件读取文本内容，使用默认字符集。 */
		PromptUserSpec text(Resource text);

		/** 批量设置模板参数。 */
		PromptUserSpec params(Map<String, Object> p);

		/** 设置单个模板参数。 */
		PromptUserSpec param(String k, Object v);

		/** 添加一个或多个媒体内容（如图片、音频等）。 */
		PromptUserSpec media(Media... media);

		/** 通过 MIME 类型和 URL 添加媒体内容。 */
		PromptUserSpec media(MimeType mimeType, URL url);

		/** 通过 MIME 类型和资源文件添加媒体内容。 */
		PromptUserSpec media(MimeType mimeType, Resource resource);

		/** 批量设置消息的元数据。 */
		PromptUserSpec metadata(Map<String, Object> metadata);

		/** 设置单个消息元数据键值对。 */
		PromptUserSpec metadata(String k, Object v);

	}

	/**
	 * Specification for a prompt system.
	 * 用于配置系统提示消息的规范接口。
	 */
	interface PromptSystemSpec {

		/** 设置系统消息的文本内容。 */
		PromptSystemSpec text(String text);

		/** 从资源文件读取文本内容，并指定字符集。 */
		PromptSystemSpec text(Resource text, Charset charset);

		/** 从资源文件读取文本内容，使用默认字符集。 */
		PromptSystemSpec text(Resource text);

		/** 批量设置模板参数。 */
		PromptSystemSpec params(Map<String, Object> p);

		/** 设置单个模板参数。 */
		PromptSystemSpec param(String k, Object v);

		/** 批量设置消息的元数据。 */
		PromptSystemSpec metadata(Map<String, Object> metadata);

		/** 设置单个消息元数据键值对。 */
		PromptSystemSpec metadata(String k, Object v);

	}

	/**
	 * Specification for configuring advisors.
	 * 用于配置 Advisor（拦截器/增强器）的规范接口。
	 */
	interface AdvisorSpec {

		/** 设置单个 Advisor 参数。 */
		AdvisorSpec param(String k, Object v);

		/** 批量设置 Advisor 参数。 */
		AdvisorSpec params(Map<String, Object> p);

		/** 以可变参数形式添加一个或多个 Advisor。 */
		AdvisorSpec advisors(Advisor... advisors);

		/** 以列表形式添加多个 Advisor。 */
		AdvisorSpec advisors(List<Advisor> advisors);

	}

	/**
	 * Configures optional behaviour for {@code entity(...)} calls. Options may be
	 * combined.
	 * 用于配置 {@code entity(...)} 调用的可选行为。各选项可组合使用。
	 */
	interface EntityParamSpec {

		/**
		 * Delivers the JSON schema to the AI provider as an API-level constraint rather
		 * than appending it as prompt text. Has no effect if the underlying
		 * {@link org.springframework.ai.chat.model.ChatModel} does not support
		 * {@link org.springframework.ai.model.tool.StructuredOutputChatOptions}.
		 *
		 * <p>
		 * <b>Not enabled by default</b> because native structured output support varies
		 * across models and providers. Known limitations:
		 * <ul>
		 * <li><b>Ollama</b>: models with a built-in reasoning/thinking mode (e.g.
		 * {@code qwen3:8b}, {@code qwen3.5:9b}) may return plain text instead of JSON,
		 * causing deserialization failures. Use {@link #validateSchema()} alongside this
		 * option for automatic retry, or switch to a non-reasoning model such as
		 * {@code llama3.1:latest}.</li>
		 * <li><b>OpenAI</b>: the Structured Outputs API does not accept a top-level JSON
		 * array schema. Requesting a {@code List<T>} with this option enabled will fail.
		 * Wrap the list in a container record or use the default prompt-based approach
		 * instead.</li>
		 * </ul>
		 *
		 * 将 JSON Schema 作为 API 级别的约束直接传递给 AI 提供商，而不是将其拼接到提示文本中。
		 * 若底层 {@link org.springframework.ai.chat.model.ChatModel} 不支持
		 * {@link org.springframework.ai.model.tool.StructuredOutputChatOptions}，则此选项无效。
		 *
		 * <p>
		 * <b>默认不启用</b>，因为原生结构化输出能力因模型和提供商而异。已知限制：
		 * <ul>
		 * <li><b>Ollama</b>：内置推理/思考模式的模型（如 {@code qwen3:8b}、{@code qwen3.5:9b}）
		 * 可能返回纯文本而非 JSON，导致反序列化失败。可搭配 {@link #validateSchema()} 自动重试，
		 * 或改用非推理模型如 {@code llama3.1:latest}。</li>
		 * <li><b>OpenAI</b>：结构化输出 API 不支持顶层 JSON 数组 Schema。
		 * 启用此选项时请求 {@code List<T>} 会失败，
		 * 请将列表包装在容器记录中，或改用默认的基于提示文本的方式。</li>
		 * </ul>
		 */
		EntityParamSpec useProviderStructuredOutput();

		/**
		 * Validates the model's JSON response against the entity schema and retries with
		 * the error feedback on failure, up to {@code maxRepeatAttempts} times (default:
		 * 3). Streaming is not supported.
		 *
		 * 对模型返回的 JSON 响应进行 Schema 校验，失败时携带错误反馈自动重试，
		 * 最多重试 {@code maxRepeatAttempts} 次（默认：3 次）。不支持流式响应。
		 */
		EntityParamSpec validateSchema();

	}

	/**
	 * Specification for the synchronous call response.
	 * 同步调用响应的规范接口。
	 */
	interface CallResponseSpec {

		/**
		 * Deserializes the response into a {@code T} instance, with behaviour configured
		 * via the {@code entityParamSpecConsumer}.
		 * 将响应反序列化为 {@code T} 实例，行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param type the target parameterized type / 目标参数化类型
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(ParameterizedTypeReference<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Deserializes the response into a {@code T} instance.
		 * 将响应反序列化为 {@code T} 实例。
		 * @param type the target parameterized type / 目标参数化类型
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(ParameterizedTypeReference<T> type);

		/**
		 * Deserializes the response using the given converter, with behaviour configured
		 * via the {@code entityParamSpecConsumer}.
		 * 使用指定的转换器对响应进行反序列化，行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param structuredOutputConverter the converter for parsing and schema
		 * resolution / 用于解析和 Schema 生成的转换器
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Deserializes the response using the given converter.
		 * 使用指定的转换器对响应进行反序列化。
		 * @param structuredOutputConverter the converter for parsing and schema
		 * resolution / 用于解析和 Schema 生成的转换器
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter);

		/**
		 * Deserializes the response into a {@code T} instance, with behaviour configured
		 * via the {@code entityParamSpecConsumer}.
		 * 将响应反序列化为 {@code T} 实例，行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param type the target class / 目标类
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(Class<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Deserializes the response into a {@code T} instance.
		 * 将响应反序列化为 {@code T} 实例。
		 * @param type the target class / 目标类
		 * @return the deserialized entity, or {@code null} if the response is empty
		 * / 反序列化后的实体对象，响应为空时返回 {@code null}
		 */
		<T> @Nullable T entity(Class<T> type);

		/** 返回完整的 {@link ChatClientResponse} 对象。 */
		ChatClientResponse chatClientResponse();

		/** 返回 {@link ChatResponse} 对象，可能为 {@code null}。 */
		@Nullable ChatResponse chatResponse();

		/** 返回响应的文本内容，可能为 {@code null}。 */
		@Nullable String content();

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and a specific entity type, with behaviour
		 * configured via the {@code entityParamSpecConsumer}.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和指定类型的实体对象，
		 * 行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param type the target class / 目标类
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entity
		 * / 包含完整 {@link ChatResponse} 和反序列化实体的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and a specific entity type.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和指定类型的实体对象。
		 * @param type the target class / 目标类
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entity
		 * / 包含完整 {@link ChatResponse} 和反序列化实体的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type);

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and a specific entity type, with behaviour
		 * configured via the {@code entityParamSpecConsumer}.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和指定参数化类型的实体对象，
		 * 行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param type the target parameterized type / 目标参数化类型
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entity
		 * / 包含完整 {@link ChatResponse} 和反序列化实体的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and a {@link Collection} of entity types.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和实体集合。
		 * @param type the target parameterized type / 目标参数化类型
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entities
		 * / 包含完整 {@link ChatResponse} 和反序列化实体集合的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type);

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and an entity converted using a specified
		 * {@link StructuredOutputConverter}, with behaviour configured via the
		 * {@code entityParamSpecConsumer}.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和通过指定
		 * {@link StructuredOutputConverter} 转换的实体对象，行为可通过 {@code entityParamSpecConsumer} 配置。
		 * @param structuredOutputConverter the converter for parsing and schema
		 * resolution / 用于解析和 Schema 生成的转换器
		 * @param entityParamSpecConsumer configures options such as
		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
		 * {@link EntityParamSpec#validateSchema()}
		 * / 配置选项，如 {@link EntityParamSpec#useProviderStructuredOutput()} 和
		 * {@link EntityParamSpec#validateSchema()}
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entity
		 * / 包含完整 {@link ChatResponse} 和反序列化实体的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter,
				Consumer<EntityParamSpec> entityParamSpecConsumer);

		/**
		 * Returns a {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and an entity converted using a specified
		 * {@link StructuredOutputConverter}.
		 * 返回一个 {@link ResponseEntity}，同时包含完整的 {@link ChatResponse} 对象和通过指定
		 * {@link StructuredOutputConverter} 转换的实体对象。
		 * @param structuredOutputConverter the converter for parsing and schema
		 * resolution / 用于解析和 Schema 生成的转换器
		 * @return the {@link ResponseEntity} containing both the complete
		 * {@link ChatResponse} object and the deserialized entity
		 * / 包含完整 {@link ChatResponse} 和反序列化实体的 {@link ResponseEntity}
		 */
		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter);

	}

	/**
	 * Specification for the streaming response.
	 * 流式响应的规范接口。
	 */
	interface StreamResponseSpec {

		/** 返回 {@link ChatClientResponse} 的响应式流。 */
		Flux<ChatClientResponse> chatClientResponse();

		/** 返回 {@link ChatResponse} 的响应式流。 */
		Flux<ChatResponse> chatResponse();

		/** 返回文本内容的响应式流。 */
		Flux<String> content();

	}

	/**
	 * Specification for a chat client request, providing a fluent API for configuring
	 * the request and executing it synchronously or asynchronously.
	 * 聊天客户端请求的规范接口，提供流式 API 来配置请求并以同步或异步方式执行。
	 */
	interface ChatClientRequestSpec {

		/**
		 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
		 * settings are replicated from this {@link ChatClientRequest}.
		 * 返回一个 {@link ChatClient.Builder}，用于新建 {@link ChatClient} 实例，
		 * 该实例的配置基于当前 {@link ChatClientRequest} 复制生成。
		 */
		Builder mutate();

		/** 通过 {@link Consumer} 配置 Advisor。 */
		ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);

		/** 以可变参数形式添加一个或多个 Advisor。 */
		ChatClientRequestSpec advisors(Advisor... advisors);

		/** 以列表形式添加多个 Advisor。 */
		ChatClientRequestSpec advisors(List<Advisor> advisors);

		/** 以可变参数形式添加一个或多个消息。 */
		ChatClientRequestSpec messages(Message... messages);

		/** 以列表形式添加多个消息。 */
		ChatClientRequestSpec messages(List<Message> messages);

		/** 自定义聊天选项配置。 */
		<B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B customizer);

		/**
		 * Register one or more tools for this chat request. The method accepts a
		 * heterogeneous mix of tool representations and routes each element to the
		 * appropriate internal list automatically:
		 * 为本次聊天请求注册一个或多个工具。该方法接受混合类型的工具表示形式，
		 * 并自动将每个元素路由到对应的内部列表：
		 *
		 * <ul>
		 * <li>{@link org.springframework.ai.tool.ToolCallback} — registered directly as a
		 * callback. / 直接注册为回调。</li>
		 * <li>{@link org.springframework.ai.tool.ToolCallbackProvider} — registered
		 * directly as a provider; its callbacks are resolved lazily at request time.
		 * / 直接注册为提供者；其回调在请求时延迟解析。</li>
		 * <li>{@code ToolCallback[]} or {@code ToolCallbackProvider[]} — every element of
		 * the array is registered as above. / 数组中的每个元素按上述规则注册。</li>
		 * <li>{@link java.util.Collection} — iterated and each element is dispatched by
		 * the same rules. / 遍历集合并按相同规则分派每个元素。</li>
		 * <li>Any other object — treated as a {@code @Tool}-annotated POJO; a
		 * {@link org.springframework.ai.tool.ToolCallback} is generated for each
		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated method it
		 * contains. / 其他对象视为带 {@code @Tool} 注解的 POJO；
		 * 为其每个 {@link org.springframework.ai.tool.annotation.Tool} 注解方法生成
		 * {@link org.springframework.ai.tool.ToolCallback}。</li>
		 * </ul>
		 *
		 * <p>
		 * Mixed calls are fully supported:
		 * 完全支持混合调用：
		 *
		 * <pre>{@code
		 * chatClient.prompt()
		 *     .tools(new DateTimeTools(), existingCallback, myProvider)
		 *     .toolContext(Map.of("tenantId", "acme"))
		 *     .call().content();
		 * }</pre>
		 *
		 * <p>
		 * Tools registered here are available only for this specific request. Use
		 * {@link Builder#defaultTools(Object...)} to register tools that apply to every
		 * request built from the same {@link Builder}.
		 * 在此注册的工具仅对当前请求有效。使用 {@link Builder#defaultTools(Object...)}
		 * 可注册对同一 {@link Builder} 构建的所有请求生效的默认工具。
		 * @param tools tool objects to register; must not be {@code null} and must not
		 * contain {@code null} elements / 要注册的工具对象；不能为 {@code null} 且不能包含 {@code null} 元素
		 * @return this spec for chaining / 当前规范实例，支持链式调用
		 * @throws IllegalArgumentException if {@code tools} is {@code null}, contains
		 * {@code null} elements, or if a POJO argument has no
		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated methods
		 * / 当 {@code tools} 为 {@code null}、包含 {@code null} 元素、
		 * 或 POJO 参数没有 {@link org.springframework.ai.tool.annotation.Tool} 注解方法时抛出
		 */
		ChatClientRequestSpec tools(Object... tools);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
		 * in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #tools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
		 * in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #tools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
		 * in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #tools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		/** 设置工具调用的上下文参数。 */
		ChatClientRequestSpec toolContext(Map<String, Object> toolContext);

		/** 设置系统消息的文本内容。 */
		ChatClientRequestSpec system(String text);

		/** 从资源文件读取系统消息内容，并指定字符集。 */
		ChatClientRequestSpec system(Resource textResource, Charset charset);

		/** 从资源文件读取系统消息内容，使用默认字符集。 */
		ChatClientRequestSpec system(Resource text);

		/** 通过 {@link Consumer} 配置系统消息。 */
		ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);

		/** 设置用户消息的文本内容。 */
		ChatClientRequestSpec user(String text);

		/** 从资源文件读取用户消息内容，并指定字符集。 */
		ChatClientRequestSpec user(Resource text, Charset charset);

		/** 从资源文件读取用户消息内容，使用默认字符集。 */
		ChatClientRequestSpec user(Resource text);

		/** 通过 {@link Consumer} 配置用户消息。 */
		ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);

		/** 设置自定义的模板渲染器。 */
		ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer);

		/** 发起同步调用，返回 {@link CallResponseSpec}。 */
		CallResponseSpec call();

		/** 发起流式调用，返回 {@link StreamResponseSpec}。 */
		StreamResponseSpec stream();

	}

	/**
	 * A mutable builder for creating a {@link ChatClient}.
	 * 用于创建 {@link ChatClient} 的可变构建器。
	 */
	interface Builder {

		/** 以可变参数形式添加一个或多个默认 Advisor。 */
		Builder defaultAdvisors(Advisor... advisors);

		/** 通过 {@link Consumer} 配置默认 Advisor。 */
		Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);

		/** 以列表形式添加多个默认 Advisor。 */
		Builder defaultAdvisors(List<Advisor> advisors);

		/** 设置默认的聊天选项。 */
		Builder defaultOptions(ChatOptions.Builder chatOptions);

		/** 设置默认用户消息的文本内容。 */
		Builder defaultUser(String text);

		/** 从资源文件读取默认用户消息内容，并指定字符集。 */
		Builder defaultUser(Resource text, Charset charset);

		/** 从资源文件读取默认用户消息内容，使用默认字符集。 */
		Builder defaultUser(Resource text);

		/** 通过 {@link Consumer} 配置默认用户消息。 */
		Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);

		/** 设置默认系统消息的文本内容。 */
		Builder defaultSystem(String text);

		/** 从资源文件读取默认系统消息内容，并指定字符集。 */
		Builder defaultSystem(Resource text, Charset charset);

		/** 从资源文件读取默认系统消息内容，使用默认字符集。 */
		Builder defaultSystem(Resource text);

		/** 通过 {@link Consumer} 配置默认系统消息。 */
		Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);

		/** 设置默认的模板渲染器。 */
		Builder defaultTemplateRenderer(TemplateRenderer templateRenderer);

		/**
		 * Register one or more default tools that will be available to every request
		 * built from this {@link Builder}. The method accepts the same heterogeneous mix
		 * of tool representations as {@link ChatClientRequestSpec#tools(Object...)} and
		 * applies the same automatic dispatch rules:
		 * 注册一个或多个默认工具，这些工具对本 {@link Builder} 构建的每个请求都有效。
		 * 该方法接受与 {@link ChatClientRequestSpec#tools(Object...)} 相同的混合类型工具表示形式，
		 * 并应用相同的自动分派规则：
		 *
		 * <ul>
		 * <li>{@link org.springframework.ai.tool.ToolCallback} — registered directly as a
		 * callback. / 直接注册为回调。</li>
		 * <li>{@link org.springframework.ai.tool.ToolCallbackProvider} — registered
		 * directly as a provider; its callbacks are resolved lazily at request time.
		 * / 直接注册为提供者；其回调在请求时延迟解析。</li>
		 * <li>{@code ToolCallback[]} or {@code ToolCallbackProvider[]} — every element of
		 * the array is registered as above. / 数组中的每个元素按上述规则注册。</li>
		 * <li>{@link java.util.Collection} — iterated and each element is dispatched by
		 * the same rules. / 遍历集合并按相同规则分派每个元素。</li>
		 * <li>Any other object — treated as a {@code @Tool}-annotated POJO; a
		 * {@link org.springframework.ai.tool.ToolCallback} is generated for each
		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated method it
		 * contains. / 其他对象视为带 {@code @Tool} 注解的 POJO；
		 * 为其每个 {@link org.springframework.ai.tool.annotation.Tool} 注解方法生成
		 * {@link org.springframework.ai.tool.ToolCallback}。</li>
		 * </ul>
		 *
		 * <p>
		 * Default tools are shared across all requests produced by {@link ChatClient}
		 * instances built from this builder. If a request also provides its own tools via
		 * {@link ChatClientRequestSpec#tools(Object...)}, those runtime tools completely
		 * override the defaults for that request.
		 * 默认工具在该构建器创建的所有 {@link ChatClient} 实例产生的请求间共享。
		 * 如果某个请求通过 {@link ChatClientRequestSpec#tools(Object...)} 提供了自己的工具，
		 * 则运行时工具会完全覆盖该请求的默认工具。
		 *
		 * <p>
		 * WARNING: Because default tools are shared, be careful not to register tools
		 * that should only be available in specific contexts.
		 * 注意：由于默认工具是共享的，请勿注册仅在特定上下文中可用的工具。
		 * @param tools tool objects to register; must not be {@code null} and must not
		 * contain {@code null} elements / 要注册的工具对象；不能为 {@code null} 且不能包含 {@code null} 元素
		 * @return this builder for chaining / 当前构建器实例，支持链式调用
		 * @throws IllegalArgumentException if {@code tools} is {@code null}, contains
		 * {@code null} elements, or if a POJO argument has no
		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated methods
		 * / 当 {@code tools} 为 {@code null}、包含 {@code null} 元素、
		 * 或 POJO 参数没有 {@link org.springframework.ai.tool.annotation.Tool} 注解方法时抛出
		 */
		Builder defaultTools(Object... tools);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
		 * removed in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #defaultTools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(ToolCallback... toolCallbacks);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
		 * removed in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #defaultTools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks);

		/**
		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
		 * removed in 3.0.0.
		 * 自 2.0.0 起已弃用，请使用 {@link #defaultTools(Object...)}。将在 3.0.0 中移除。
		 */
		@Deprecated(since = "2.0.0", forRemoval = true)
		Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		/** 设置默认的工具调用上下文参数。 */
		Builder defaultToolContext(Map<String, Object> toolContext);

		/** 克隆当前构建器。 */
		Builder clone();

		/** 构建并返回 {@link ChatClient} 实例。 */
		ChatClient build();

	}

}
