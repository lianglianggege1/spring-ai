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
 * 【中文说明】GraalVM 原生镜像（AOT，Ahead-Of-Time）支持包。
 *
 * <p>
 * 原生镜像在编译期做闭世界静态分析，会裁剪掉未被静态引用的类与资源；而 Spring AI 大量依赖
 * 反射（Jackson 序列化、工具方法调用）与类路径资源（词表、维度表）。本包的作用就是把这些
 * “动态依赖”显式登记给 AOT 引擎，避免原生镜像运行时报 ClassNotFound / 资源缺失。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@code AiRuntimeHints}：通用扫描工具（找 Jackson 注解类、找内部类）。</li>
 * <li>{@code SpringAiCoreRuntimeHints}：登记核心消息/内容/工具类型的反射提示。</li>
 * <li>{@code ToolRuntimeHints}：登记工具调用链路中固定的框架类型。</li>
 * <li>{@code ToolBeanRegistrationAotProcessor}：动态发现带 {@code @Tool} 方法的 Bean 并按需登记。</li>
 * <li>{@code KnuddelsRuntimeHints}：登记 jtokkit 的 tiktoken 词表资源。</li>
 * </ul>
 *
 * <p>
 * 这些类均通过 {@code META-INF/spring/aot.factories} 由框架自动装配，业务代码通常无需直接引用。
 * 包上的 {@link org.jspecify.annotations.NullMarked @NullMarked} 表示默认非空语义。
 */
@NullMarked
package org.springframework.ai.aot;

import org.jspecify.annotations.NullMarked;
