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
 * 【中文说明】SpringAIModels 集中定义了 Spring AI 支持的<b>各模型厂商标识符</b>常量。
 *
 * <p>
 * 用途：与 {@link SpringAIModelProperties} 配对使用——后者提供属性"键"，本类提供属性"值"。
 * 用户在 application.yml 中写 {@code spring.ai.model.chat=openai}，其中 "openai" 即对应
 * 本类的 {@link #OPENAI} 常量；自动配置类则用同一常量做条件匹配，从而避免两边字符串写错。
 *
 * <p>
 * 关键特点：全部为字符串常量，命名统一采用小写加连字符（如 azure-openai），
 * 与配置文件中的书写形式保持一致。
 *
 * <p>
 * 典型用法：{@code @ConditionalOnProperty(name = SpringAIModelProperties.CHAT_MODEL,
 * havingValue = SpringAIModels.OLLAMA, matchIfMissing = true)}
 */
public final class SpringAIModels {

	// 【中文】私有构造器，禁止实例化（纯常量持有类）。
	private SpringAIModels() {
		// Avoids instantiation
	}

	// 【中文】以下均为厂商/接入方式标识常量，取值即配置文件中要填写的字符串。
	// Anthropic（Claude 系列）
	public static final String ANTHROPIC = "anthropic";

	// 【中文】微软 Azure 托管的 OpenAI 服务（与直连 OpenAI 的鉴权和端点不同）
	public static final String AZURE_OPENAI = "azure-openai";

	// 【中文】AWS Bedrock 上的 Cohere 模型
	public static final String BEDROCK_COHERE = "bedrock-cohere";

	// 【中文】AWS Bedrock 的 Converse 统一对话 API（推荐的 Bedrock 接入方式）
	public static final String BEDROCK_CONVERSE = "bedrock-converse";

	// 【中文】AWS Bedrock 上的 Amazon Titan 模型
	public static final String BEDROCK_TITAN = "bedrock-titan";

	// 【中文】Mistral AI
	public static final String MISTRAL = "mistral";

	// 【中文】Oracle 云的生成式 AI 服务
	public static final String OCI_GENAI = "oci-genai";

	// 【中文】Ollama，常用于本地部署开源模型（通常无需 API key）
	public static final String OLLAMA = "ollama";

	// 【中文】OpenAI 官方接口（Spring AI 自研 HTTP 客户端实现）
	public static final String OPENAI = "openai";

	// 【中文】基于 OpenAI 官方 Java SDK 的接入方式。
	// 注意与上面的 OPENAI 区分：二者面向同一服务但底层客户端实现不同，故需要两个标识。
	public static final String OPENAI_SDK = "openai-sdk";

	// 【中文】PostgresML：在 PostgreSQL 数据库内部执行推理/嵌入
	public static final String POSTGRESML = "postgresml";

	// 【中文】Stability AI（图像生成）
	public static final String STABILITY_AI = "stabilityai";

	// 【中文】本地 Transformers 运行时（ONNX 等，进程内推理，无需远程服务）
	public static final String TRANSFORMERS = "transformers";

	// 【中文】Google Vertex AI 平台
	public static final String VERTEX_AI = "vertexai";

	// 【中文】Google Gen AI（Gemini）统一 SDK 接入
	public static final String GOOGLE_GEN_AI = "google-genai";

	// 【中文】DeepSeek
	public static final String DEEPSEEK = "deepseek";

	// 【中文】ElevenLabs（语音合成 TTS）
	public static final String ELEVEN_LABS = "elevenlabs";

}
