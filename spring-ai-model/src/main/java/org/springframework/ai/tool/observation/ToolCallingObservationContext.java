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

package org.springframework.ai.tool.observation;

import io.micrometer.observation.Observation;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Context used to store data for tool calling observations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用观测上下文：在一次工具调用的可观测性流程中，作为「数据载体」 在调用前后传递，供 Convention 读取并生成指标标签。
 *
 * <p>
 * 继承自 Micrometer 的 {@link Observation.Context}，因此天然具备存放低/高基数标签的能力。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code operationMetadata}：固定为「执行工具 + Spring AI 提供方」，用于统一的 AI 语义约定；</li>
 * <li>{@code toolDefinition}/{@code toolMetadata}：工具的定义与元数据；</li>
 * <li>{@code toolType}：工具类型，默认 {@code "function"}；</li>
 * <li>{@code toolCallId}：本次调用的 ID（由模型生成）；</li>
 * <li>{@code toolCallArguments}：入参 JSON，默认 {@code "{}"}；</li>
 * <li>{@code toolCallResult}：<b>唯一的可变字段</b>，因为结果要等工具执行完毕后才能回填。</li>
 * </ul>
 *
 * <p>
 * 典型用法：调用前用 Builder 构建上下文并开启 Observation，调用后通过
 * {@link #setToolCallResult(String)} 回填结果再结束 Observation。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class ToolCallingObservationContext extends Observation.Context {

	// 固定的操作元数据：操作类型=执行工具，提供方=Spring AI（工具在本地执行，而非某个模型厂商）
	private final AiOperationMetadata operationMetadata = new AiOperationMetadata(AiOperationType.EXECUTE_TOOL.value(),
			AiProvider.SPRING_AI.value());

	private final ToolDefinition toolDefinition;

	private final ToolMetadata toolMetadata;

	private final String toolType;

	private final String toolCallId;

	private final String toolCallArguments;

	// 唯一可变字段：工具执行完成后才能回填结果
	private @Nullable String toolCallResult;

	/**
	 * 【中文说明】私有构造器，强制通过 {@link Builder} 创建实例。
	 * <p>
	 * 空值处理策略：对三个可空的字符串参数使用 {@code StringUtils.hasText} 判断并给出默认值，
	 * 使对外暴露的 getter 都能返回非 null，从而让下游生成标签时无需再做判空。
	 */
	private ToolCallingObservationContext(ToolDefinition toolDefinition, ToolMetadata toolMetadata,
			@Nullable String toolType, @Nullable String toolCallId, @Nullable String toolCallArguments,
			@Nullable String toolCallResult) {
		// 两个必填项做非空校验
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolMetadata, "toolMetadata cannot be null");

		this.toolDefinition = toolDefinition;
		this.toolMetadata = toolMetadata;
		// 默认类型 "function"，与 OpenAI 等厂商的工具类型命名保持一致
		this.toolType = StringUtils.hasText(toolType) ? toolType : "function";
		// 无调用 ID 时用空串占位，避免标签值为 null 导致监控系统报错
		this.toolCallId = StringUtils.hasText(toolCallId) ? toolCallId : "";
		// 无入参时用空 JSON 对象 "{}" 占位，保证该标签始终是合法 JSON
		this.toolCallArguments = StringUtils.hasText(toolCallArguments) ? toolCallArguments : "{}";
		// 结果允许为 null（调用尚未完成，或工具无返回值）
		this.toolCallResult = toolCallResult;
	}

	// 获取 AI 操作元数据（操作类型与提供方），用于生成 AI 通用语义标签
	public AiOperationMetadata getOperationMetadata() {
		return this.operationMetadata;
	}

	// 获取工具定义：名称、描述、入参 Schema
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	// 获取工具元数据，如 returnDirect
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	// 获取本次调用的 ID；未提供时为空串而非 null
	public String getToolCallId() {
		return this.toolCallId;
	}

	// 获取工具类型；默认为 "function"
	public String getToolType() {
		return this.toolType;
	}

	// 获取入参 JSON；未提供时为 "{}" 而非 null
	public String getToolCallArguments() {
		return this.toolCallArguments;
	}

	/**
	 * 【中文说明】获取工具执行结果。 返回值可为 null——工具尚未执行完毕，或工具本身无返回值。
	 * @return 结果字符串，可为 null
	 */
	public @Nullable String getToolCallResult() {
		return this.toolCallResult;
	}

	/**
	 * 【中文说明】回填工具执行结果。
	 * <p>
	 * 这是本类唯一的 setter：上下文在调用<b>前</b>创建，而结果只有在调用<b>后</b>才产生， 因此必须留一个可写入口。
	 * @param toolCallResult 执行结果，可为 null
	 */
	public void setToolCallResult(@Nullable String toolCallResult) {
		this.toolCallResult = toolCallResult;
	}

	/**
	 * 【中文说明】获取 Builder，这是创建本类实例的唯一途径（构造器为 private）。
	 * @return 新的 Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】上下文构建器。
	 * <p>
	 * 约束说明：{@code toolDefinition} 是<b>唯一必填项</b>（在 {@link #build()} 中校验）；
	 * {@code toolMetadata} 有默认值；其余字段均可省略，缺省逻辑在构造器中统一处理。
	 */
	public static final class Builder {

		// 必填项：初始为 null，build() 时校验
		private @Nullable ToolDefinition toolDefinition;

		// 可选项：预置一份默认元数据，避免使用方必须显式设置
		private ToolMetadata toolMetadata = ToolMetadata.builder().build();

		private @Nullable String toolType;

		private @Nullable String toolCallId;

		private @Nullable String toolCallArguments;

		private @Nullable String toolCallResult;

		// 私有构造器：只能通过外部类的 builder() 方法获得
		private Builder() {
		}

		// 设置工具定义（必填）
		public Builder toolDefinition(ToolDefinition toolDefinition) {
			this.toolDefinition = toolDefinition;
			return this;
		}

		// 设置工具元数据（可选，默认值已预置）
		public Builder toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		// 设置工具类型（可选，缺省为 "function"）
		// 注意：参数名为 toolCallType，与字段名 toolType 不一致，属于源码中的命名小瑕疵
		public Builder toolType(String toolCallType) {
			this.toolType = toolCallType;
			return this;
		}

		// 设置工具调用 ID（可选，缺省为空串）
		public Builder toolCallId(String toolCallId) {
			this.toolCallId = toolCallId;
			return this;
		}

		// 设置入参 JSON（可选，缺省为 "{}"）
		public Builder toolCallArguments(String toolCallArguments) {
			this.toolCallArguments = toolCallArguments;
			return this;
		}

		// 设置执行结果（可选，通常在调用完成后通过 setter 回填而非在此设置）
		public Builder toolCallResult(@Nullable String toolCallResult) {
			this.toolCallResult = toolCallResult;
			return this;
		}

		/**
		 * 【中文说明】构建上下文实例。
		 * @return 构建完成的观测上下文
		 * @throws IllegalArgumentException 当未设置 toolDefinition 时抛出
		 */
		public ToolCallingObservationContext build() {
			// 必填校验：提前失败，避免把 null 传入构造器
			Assert.notNull(this.toolDefinition, "toolDefinition cannot be null");
			return new ToolCallingObservationContext(this.toolDefinition, this.toolMetadata, this.toolType,
					this.toolCallId, this.toolCallArguments, this.toolCallResult);
		}

	}

}
