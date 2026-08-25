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

package org.springframework.ai.template;

/**
 * Validation modes for template renderers.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public enum ValidationMode {

	/**
	 * If the validation fails, an exception is thrown. This is the default mode.
	 */
	/**
	 * 若校验失败，则抛出异常。此为默认模式。
	 */
	THROW,

	/**
	 * If the validation fails, a warning is logged. The template is rendered with the
	 * missing placeholders/variables. This mode is not recommended for production use.
	 */
	/**
	 * 若校验失败，则记录警告日志。模板仍会使用缺失的占位符/变量完成渲染。该模式不建议在生产环境使用。
	 */
	WARN,

	/**
	 * No validation is performed.
	 */
	/**
	 * 不执行任何校验。
	 */
	NONE

}
