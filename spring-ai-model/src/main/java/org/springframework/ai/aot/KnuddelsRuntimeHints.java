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

package org.springframework.ai.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;

/**
 * 【中文说明】为 jtokkit（Knuddels 出品的 tiktoken Java 实现）登记原生镜像资源提示。
 *
 * <p>
 * 用途：实现 {@link RuntimeHintsRegistrar}，解决一个具体问题——jtokkit 在运行时需要从 classpath 读取
 * BPE 词表文件 {@code cl100k_base.tiktoken}（用于 token 计数）。GraalVM 原生镜像默认不会把资源文件
 * 打包进去，若不显式登记，运行期会因找不到该资源而报错。
 *
 * <p>
 * 关键点：
 * <ul>
 * <li>本类只登记「资源」，不涉及反射；仅一行 {@code registerResource} 即完成。</li>
 * <li>它需要在 {@code META-INF/spring/aot.factories} 中注册后才会被 Spring AOT 引擎回调，
 * 属于 SPI 式的扩展点，业务代码不会直接 new 它。</li>
 * </ul>
 */
public class KnuddelsRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	// AOT 处理阶段由 Spring 框架回调；classLoader 可为 null，本实现未使用它
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		// 登记 jtokkit 的 cl100k_base 词表文件，确保它被包含进原生镜像并可在运行时读取
		hints.resources().registerResource(new ClassPathResource("/com/knuddels/jtokkit/cl100k_base.tiktoken"));
	}

}
