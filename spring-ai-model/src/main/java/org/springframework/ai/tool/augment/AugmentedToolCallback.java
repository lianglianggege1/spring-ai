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

package org.springframework.ai.tool.augment;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.ToolInputSchemaAugmenter.AugmentedArgumentType;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.Assert;

/**
 * This class wraps an existing {@link ToolCallback} and modifies its input schema to
 * include additional fields defined in the provided Record type. It also provides a
 * mechanism to handle these extended arguments, either by consuming them via a provided
 * {@link Consumer} or by removing them from the input after processing.
 *
 * @author Christian Tzolov
 */
/**
 * 【中文说明】{@code AugmentedToolCallback}：一个使用<b>装饰器模式</b>的 {@link ToolCallback} 包装类。
 *
 * <p>
 * <b>它解决什么问题？</b>有时我们希望模型在调用工具时，除了工具本身需要的参数外，
 * 再额外提供一些「旁路信息」（如：调用该工具的理由、置信度、意图分类）。
 * 这些信息对业务侧有价值（审计、监控、路由），但目标工具方法本身并不需要它们。
 * </p>
 *
 * <p>
 * <b>工作原理（三步）：</b>
 * </p>
 * <ol>
 * <li><b>改写 schema</b>：构造时把 {@code T}（一个 record）的字段合并进被包装工具的 inputSchema，
 * 于是模型看到的入参多了这些额外字段。</li>
 * <li><b>提取并消费</b>：调用时把模型传回的 JSON 解析成 {@code T}，包装成
 * {@link AugmentedArgumentEvent} 交给用户的 {@code Consumer} 处理。</li>
 * <li><b>可选剔除</b>：若 {@code removeAugmentedArgumentsAfterProcessing} 为 true，
 * 则从 JSON 中删掉这些额外字段后再转发给被包装的工具，避免其反序列化时出现未知字段。</li>
 * </ol>
 *
 * <p>
 * <b>泛型约束 {@code <T extends Record>}：</b>增强参数必须是 record 类型，
 * 因为实现依赖 {@code getRecordComponents()} 来反射字段名与类型；构造器中还会再次断言校验。
 * </p>
 *
 * <p>
 * <b>典型用法：</b>
 * </p>
 *
 * <pre>{@code
 * record Reasoning(@ToolParam(description = "调用该工具的理由") String reason) {
 * }
 *
 * ToolCallback augmented = new AugmentedToolCallback<>(originalCallback, Reasoning.class,
 *         event -> log.info("模型理由: {}", event.arguments().reason()), true);
 * }</pre>
 *
 * @author Christian Tzolov
 * @see AugmentedToolCallbackProvider
 * @see ToolInputSchemaAugmenter
 */
public class AugmentedToolCallback<T extends Record> implements ToolCallback {

	// 中文：JSON 工具，static final 复用单例。
	private static final JsonHelper jsonHelper = new JsonHelper();

	/**
	 * The delegate ToolCallback that this class extends.
	 */
	// 中文：被装饰（委托）的原始工具回调，真正的业务执行仍由它完成。
	private final ToolCallback delegate;

	/**
	 * The augmented ToolDefinition that includes the augmented input schema.
	 */
	// 中文：增强后的工具定义。名称与描述沿用原工具，仅 inputSchema 被扩充。
	// 对外暴露的是这份定义，因此模型看到的入参会多出增强字段。
	private final ToolDefinition augmentedToolDefinition;

	/**
	 * The record class type that defines the structure of the augmented arguments.
	 */
	// 中文：增强参数的 record 类型，用于把模型传回的 JSON 反序列化成强类型对象。
	private final Class<T> augmentedArgumentsClass;

	/**
	 * A consumer that processes the augmented arguments extracted from the tool input.
	 */
	// 中文：增强参数的消费者（可为 null）。为 null 时表示只改 schema、不做提取处理，
	// 此时增强字段会原样随 JSON 传给被装饰的工具。
	private final @Nullable Consumer<AugmentedArgumentEvent<T>> augmentedArgumentsConsumer;

	/**
	 * The list of tool argument types that have been added to the tool input schema.
	 */
	// 中文：已追加到 schema 的增强字段元信息列表（名称/类型/描述/是否必填）。
	// 在剔除字段时需要按这里的 name 逐个从 JSON 中移除。
	private final List<AugmentedArgumentType> augmentedArgumentTypes;

	/**
	 * A flag indicating whether to remove the augmented arguments from the tool input
	 * after they have been processed. If the arguments are not removed, they will remain
	 * in the tool input for the delegate to process. In many cases this could be useful.
	 */
	// 中文：处理完增强参数后，是否把它们从 JSON 中剔除再转发给原工具。
	// false（默认）：保留，原工具也能看到这些字段（某些场景下确实有用）；
	// true：剔除，避免原工具反序列化时遇到未知字段而报错。
	private boolean removeAugmentedArgumentsAfterProcessing = false;

