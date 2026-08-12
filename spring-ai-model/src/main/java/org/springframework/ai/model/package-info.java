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
 * Provides a set of interfaces and classes for a generic API designed to interact with
 * various AI models. This package includes interfaces for handling AI model calls,
 * requests, responses, results, and associated metadata. It is designed to offer a
 * flexible and adaptable framework for interacting with different types of AI models,
 * abstracting the complexities involved in model invocation and result processing. The
 * use of generics enhances the API's capability to work with a wide range of models,
 * ensuring a broad applicability across diverse AI scenarios.
 *
 */

/**
 * 【中文说明】本包（org.springframework.ai.model）是 Spring AI 的<b>模型抽象核心包</b>。
 *
 * <p>
 * 它定义了与具体厂商无关的通用 API 骨架，主要包含四条主线：
 * <ul>
 * <li><b>调用入口</b>：{@code Model}（同步）、{@code StreamingModel}（流式）；</li>
 * <li><b>请求/响应模型</b>：{@code ModelRequest}、{@code ModelResponse}、{@code ModelResult}，
 * 构成"请求 -> 响应 -> 单条结果"的三层结构；</li>
 * <li><b>元数据与选项</b>：{@code ModelOptions}、{@code ResultMetadata}、
 * {@code ResponseMetadata} 及其抽象/可变实现；</li>
 * <li><b>辅助设施</b>：{@code ApiKey} 凭据抽象、{@code EmbeddingUtils} 向量转换工具、
 * {@code SpringAIModels} / {@code SpringAIModelProperties} 自动配置常量。</li>
 * </ul>
 *
 * <p>
 * 设计上大量使用泛型，使同一套契约可适配聊天、嵌入、图像、语音等各种模态。
 *
 * <p>
 * 关于 {@code @NullMarked}：这是 JSpecify 的包级注解，表示<b>本包内所有类型默认非空</b>，
 * 只有显式标注 {@code @Nullable} 的地方才允许为 null。它能让 IDE 和静态分析工具
 * 提前发现潜在的空指针问题，阅读源码时可据此判断哪些返回值需要判空。
 */

@NullMarked
package org.springframework.ai.model;

import org.jspecify.annotations.NullMarked;
