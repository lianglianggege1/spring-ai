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

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Content;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;

/**
 * 【中文说明】Spring AI 核心类型的原生镜像（GraalVM Native Image）运行时提示登记器。
 *
 * <p>
 * 用途：实现 {@link RuntimeHintsRegistrar}，把 spring-ai-model 模块中会被反射访问的核心类型
 * （各类 Message、Content、ToolCallback 等）与必需的资源文件登记到 {@link RuntimeHints}，
 * 保证应用编译为原生镜像后，JSON 序列化与工具调用等依赖反射的功能仍然可用。
 *
 * <p>
 * 登记内容分两部分：
 * <ol>
 * <li><b>反射提示</b>：{@code chatTypes} 中列出的核心类型及其全部内部类。</li>
 * <li><b>资源提示</b>：{@code embedding/embedding-model-dimensions.properties}（嵌入模型维度表）。</li>
 * </ol>
 *
 * <p>
 * 关键点：该类通过 {@code META-INF/spring/aot.factories} 被 Spring AOT 引擎发现并回调，
 * 不需要业务代码显式调用。
 */
public class SpringAiCoreRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	// AOT 构建期回调入口：向 hints 中登记反射与资源提示
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		// 需要开放反射的核心类型清单：消息体系、内容体系、工具调用相关类型，
		// 它们会被 Jackson 序列化或被框架动态实例化
		var chatTypes = Set.of(AbstractMessage.class, AssistantMessage.class, ToolResponseMessage.class, Message.class,
				ToolCallback.class, ToolDefinition.class, AssistantMessage.ToolCall.class, MessageType.class,
				UserMessage.class, SystemMessage.class, Content.class, MediaContent.class);

		// MemberCategory.values() 表示开放全部成员类别（构造器、字段、方法等），
		// 这是最宽松的策略：以镜像体积换取运行时的确定性
		var memberCategories = MemberCategory.values();

		for (var c : chatTypes) {
			// 登记类型本身
			hints.reflection().registerType(c, memberCategories);
			// 内部类（如 Builder、嵌套 record）同样可能被反射访问，需一并登记
			var innerClassesFor = AiRuntimeHints.findInnerClassesFor(c);
			for (var cc : innerClassesFor) {
				hints.reflection().registerType(cc, memberCategories);
			}
		}

		// 资源提示：目前只有一个文件，用 Set + for 循环写法便于将来扩充
		for (var r : Set.of("embedding/embedding-model-dimensions.properties")) {
			hints.resources().registerResource(new ClassPathResource(r));
		}

	}

}
