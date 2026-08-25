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
 * Annotates a method used for completion functionality in the MCP framework. This
 * annotation can be used in two mutually exclusive ways: 1. To complete an expression
 * within a URI template of a resource 2. To complete a prompt argument
 *
 * Note: You must use either the prompt or the uri attribute, but not both simultaneously.
 *
 * @author Christian Tzolov
 */
/**
 * 标注MCP框架中用于补全功能的方法。该注解有两种互斥使用方式：
 * 1. 对资源URI模板内的表达式进行补全
 * 2. 对提示词参数进行补全
 *
 * 注意：只能使用prompt或uri其中一个属性，不可同时使用。
 *
 * @author Christian Tzolov
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpComplete {

	/**
	 * The name reference to a prompt. This is used when the completion method is intended
	 * to complete a prompt argument.
	 */
	/**
	 * 提示词的名称引用。当补全方法用于补全提示词参数时使用该属性。
	 */
	String prompt() default "";

	/**
	 * The name reference to a resource template URI. This is used when the completion
	 * method is intended to complete an expression within a URI template of a resource.
	 */
	/**
	 * 资源模板URI的名称引用。当补全方法用于补全资源URI模板内的表达式时使用该属性。
	 */
	String uri() default "";

}