	// 中文：构造器。所有"schema 改写"的重活都在这里一次性完成，运行期调用时不再重复计算。
	public AugmentedToolCallback(ToolCallback delegate, Class<T> augmentedArgumentsClass,
			@Nullable Consumer<AugmentedArgumentEvent<T>> augmentedArgumentsConsumer,
			boolean removeExtraArgumentsAfterProcessing) {
		Assert.notNull(delegate, "Delegate ToolCallback must not be null");
		Assert.notNull(augmentedArgumentsClass, "Argument types must not be null");
		// 中文：运行期再次确认必须是 record 类型 —— 泛型 <T extends Record> 只在编译期生效，
		// 若调用方用原始类型或反射绕过，这里可兜底拦截。后续依赖 getRecordComponents() 反射字段。
		Assert.isTrue(augmentedArgumentsClass.isRecord(), "Argument types must be a Record type");
		// 中文：至少要有一个字段，否则"增强"没有任何意义。
		Assert.isTrue(augmentedArgumentsClass.getRecordComponents().length > 0,
				"Argument types must have at least one field");

		this.delegate = delegate;
		// 中文：第一步 —— 反射 record 组件，提取出字段名/类型/描述/required 元信息。
		this.augmentedArgumentTypes = ToolInputSchemaAugmenter.toAugmentedArgumentTypes(augmentedArgumentsClass);
		String originalSchema = this.delegate.getToolDefinition().inputSchema();
		// 中文：第二步 —— 把增强字段合并进原始 JSON Schema。
		String augmentedSchema = ToolInputSchemaAugmenter.augmentToolInputSchema(originalSchema,
				this.augmentedArgumentTypes);
		// 中文：第三步 —— 构造新的工具定义。注意：name 与 description 完全沿用原工具，
		// 这样对模型而言仍是"同一个工具"，只是参数变多了。
		this.augmentedToolDefinition = ToolDefinition.builder()
			.name(this.delegate.getToolDefinition().name())
			.description(this.delegate.getToolDefinition().description())
			.inputSchema(augmentedSchema)
			.build();

		this.augmentedArgumentsClass = augmentedArgumentsClass;
		this.augmentedArgumentsConsumer = augmentedArgumentsConsumer;
		this.removeAugmentedArgumentsAfterProcessing = removeExtraArgumentsAfterProcessing;
	}

	// 中文：对外返回"增强后"的定义，而非原始定义 —— 这是整个装饰器生效的关键。
	@Override
	public ToolDefinition getToolDefinition() {
		return this.augmentedToolDefinition;
	}

	// 中文：不带上下文的调用。先处理增强参数，再把（可能已剔除字段的）输入转发给被装饰工具。
	@Override
	public String call(String toolInput) {
		return this.delegate.call(this.handleAugmentedArguments(toolInput));
	}

	// 中文：带工具上下文的调用，逻辑同上，仅多透传一个 ToolContext。
	// 注意参数名 tooContext 是源码中的拼写（少了一个 l），仅为形参名，不影响功能。
	@Override
	public String call(String toolInput, @Nullable ToolContext tooContext) {
		return this.delegate.call(this.handleAugmentedArguments(toolInput), tooContext);
	}

	/**
	 * Handles the augmented arguments in the tool input. It extracts the augmented
	 * arguments from the tool input, processes them using the provided consumer, and
	 * optionally removes them from the tool input.
	 * @param toolInput the input as received from the LLM.
	 * @return the input to send to the delegate ToolCallback
	 */
	// 中文：处理增强参数的核心私有方法。
	// 入参是模型给出的原始 JSON，返回值是要转发给被装饰工具的 JSON（可能已剔除增强字段）。
	private String handleAugmentedArguments(String toolInput) {

		// Extract the augmented arguments from the toolInput and send them to the
		// consumer if provided.
		// 中文：阶段一 —— 提取并消费。仅在注册了消费者时才做反序列化，避免无谓开销。
		if (this.augmentedArgumentsConsumer != null) {
			// 中文：把 JSON 解析成增强参数 record。JSON 中多余的（属于原工具的）字段会被忽略。
			T augmentedArguments = jsonHelper.fromJson(toolInput, this.augmentedArgumentsClass);
			// 中文：注意事件里带的是"原始"toolInput，便于消费者做完整审计。
			this.augmentedArgumentsConsumer
				.accept(new AugmentedArgumentEvent<>(this.augmentedToolDefinition, toolInput, augmentedArguments));
		}

		// Optionally remove the extra arguments from the toolInput
		// 中文：阶段二 —— 可选剔除。把增强字段从 JSON 中删掉，
		// 使被装饰的原工具看到的入参与它原本的 schema 完全一致。
		if (this.removeAugmentedArgumentsAfterProcessing) {
			// 中文：转成 Map 便于按 key 删除。
			var args = jsonHelper.fromJsonToMap(toolInput);

			// 中文：按构造时解析出的字段名逐个移除。
			for (AugmentedArgumentType newFieldType : this.augmentedArgumentTypes) {
				args.remove(newFieldType.name());
			}
			// 中文：重新序列化回 JSON 字符串。这里对形参 toolInput 重新赋值，
			// 使下面的 return 能统一返回"处理后"的结果。
			toolInput = jsonHelper.toJson(args);
		}

		// 中文：若未开启剔除，则原样返回，增强字段会一并传给被装饰工具。
		return toolInput;
	}

}
