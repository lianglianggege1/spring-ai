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

package org.springframework.ai.model;

/**
 * 【中文说明】SpringAIModelProperties 集中定义了 Spring AI 中<b>各模态模型的配置属性键名</b>常量。
 *
 * <p>
 * 用途：Spring Boot 自动配置通过 {@code @ConditionalOnProperty} 判断用户在
 * application.yml 中把某个模态指定给了哪个厂商，从而决定装配哪套实现。例如配置
 * {@code spring.ai.model.chat=openai}，则只有 OpenAI 的 ChatModel 会被创建。
 * 属性值一般取自 {@link SpringAIModels} 中的厂商常量。
 *
 * <p>
 * 关键字段：{@link #MODEL_PREFIX} 是统一前缀 {@code spring.ai.model}，
 * 其余常量都基于它拼接而成，这样若前缀调整只需改一处，避免了硬编码散落各处。
 *
 * <p>
 * 覆盖的模态：聊天、嵌入（含文本/多模态子类）、图像、语音转写、语音合成、内容审核。
 *
 * <p>
 * 典型用法：{@code @ConditionalOnProperty(name = SpringAIModelProperties.CHAT_MODEL,
 * havingValue = SpringAIModels.OPENAI)}
 */
public final class SpringAIModelProperties {

	// 【中文】私有构造器，禁止实例化（纯常量持有类）。
	private SpringAIModelProperties() {
		// Avoids instantiation
	}

	// 【中文】所有模型相关配置的统一前缀。下面各常量均基于此拼接，保证命名一致性。
	public static final String MODEL_PREFIX = "spring.ai.model";

	// 【中文】聊天模型选择项：spring.ai.model.chat
	public static final String CHAT_MODEL = MODEL_PREFIX + ".chat";

	// 【中文】嵌入模型选择项：spring.ai.model.embedding（通用嵌入）
	public static final String EMBEDDING_MODEL = MODEL_PREFIX + ".embedding";

	// 【中文】文本嵌入模型：spring.ai.model.embedding.text。
	// 注意它是 embedding 的下级键，用于在同时支持多种嵌入类型时做更细粒度的区分。
	public static final String TEXT_EMBEDDING_MODEL = MODEL_PREFIX + ".embedding.text";

	// 【中文】多模态嵌入模型：spring.ai.model.embedding.multimodal（如同时支持图文向量化）
	public static final String MULTI_MODAL_EMBEDDING_MODEL = MODEL_PREFIX + ".embedding.multimodal";

	// 【中文】图像生成模型：spring.ai.model.image
	public static final String IMAGE_MODEL = MODEL_PREFIX + ".image";

	// 【中文】语音转文字（ASR/转写）模型：spring.ai.model.audio.transcription
	public static final String AUDIO_TRANSCRIPTION_MODEL = MODEL_PREFIX + ".audio.transcription";

	// 【中文】文字转语音（TTS）模型：spring.ai.model.audio.speech
	public static final String AUDIO_SPEECH_MODEL = MODEL_PREFIX + ".audio.speech";

	// 【中文】内容审核（Moderation）模型：spring.ai.model.moderation
	public static final String MODERATION_MODEL = MODEL_PREFIX + ".moderation";

}
