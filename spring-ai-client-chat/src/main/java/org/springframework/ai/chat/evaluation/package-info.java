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

/**
 * The org.sf.ai.chat package represents the bounded context for the Chat Model within the
 * AI generative model domain. This package extends the core domain defined in
 * org.sf.ai.generative, providing implementations specific to chat-based generative AI
 * interactions.
 * <p>
 * In line with Domain-Driven Design principles, this package includes implementations of
 * entities and value objects specific to the chat context, such as ChatPrompt and
 * ChatResponse, adhering to the ubiquitous language of chat interactions in AI models.
 * <p>
 * This bounded context is designed to encapsulate all aspects of chat-based AI
 * functionalities, maintaining a clear boundary from other contexts within the AI domain.
 */
/**
 * org.sf.ai.chat 包代表生成式AI模型领域中，对话模型的限界上下文。
 * 该包扩展了 org.sf.ai.generative 中定义的核心领域，提供面向对话式生成AI交互的专属实现。
 * <p>
 * 遵循领域驱动设计(DDD)原则，本包包含对话上下文专属的实体与值对象实现，
 * 例如 ChatPrompt、ChatResponse，贴合AI模型对话交互的统一领域语言。
 * <p>
 * 该限界上下文用于封装全部对话相关AI能力，与AI领域内其他上下文保持清晰边界。
 */
@NullMarked
package org.springframework.ai.chat.evaluation;

import org.jspecify.annotations.NullMarked;
