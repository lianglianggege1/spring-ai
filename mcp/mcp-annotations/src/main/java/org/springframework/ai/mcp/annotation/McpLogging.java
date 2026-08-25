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

package org.springframework.ai.mcp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that handle logging message notifications from MCP servers. This
 * annotation is applicable only for MCP clients.
 *
 * <p>
 * Methods annotated with this annotation can be used to consume logging messages from MCP
 * servers. The methods can have one of two signatures:
 * <ul>
 * <li>A single parameter of type {@code LoggingMessageNotification}
 * <li>Three parameters of types {@code LoggingLevel}, {@code String} (logger), and
 * {@code String} (data)
 * </ul>
 *
 * <p>
 * For synchronous consumers, the method must have a void return type. For asynchronous
 * consumers, the method can have either a void return type or return {@code Mono<Void>}.
 *
 * <p>
 * Example usage: <pre>{@code
 * &#64;McpLogging
 * public void handleLoggingMessage(LoggingMessageNotification notification) {
 *     // Handle the notification
 * }
 *
 *

&#64;McpLogging
 * public void handleLoggingMessageWithParams(LoggingLevel level, String logger, String data) {
 *     // Handle the logging message
 * }
 * }</pre>
 *
 * @author Christian Tzolov
 * @see io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification
 * @see io.modelcontextprotocol.spec.McpSchema.LoggingLevel
 */
/**
 * 用于处理来自MCP服务器的日志消息通知的方法注解。该注解仅适用于MCP客户端。
 *
 * <p>
 * 被该注解标记的方法可消费MCP服务器输出的日志消息。方法支持两种入参形式：
 * <ul>
 * <li>单个参数，类型为 {@code LoggingMessageNotification}
 * <li>三个参数，依次为 {@code LoggingLevel}、{@code String}（日志器名称）、{@code String}（日志数据）
 * </ul>
 *
 * <p>
 * 同步消费方法返回值必须为void；异步消费方法返回值可以是void，也可以返回 {@code Mono<Void>}。
 *
 * <p>
 * 使用示例：<pre>{@code
 * &#64;McpLogging
 * public void handleLoggingMessage(LoggingMessageNotification notification) {
 *     // 处理通知消息
 * }
 *
 * &#64;McpLogging
 * public void handleLoggingMessageWithParams(LoggingLevel level, String logger, String data) {
 *     // 处理日志消息
 * }
 * }</pre>
 *
 * @author Christian Tzolov
 * @see io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification
 * @see io.modelcontextprotocol.spec.McpSchema.LoggingLevel
 */

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpLogging {

	/**
	 * Used as connection or clients identifier to select the MCP clients, the logging
	 * consumer is associated with. At least one client identifier must be specified.
	 */
	/**
	 * 用作连接或客户端标识，用于筛选与该日志消费方法关联的MCP客户端。必须至少指定一个客户端标识。
	 */
	String[] clients();

}
