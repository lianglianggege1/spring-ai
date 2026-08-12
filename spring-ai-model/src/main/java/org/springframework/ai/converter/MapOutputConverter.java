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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * {@link StructuredOutputConverter} implementation that uses a pre-configured
 * {@link JacksonJsonMessageConverter} to convert the LLM output into a
 * java.util.Map&lt;String, Object&gt; instance.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 */
/**
 * 【中文说明】Map 输出转换器：把模型返回的 JSON 文本转换为
 * {@code Map<String, Object>}。
 *
 * <p>
 * 适用于「结构不固定、无需定义 POJO」的场景——只想按 key 取值时用它最省事。若结构已知且固定，
 * 建议改用 {@code BeanOutputConverter} 以获得类型安全与 JSON Schema 约束。
 *
 * <p>
 * 实现要点：
 * <ul>
 * <li>继承 {@code AbstractMessageOutputConverter}，底层用 Jackson 消息转换器做反序列化</li>
 * <li>构造时禁用 {@code FAIL_ON_TRAILING_TOKENS}，容忍 JSON 后面残留的多余字符</li>
 * <li>{@link #convert(String)} 内置了简易的 <code>```json</code> 围栏剥离</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * MapOutputConverter converter = new MapOutputConverter();
 * Map<String, Object> map = converter.convert(chatModel.call("..." + converter.getFormat()));
 * }</pre>
 */
public class MapOutputConverter extends AbstractMessageOutputConverter<Map<String, Object>> {

	// 中文：无参构造。禁用 FAIL_ON_TRAILING_TOKENS 是关键容错措施——
	// 模型常在合法 JSON 之后多输出说明文字，开启该特性会直接抛异常，禁用后可正常解析前段 JSON
	public MapOutputConverter() {
		super(new JacksonJsonMessageConverter(
				JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)));
	}

	@Override
	public Map<String, Object> convert(String text) {
		// 中文：剥离 Markdown 代码围栏。substring(7, len-3) 中的 7 恰为 "```json" 的长度，
		// 3 为结尾 "```" 的长度；注意此处只认带 json 标识的围栏，更通用的清洗见 MarkdownCodeBlockCleaner
		if (text.startsWith("```json") && text.endsWith("```")) {
			text = text.substring(7, text.length() - 3);
		}

		// 中文：把纯文本包装成 Spring Messaging 的 Message，载荷为 UTF-8 字节，
		// 这是复用 MessageConverter 反序列化能力所必需的适配步骤
		Message<?> message = MessageBuilder.withPayload(text.getBytes(StandardCharsets.UTF_8)).build();
		// 中文：目标类型指定为 HashMap.class；此处使用裸类型 Map 接收后再转型，故会有未检查警告
		Map result = (Map) this.getMessageConverter().fromMessage(message, HashMap.class);
		// 中文：空值兜底，保证返回非 null 的 Map
		return result == null ? new HashMap<>() : result;
	}

	@Override
	public String getFormat() {
		// 中文：格式指令模板。四句话分别约束：输出 JSON、匹配指定 Java 类型、
		// 不要解释性文字（须符合 RFC8259）、不要包 ```json 围栏
		String raw = """
				Your response should be in JSON format.
				The data structure for the JSON should match this Java class: %s
				Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
				Remove the ```json markdown surrounding the output including the trailing "```".
				""";
		// 中文：把占位符 %s 替换为 java.util.HashMap 的全限定名，提示模型输出「键值对」结构
		return String.format(raw, HashMap.class.getName());
	}

}
