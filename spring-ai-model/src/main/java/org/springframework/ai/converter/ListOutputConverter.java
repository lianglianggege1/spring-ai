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

import java.util.Collections;
import java.util.List;

import org.springframework.core.convert.support.DefaultConversionService;

/**
 * {@link StructuredOutputConverter} implementation that uses a
 * {@link DefaultConversionService} to convert the LLM output into a
 * {@link java.util.List} instance.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 */
/**
 * 【中文说明】列表输出转换器：把模型返回的「逗号分隔文本」转换为 {@code List<String>}。
 *
 * <p>
 * 这是最轻量的一种结构化输出方式，不涉及 JSON，靠 Spring 内置的
 * {@code String -> Collection} 转换规则按逗号切分。
 *
 * <p>
 * 工作流程：
 * <ol>
 * <li>{@link #getFormat()} 生成指令，要求模型「只输出逗号分隔值，不要任何前后缀文字」</li>
 * <li>模型返回如 {@code foo, bar, baz}</li>
 * <li>{@link #convert(String)} 交由 ConversionService 切分为 {@code [foo, bar, baz]}</li>
 * </ol>
 *
 * <p>
 * 典型用法：{@code chatClient.prompt().user("列出5种水果").call().entity(new ListOutputConverter());}
 *
 * <p>
 * 局限：结果元素固定为 String，且值本身不能含逗号，复杂结构请改用 {@code BeanOutputConverter}。
 */
public class ListOutputConverter extends AbstractConversionServiceOutputConverter<List<String>> {

	// 中文：无参构造——使用 Spring 默认转换服务，其中已注册 String->Collection 的逗号切分规则
	public ListOutputConverter() {
		this(new DefaultConversionService());
	}

	// 中文：允许外部传入自定义转换服务（例如改用其它分隔符规则）
	public ListOutputConverter(DefaultConversionService defaultConversionService) {
		super(defaultConversionService);
	}

	@Override
	// 中文：返回追加到 Prompt 的格式指令。明确「不要前后缀文字」并给出示例，
	// 是为了避免模型输出「好的，以下是列表：foo, bar」这类无法解析的内容
	public String getFormat() {
		return """
				Respond with only a list of comma-separated values, without any leading or trailing text.
				Example format: foo, bar, baz
				""";
	}

	@Override
	// 中文：解析入口。委托 ConversionService 完成切分；
	// 转换结果可能为 null，此处兜底返回空列表，保证调用方永远拿不到 null
	public List<String> convert(String text) {
		List<String> result = this.getConversionService().convert(text, List.class);
		return result == null ? Collections.emptyList() : result;
	}

}
