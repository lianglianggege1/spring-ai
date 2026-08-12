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
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A set of options that can be used to configure the interaction with a chat model,
 * including tool calling.
 *
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】带「工具调用（Tool Calling）」能力的聊天模型选项接口。
 *
 * <p>
 * 它在通用的 {@link ChatOptions}（temperature / maxTokens / topP 等采样参数）之上，
 * 追加了工具调用相关的两块配置：
 * <ul>
 * <li><b>toolCallbacks</b>：注册到 ChatModel 的工具回调列表，每个 {@link ToolCallback}
 * 既描述了工具元信息（名称、描述、入参 JSON Schema），也承载了实际执行逻辑。</li>
 * <li><b>toolContext</b>：工具上下文，一个 {@code Map<String, Object>}，在工具执行时透传给工具，
 * 用于携带不希望暴露给大模型的数据（如当前用户 ID、租户信息、数据库连接等）。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ToolCallingChatOptions options = ToolCallingChatOptions.builder()
 *         .model("gpt-4o")
 *         .toolCallbacks(weatherToolCallback)
 *         .toolContext("userId", "1024")
 *         .build();
 * chatModel.call(new Prompt("北京天气如何？", options));
 * }</pre>
 *
 * <p>
 * 接口中还提供了三个静态工具方法，供各家模型的 XxxChatModel 在
 * 「运行时选项」与「默认选项」合并时复用：{@code mergeToolCallbacks}、{@code mergeToolContext}、
 * {@code validateToolCallbacks}。
 *
 * <p>
 * 对应英文 javadoc 中的标签：{@code @author} Thomas Vitale、Ilayaperumal Gopinathan、Christian
 * Tzolov；{@code @since} 1.0.0。
 */
public interface ToolCallingChatOptions extends ChatOptions {

	/**
	 * ToolCallbacks to be registered with the ChatModel.
	 */
	/**
	 * 【中文说明】获取注册到 ChatModel 的工具回调列表。
	 * @return 工具回调列表；未配置任何工具时可能为 {@code null}（注意判空）
	 */
	@Nullable List<ToolCallback> getToolCallbacks();

	/**
	 * Get the configured tool context.
	 * @return the tool context map.
	 */
	/**
	 * 【中文说明】获取已配置的工具上下文。
	 *
	 * <p>
	 * 该 Map 会在工具执行阶段被包装成 {@code ToolContext} 传给工具方法，内容不会发送给大模型。
	 * @return 工具上下文 Map；未配置时可能为 {@code null}
	 */
	@Nullable Map<String, Object> getToolContext();

	/**
	 * Returns a new {@link ToolCallingChatOptions.Builder} initialized with the values of
	 * this {@link ToolCallingChatOptions}.
	 *
	 * Narrows the return type of {@link ChatOptions#mutate()} so generic tool calling
	 * code can chain methods without casting.
	 */
	/**
	 * 【中文说明】返回一个以当前对象各字段值预填充的 Builder，用于「基于现有选项做局部修改」。
	 *
	 * <p>
	 * 这里通过<b>协变返回类型</b>把父接口 {@link ChatOptions#mutate()} 的返回值收窄为
	 * {@code ToolCallingChatOptions.Builder}，好处是通用的工具调用代码可以直接链式调用
	 * {@code options.mutate().toolCallbacks(...).build()}，无需强制类型转换。
	 */
	@Override
	ToolCallingChatOptions.Builder<?> mutate();

	/**
	 * A builder to create a new {@link ToolCallingChatOptions} instance.
	 */
	/**
	 * 【中文说明】创建默认实现（{@link DefaultToolCallingChatOptions}）的 Builder 入口。
	 */
	static ToolCallingChatOptions.Builder<?> builder() {
		return new DefaultToolCallingChatOptions.Builder<>();
	}

	/**
	 * 【中文说明】合并「运行时工具回调」与「默认工具回调」。
	 *
	 * <p>
	 * 合并策略是<b>覆盖而非叠加</b>：只要运行时传入了非空列表，就完全以运行时为准，忽略默认值；
	 * 运行时为空才回退到默认值。返回的都是不可变副本（{@code List.copyOf}），避免外部修改影响内部状态。
	 * @param runtimeToolCallbacks 单次请求（Prompt）中携带的工具回调
	 * @param defaultToolCallbacks 构建 ChatModel 时配置的默认工具回调
	 * @return 合并后的不可变列表；两者都为空时返回 {@code null}
	 */
	static @Nullable List<ToolCallback> mergeToolCallbacks(@Nullable List<ToolCallback> runtimeToolCallbacks,
			@Nullable List<ToolCallback> defaultToolCallbacks) {
		// 运行时未指定工具：回退到默认工具列表（拷贝为不可变列表）
		if (CollectionUtils.isEmpty(runtimeToolCallbacks)) {
			return defaultToolCallbacks != null ? List.copyOf(defaultToolCallbacks) : null;
		}
		// 运行时已指定：整体覆盖默认值，不做逐项合并
		return List.copyOf(runtimeToolCallbacks);
	}

