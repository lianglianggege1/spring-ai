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

import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers runtime hints for the tool calling APIs.
 *
 * @author Thomas Vitale
 */
/**
 * 【中文说明】工具调用（Tool Calling）API 的原生镜像运行时提示登记器。
 *
 * <p>
 * 用途：实现 {@link RuntimeHintsRegistrar}，为工具调用链路中会被反射实例化的固定类型登记提示。
 * 当前只登记 {@link DefaultToolCallResultConverter}——它是把工具方法返回值转换为字符串结果的默认转换器，
 * 框架通过反射按类名创建其实例，因此在原生镜像中必须显式保留。
 *
 * <p>
 * 与 {@code ToolBeanRegistrationAotProcessor} 的分工：
 * <ul>
 * <li>本类负责「静态已知」的框架内部类型。</li>
 * <li>后者负责「动态发现」用户自定义的 {@code @Tool} Bean。</li>
 * </ul>
 *
 * @author Thomas Vitale
 */
public class ToolRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	// AOT 构建期回调；classLoader 参数允许为 null，此实现未使用
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		// 全量成员类别：转换器需要被反射实例化并调用方法，故一次性全部开放
		var mcs = MemberCategory.values();
		hints.reflection().registerType(DefaultToolCallResultConverter.class, mcs);
	}

}
