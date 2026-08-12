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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.SpringAiKind;
import org.springframework.util.Assert;

/**
 * Default conventions to populate observations for tool calling operations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolCallingObservationConvention} 的默认实现：规定工具调用观测的 指标名称与各项标签。
 *
 * <p>
 * 产出内容：
 * <ul>
 * <li><b>指标名</b>：默认 {@code "spring.ai.tool"}，可通过构造器自定义；</li>
 * <li><b>上下文名</b>（用于链路追踪的 span 名）：{@code "execute_tool 工具名"}；</li>
 * <li><b>低基数标签</b>：操作类型、提供方、Spring AI 种类、工具类型、工具名——取值有限，适合做指标维度；</li>
 * <li><b>高基数标签</b>：工具描述、入参 Schema、调用 ID——取值近乎无限，只适合进链路追踪。</li>
 * </ul>
 *
 * <p>
 * 扩展方式：各个标签的生成方法都声明为 {@code protected}，子类可<b>按需重写单个标签</b>
 * 而不必重写整个 {@code getLowCardinalityKeyValues} 方法。
 *
 * <p>
 * 注意：入参与结果的<b>具体内容</b>不在此处记录（涉及敏感数据），需要时由
 * {@link ToolCallingContentObservationFilter} 单独开启。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class DefaultToolCallingObservationConvention implements ToolCallingObservationConvention {

	// 默认指标名称
	public static final String DEFAULT_NAME = "spring.ai.tool";

	private final String name;

	// 无参构造器：使用默认指标名
	public DefaultToolCallingObservationConvention() {
		this(DEFAULT_NAME);
	}

	// 带参构造器：允许自定义指标名
	public DefaultToolCallingObservationConvention(String name) {
		this.name = name;
	}

	// 返回指标名称（对应 Micrometer 中的 metric name）
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * 【中文说明】生成「上下文名称」，即链路追踪中 span 的可读名称。 格式为 {@code "execute_tool <工具名>"}，例如
	 * {@code "execute_tool getWeather"}。
	 * @param context 观测上下文
	 * @return span 名称
	 */
	@Override
	public String getContextualName(ToolCallingObservationContext context) {
		Assert.notNull(context, "context cannot be null");
		String toolName = context.getToolDefinition().name();
		return "%s %s".formatted(AiOperationType.EXECUTE_TOOL.value(), toolName);
	}

	/**
	 * 【中文说明】组装低基数标签集合。
	 * <p>
	 * 低基数（low cardinality）指取值种类有限，可安全用作监控系统的指标维度； 若把调用 ID 这类高基数值放进来，会导致时间序列爆炸。
	 * @param context 观测上下文
	 * @return 低基数标签集合
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
		return KeyValues.of(aiOperationType(context), aiProvider(context), springAiKind(context), toolType(context),
				toolDefinitionName(context));
	}

	// 标签：工具类型（如 "function"）；protected 便于子类重写
	protected KeyValue toolType(ToolCallingObservationContext context) {
		return KeyValue.of(ToolCallingObservationDocumentation.LowCardinalityKeyNames.TOOL_TYPE, context.getToolType());
	}

	// 标签：AI 操作类型，此处恒为 "execute_tool"
	protected KeyValue aiOperationType(ToolCallingObservationContext context) {
		return KeyValue.of(ToolCallingObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
				context.getOperationMetadata().operationType());
	}

	// 标签：提供方，此处恒为 "spring_ai"（工具在本地执行，不归属某个模型厂商）
	protected KeyValue aiProvider(ToolCallingObservationContext context) {
		return KeyValue.of(ToolCallingObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
				context.getOperationMetadata().provider());
	}

	// 标签：Spring AI 种类，固定为 TOOL_CALL，用于区分聊天、向量检索等不同观测
	protected KeyValue springAiKind(ToolCallingObservationContext context) {
		return KeyValue.of(ToolCallingObservationDocumentation.LowCardinalityKeyNames.SPRING_AI_KIND,
				SpringAiKind.TOOL_CALL.value());
	}

	// 标签：工具名称。这是排查问题时最常用的维度（可按工具名聚合耗时、错误率）
	protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
		String toolName = context.getToolDefinition().name();
		return KeyValue.of(ToolCallingObservationDocumentation.LowCardinalityKeyNames.TOOL_DEFINITION_NAME, toolName);
	}

	/**
	 * 【中文说明】组装高基数标签集合。
	 * <p>
	 * 写法上与低基数不同：从 {@code KeyValues.empty()} 出发，逐个方法链式追加。 由于 {@link KeyValues}
	 * 是<b>不可变</b>的，每次 {@code and(...)} 都返回新实例， 因此必须用返回值覆盖变量，不能只调用而丢弃结果。
	 * @param context 观测上下文
	 * @return 高基数标签集合
	 */
	@Override
	public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
		var keyValues = KeyValues.empty();
		keyValues = toolDefinitionDescription(keyValues, context);
		keyValues = toolDefinitionSchema(keyValues, context);
		keyValues = toolCallId(keyValues, context);
		return keyValues;
	}

	// 高基数标签：工具描述文本（内容较长，不适合作指标维度）
	protected KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
		String toolDescription = context.getToolDefinition().description();
		return keyValues.and(
				ToolCallingObservationDocumentation.HighCardinalityKeyNames.TOOL_DEFINITION_DESCRIPTION.asString(),
				toolDescription);
	}

	// 高基数标签：入参 JSON Schema，便于排查「模型为何传错参数」
	protected KeyValues toolDefinitionSchema(KeyValues keyValues, ToolCallingObservationContext context) {
		String toolSchema = context.getToolDefinition().inputSchema();
		return keyValues.and(
				ToolCallingObservationDocumentation.HighCardinalityKeyNames.TOOL_DEFINITION_SCHEMA.asString(),
				toolSchema);
	}

	// 高基数标签：本次调用的唯一 ID —— 每次调用都不同，是典型的高基数值，
	// 可用于把一次工具调用与对应的模型请求关联起来
	protected KeyValues toolCallId(KeyValues keyValues, ToolCallingObservationContext context) {
		String toolCallId = context.getToolCallId();
		return keyValues.and(ToolCallingObservationDocumentation.HighCardinalityKeyNames.TOOL_CALL_ID.asString(),
				toolCallId);
	}

}
