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

package org.springframework.ai.support;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具回调（ToolCallback）的静态工厂工具类。
 *
 * <p>
 * 用途：把普通 Java 对象中标注了 {@code @Tool} 的方法，一键转换成模型可调用的
 * {@link ToolCallback} 数组，是 Function Calling / 工具调用场景最常用的入口。
 *
 * <p>
 * 设计要点：
 * <ul>
 * <li>{@code final class} + 私有构造器：典型的工具类写法，禁止继承与实例化。</li>
 * <li>内部委托给 {@link MethodToolCallbackProvider} 的 Builder 完成真正的方法扫描与解析，
 * 本类只是一层语法糖。</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ToolCallback[] callbacks = ToolCallbacks.from(new WeatherTools(), new TimeTools());
 * chatClient.prompt().toolCallbacks(callbacks).call();
 * }</pre>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class ToolCallbacks {

	// 私有构造器：工具类不允许被实例化
	private ToolCallbacks() {
	}

	// 可变参数 sources：传入若干个“工具对象”，扫描其中带 @Tool 注解的方法并生成回调数组
	public static ToolCallback[] from(Object... sources) {
		// 建造者链：注册工具对象 -> 构建 Provider -> 取出解析好的 ToolCallback 数组
		return MethodToolCallbackProvider.builder().toolObjects(sources).build().getToolCallbacks();
	}

}
