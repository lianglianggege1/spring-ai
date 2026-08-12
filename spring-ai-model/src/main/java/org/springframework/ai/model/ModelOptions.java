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
 * Interface representing the customizable options for AI model interactions. This marker
 * interface allows for the specification of various settings and parameters that can
 * influence the behavior and output of AI models. It is designed to provide flexibility
 * and adaptability in different AI scenarios, ensuring that the AI models can be
 * fine-tuned according to specific requirements.
 *
 * @author Mark Pollack
 * @since 0.8.0
 */
/**
 * 【中文说明】ModelOptions 是"模型可调参数"的<b>标记接口</b>（marker interface，无任何方法声明）。
 *
 * <p>
 * 为什么是空接口：不同厂商模型的参数差异极大（OpenAI 有 frequencyPenalty，Anthropic 有 topK，
 * 图像模型有 width/height……），无法抽象出通用方法。因此这里只用一个空接口做<b>类型标记</b>，
 * 让框架能够在泛型签名（如 {@link ModelRequest#getOptions()}）中统一引用"某种模型选项"，
 * 而把具体参数的定义权完全下放给各实现类。
 *
 * <p>
 * 常见子接口/实现：ChatOptions、EmbeddingOptions、ImageOptions，以及各厂商的具体实现类
 * （如 OpenAiChatOptions）。它们通常都提供 Builder 以便链式构造，并支持 copy() 做防御性拷贝。
 *
 * <p>
 * 典型用法：请求级选项（放在 Prompt 里）会与客户端默认选项做<b>合并</b>，一般是请求级覆盖默认级。
 *
 * @author Mark Pollack
 * @since 0.8.0
 */
public interface ModelOptions {

}
