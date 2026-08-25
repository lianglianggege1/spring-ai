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
 * Annotation for methods that handle progress notifications from MCP servers. This
 * annotation is applicable only for MCP clients.
 *
 * <p>
 * Methods annotated with this annotation can be used to consume progress messages from
 * MCP servers. The methods takes a single parameter of type {@code ProgressNotification}
 *
 *
 * <p>
 * Example usage: <pre>{@code
 * &#64;McpProgress(clientId = "my-client-id")
 * public void handleProgressMessage(ProgressNotification notification) {
 *     // Handle the progress notification
 * }
 * }</pre>
 *
 * @author Christian Tzolov
 * @see io.modelcontextprotocol.spec.McpSchema.ProgressNotification
 */
/**
 * 用于处理来自MCP服务器的进度通知的方法注解。该注解仅适用于MCP客户端。
 *
 * <p>
 * 被该注解标记的方法可消费MCP服务器下发的进度消息。方法接收单个参数，类型为 {@code ProgressNotification}。
 *
 * <p>
 * 使用示例：<pre>{@code
 * &#64;McpProgress(clientId = "my-client-id")
 * public void handleProgressMessage(ProgressNotification notification) {
 *     // 处理进度通知
 * }
 * }</pre>
 *
 * @author Christian Tzolov
 * @see io.modelcontextprotocol.spec.McpSchema.ProgressNotification
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpProgress {

	/**
	 * Used as connection or client identifier to select the MCP client, the progress
	 * consumer is associated with. At least one client identifier must be specified.
	 */
	/**
	 * 用作连接或客户端标识，用于筛选与该进度消费方法关联的MCP客户端。必须至少指定一个客户端标识。
	 */
	String[] clients();

}
