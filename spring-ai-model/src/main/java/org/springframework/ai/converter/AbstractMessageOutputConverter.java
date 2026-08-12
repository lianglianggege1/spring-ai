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

import org.springframework.messaging.converter.MessageConverter;

/**
 * Abstract {@link StructuredOutputConverter} implementation that uses a pre-configured
 * {@link MessageConverter} to convert the LLM output into the desired type format.
 *
 * @param <T> Specifies the desired response type.
 * @author Mark Pollack
 * @author Christian Tzolov
 */
/**
 * 【中文说明】基于 Spring Messaging {@link MessageConverter} 的结构化输出转换器抽象基类。
 *
 * <p>
 * 与 {@code AbstractConversionServiceOutputConverter} 走 ConversionService 不同，
 * 本类复用 Spring Messaging 的消息转换体系：先把模型输出的文本包装成一条
 * {@code Message}（载荷为 UTF-8 字节），再交给 {@link MessageConverter}（通常是 Jackson JSON
 * 实现）反序列化为目标类型。适合 JSON 这类需要完整反序列化能力的场景。
 *
 * <p>
 * 关键字段：{@code messageConverter} —— 实际执行反序列化的消息转换器，由子类构造时确定。
 *
 * <p>
 * 典型子类：{@code MapOutputConverter}（转成 {@code Map<String, Object>}）。
 *
 * @param <T> 期望的响应类型
 */
public abstract class AbstractMessageOutputConverter<T> implements StructuredOutputConverter<T> {

	// 中文：消息转换器（一般为 JacksonJsonMessageConverter），负责字节载荷到对象的反序列化。
	// 注意此字段未声明 final，但框架内并未提供 setter，实际使用中视为只读
	private MessageConverter messageConverter;

	/**
	 * Create a new AbstractMessageOutputConverter.
	 * @param messageConverter the message converter to use
	 */
	// 中文：构造器注入消息转换器，子类通常在自己的无参构造中构造并传入一个配置好的实例
	public AbstractMessageOutputConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the message converter used by this output converter.
	 * @return the message converter
	 */
	// 中文：供子类在 convert() 中获取消息转换器
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

}