	/**
	 * 【中文说明】合并「运行时工具上下文」与「默认工具上下文」。
	 *
	 * <p>
	 * 与工具回调的「整体覆盖」不同，这里是<b>按 key 逐项合并</b>：先放入默认值，再用运行时值覆盖同名 key，
	 * 因此运行时优先级更高。合并前会校验两侧的 key 都不为 null。
	 * @param runtimeToolContext 单次请求携带的上下文
	 * @param defaultToolContext ChatModel 的默认上下文
	 * @return 合并后的不可变 Map；两者都为空时返回 {@code null}
	 */
	static @Nullable Map<String, Object> mergeToolContext(@Nullable Map<String, Object> runtimeToolContext,
			@Nullable Map<String, Object> defaultToolContext) {
		// 运行时上下文为空：直接使用默认上下文
		if (CollectionUtils.isEmpty(runtimeToolContext)) {
			return defaultToolContext != null ? Map.copyOf(defaultToolContext) : null;
		}
		// 参数校验：key 不允许为 null，否则后续 Map.copyOf 会抛异常且难以定位
		Assert.noNullElements(runtimeToolContext.keySet(), "runtimeToolContext keys cannot be null");
		// 默认上下文为空：直接使用运行时上下文
		if (CollectionUtils.isEmpty(defaultToolContext)) {
			return Map.copyOf(runtimeToolContext);
		}
		Assert.noNullElements(defaultToolContext.keySet(), "defaultToolContext keys cannot be null");
		// 以默认上下文为底，再用运行时上下文覆盖同名 key（运行时优先级更高）
		var mergedToolContext = new java.util.HashMap<>(defaultToolContext);
		mergedToolContext.putAll(runtimeToolContext);
		return Map.copyOf(mergedToolContext);
	}

	/**
	 * 【中文说明】校验工具回调列表的合法性：<b>不允许出现同名工具</b>。
	 *
	 * <p>
	 * 因为大模型是按「工具名」来发起调用的，重名会导致无法唯一定位到具体工具，
	 * 所以这里提前快速失败（fail-fast），抛出 {@link IllegalStateException} 并列出所有重复的名称。
	 * @param toolCallbacks 待校验的工具回调列表，为空时直接跳过校验
	 */
	static void validateToolCallbacks(@Nullable List<ToolCallback> toolCallbacks) {
		// 空列表无需校验
		if (CollectionUtils.isEmpty(toolCallbacks)) {
			return;
		}
		// 找出所有重复的工具名称
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			// 存在重名工具：直接抛异常，避免运行期出现无法路由的调用
			throw new IllegalStateException("Multiple tools with the same name (%s) found in ToolCallingChatOptions"
				.formatted(String.join(", ", duplicateToolNames)));
		}
	}

	/**
	 * A builder to create a {@link ToolCallingChatOptions} instance.
	 */
	/**
	 * 【中文说明】用于构建 {@link ToolCallingChatOptions} 的 Builder 接口。
	 *
	 * <p>
	 * 泛型签名 {@code Builder<B extends Builder<B>>} 是<b>递归泛型（自限定类型 / CRTP）</b>模式：
	 * 类型参数 B 指向「子类自己」，这样父接口里定义的 {@code model()}、{@code temperature()} 等方法
	 * 就能返回子类型 B，而不是父类型，从而保证子类扩展方法与父类方法可以任意顺序链式调用，
	 * 且中途不会丢失类型信息、无需强制转换。
	 *
	 * <p>
	 * 下方 {@code // ChatOptions.Builder methods} 之后的一组 {@code @Override} 方法并没有新增功能，
	 * 纯粹是为了在本接口中重新声明、收窄返回类型，便于 IDE 补全和链式调用。
	 */
	interface Builder<B extends Builder<B>> extends ChatOptions.Builder<B> {

		/**
		 * ToolCallbacks to be registered with the ChatModel.
		 */
		/**
		 * 【中文说明】以 List 形式设置工具回调，语义为<b>整体替换</b>（传 {@code null} 表示清空）。
		 */
		B toolCallbacks(@Nullable List<ToolCallback> toolCallbacks);

		/**
		 * ToolCallbacks to be registered with the ChatModel.
		 */
		/**
		 * 【中文说明】以可变参数形式设置工具回调，语义为<b>追加</b>到已有列表末尾。
		 *
		 * <p>
		 * 注意与上面的 List 重载区分：List 版本是替换，可变参数版本是追加。
		 */
		B toolCallbacks(ToolCallback... toolCallbacks);

		/**
		 * Add a {@link Map} of context values into tool context.
		 * @param context the map representing the tool context.
		 * @return the {@link ToolCallingChatOptions} Builder.
		 */
		/**
		 * 【中文说明】批量向工具上下文中放入键值对（相当于 {@code putAll}）。
		 * @param context 要合并进工具上下文的 Map，传 {@code null} 表示清空上下文
		 * @return 当前 Builder，便于链式调用
		 */
		B toolContext(@Nullable Map<String, Object> context);

		/**
		 * Add a specific key/value pair to the tool context.
		 * @param key the key to use.
		 * @param value the corresponding value.
		 * @return the {@link ToolCallingChatOptions} Builder.
		 */
		/**
		 * 【中文说明】向工具上下文中放入单个键值对，key 与 value 均不允许为空。
		 * @param key 上下文键
		 * @param value 上下文值
		 * @return 当前 Builder，便于链式调用
		 */
		B toolContext(String key, Object value);

		// ChatOptions.Builder methods
		// 【中文说明】以下方法均继承自 ChatOptions.Builder，此处重新声明仅为收窄返回类型为 B

		@Override
		B model(@Nullable String model);

		@Override
		B frequencyPenalty(@Nullable Double frequencyPenalty);

		@Override
		B maxTokens(@Nullable Integer maxTokens);

		@Override
		B presencePenalty(@Nullable Double presencePenalty);

		@Override
		B stopSequences(@Nullable List<String> stopSequences);

		@Override
		B temperature(@Nullable Double temperature);

		@Override
		B topK(@Nullable Integer topK);

		@Override
		B topP(@Nullable Double topP);

		// 【中文说明】构建最终选项对象，返回类型收窄为 ToolCallingChatOptions
		@Override
		ToolCallingChatOptions build();

	}

}
