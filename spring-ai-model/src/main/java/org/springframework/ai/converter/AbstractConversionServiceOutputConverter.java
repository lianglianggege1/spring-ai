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

package org.springframework.ai.converter;

import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Abstract {@link StructuredOutputConverter} implementation that uses a pre-configured
 * {@link DefaultConversionService} to convert the LLM output into the desired type
 * format.
 *
 * @param <T> Specifies the desired response type.
 * @author Mark Pollack
 * @author Christian Tzolov
 */
/**
 * 【中文说明】基于 Spring {@link DefaultConversionService} 的结构化输出转换器抽象基类。
 *
 * <p>
 * 它复用 Spring 框架自带的类型转换体系（{@code ConversionService}）来完成「字符串 → 目标类型」的
 * 转换，适用于目标类型本身就被 Spring 内置转换器支持的简单场景，例如
 * {@code String -> List}（逗号分隔）、{@code String -> Integer} 等。
 *
 * <p>
 * 关键字段：{@code conversionService} —— 实际执行转换的服务实例，由子类通过构造器注入，
 * 子类在实现 {@code convert()} 时调用 {@link #getConversionService()} 取用。
 *
 * <p>
 * 本类只负责「持有并暴露转换服务」，并未实现 {@code convert()} 与 {@code getFormat()}，
 * 这两个方法留给子类（如 {@code ListOutputConverter}）按各自格式实现，属于典型的模板式复用。
 *
 * @param <T> 期望的响应类型
 */
public abstract class AbstractConversionServiceOutputConverter<T> implements StructuredOutputConverter<T> {

	// 中文：Spring 的类型转换服务，final 保证注入后不可替换
	private final DefaultConversionService conversionService;

	/**
	 * Create a new {@link AbstractConversionServiceOutputConverter} instance.
	 * @param conversionService the {@link DefaultConversionService} to use for converting
	 * the output.
	 */
	// 中文：构造器注入转换服务；子类可传入自定义实例以注册额外的转换规则
	public AbstractConversionServiceOutputConverter(DefaultConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return the ConversionService used by this converter.
	 * @return the ConversionService used by this converter.
	 */
	// 中文：供子类在 convert() 中获取转换服务
	public DefaultConversionService getConversionService() {
		return this.conversionService;
	}

}
