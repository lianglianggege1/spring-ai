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

package org.springframework.ai.image.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.image.ImageOptions;
import org.springframework.util.StringUtils;

/**
 * Default conventions to populate observations for image model operations.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * {@link ImageModelObservationConvention} 的默认实现，负责为图像模型调用填充观测标签。
 * <p>
 * 核心职责是把 {@link ImageModelObservationContext} 中的请求信息翻译成两类标签：
 * <ul>
 * <li><b>低基数标签（LowCardinality）</b> —— 取值种类少（操作类型、服务商、模型名），
 * 会作为 metrics 指标的维度，因此必须保证有界，否则会造成指标爆炸。</li>
 * <li><b>高基数标签（HighCardinality）</b> —— 取值种类多（图片尺寸、格式、风格），
 * 只进入 tracing span，不会成为指标维度。</li>
 * </ul>
 * 各标签的提取方法都声明为 {@code protected}，便于子类继承并按需覆盖某一项。
 * 命名遵循 OpenTelemetry 的 GenAI 语义约定。
 *
 * @since 1.0.0
 */
public class DefaultImageModelObservationConvention implements ImageModelObservationConvention {

	// 观测名称，遵循 OpenTelemetry GenAI 语义约定，作为 metrics 的指标名
	public static final String DEFAULT_NAME = "gen_ai.client.operation";

	// 预置常量：当请求未指定模型名时使用的占位标签值（KeyValue.NONE_VALUE 即 "none"）。
	// 提前构造为静态常量，既避免重复创建对象，也保证低基数标签始终存在，维度不缺失。
	private static final KeyValue REQUEST_MODEL_NONE = KeyValue
		.of(ImageModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL, KeyValue.NONE_VALUE);

	// 返回观测（指标）名称
	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	// 返回上下文名称，即 tracing span 的显示名
	@Override
	public String getContextualName(ImageModelObservationContext context) {
		ImageOptions options = context.getRequest().getOptions();
		// 有模型名时拼成 "image dall-e-3" 这样更易读的 span 名
		if (options != null && StringUtils.hasText(options.getModel())) {
			return "%s %s".formatted(context.getOperationMetadata().operationType(), options.getModel());
		}
		// 空值兜底：拿不到模型名时，只用操作类型作为 span 名
		return context.getOperationMetadata().operationType();
	}

	// 低基数标签：操作类型、服务商、请求模型，三者取值有限，可安全用作指标维度
	@Override
	public KeyValues getLowCardinalityKeyValues(ImageModelObservationContext context) {
		return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context));
	}

	// 提取「操作类型」标签，此处恒为 image
	protected KeyValue aiOperationType(ImageModelObservationContext context) {
		return KeyValue.of(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
				context.getOperationMetadata().operationType());
	}

	// 提取「服务商」标签，如 openai、stabilityai
	protected KeyValue aiProvider(ImageModelObservationContext context) {
		return KeyValue.of(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
				context.getOperationMetadata().provider());
	}

	// 提取「请求模型名」标签
	protected KeyValue requestModel(ImageModelObservationContext context) {
		ImageOptions options = context.getRequest().getOptions();
		// hasText 同时排除 null 与纯空白字符串
		if (options != null && StringUtils.hasText(options.getModel())) {
			return KeyValue.of(ImageModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
					options.getModel());
		}
		// 空值处理：未指定模型时返回 none 占位，保证该维度恒存在
		return REQUEST_MODEL_NONE;
	}

	// 高基数标签：取值组合较多，仅用于 tracing，不作为指标维度
	@Override
	public KeyValues getHighCardinalityKeyValues(ImageModelObservationContext context) {
		// KeyValues 是不可变对象，每个 and() 都返回新实例，因此需层层重新赋值
		var keyValues = KeyValues.empty();
		// Request
		// 依次追加：返回格式、图片尺寸、画面风格；缺失的项会被静默跳过
		keyValues = requestImageFormat(keyValues, context);
		keyValues = requestImageSize(keyValues, context);
		keyValues = requestImageStyle(keyValues, context);
		return keyValues;
	}

	// Request

	// 追加「返回格式」标签（url / b64_json）；未设置则原样返回，不产生该标签
	protected KeyValues requestImageFormat(KeyValues keyValues, ImageModelObservationContext context) {
		ImageOptions options = context.getRequest().getOptions();
		if (options != null && StringUtils.hasText(options.getResponseFormat())) {
			return keyValues.and(
					ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_RESPONSE_FORMAT.asString(),
					options.getResponseFormat());
		}
		// 空值处理：信息缺失时不添加标签，保持原 KeyValues 不变
		return keyValues;
	}

	// 追加「图片尺寸」标签，格式为 宽x高（如 1024x1024）
	protected KeyValues requestImageSize(KeyValues keyValues, ImageModelObservationContext context) {
		ImageOptions options = context.getRequest().getOptions();
		// 宽高必须同时存在才有意义，只有其一时不生成该标签
		if (options != null && options.getWidth() != null && options.getHeight() != null) {
			return keyValues.and(
					ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_SIZE.asString(),
					"%sx%s".formatted(options.getWidth(), options.getHeight()));
		}
		return keyValues;
	}

	// 追加「画面风格」标签（如 vivid / natural）；未设置则跳过
	protected KeyValues requestImageStyle(KeyValues keyValues, ImageModelObservationContext context) {
		ImageOptions options = context.getRequest().getOptions();
		if (options != null && StringUtils.hasText(options.getStyle())) {
			return keyValues.and(
					ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_STYLE.asString(),
					options.getStyle());
		}
		return keyValues;
	}

}
