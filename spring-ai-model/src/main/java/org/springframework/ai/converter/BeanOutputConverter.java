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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.ParameterizedTypeReference;

/**
 * An implementation of {@link StructuredOutputConverter} that transforms the LLM output
 * to a specific object type using JSON schema. This converter works by generating a JSON
 * schema based on a given Java class or parameterized type reference, which is then used
 * to validate and transform the LLM output into the desired type.
 *
 * @param <T> The target type to which the output will be converted.
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Sebastian Ullrich
 * @author Kirk Lund
 * @author Josh Long
 * @author Sebastien Deleuze
 * @author Soby Chacko
 * @author Thomas Vitale
 * @author liugddx
 */
/**
 * 【中文说明】Bean 输出转换器：借助 JSON Schema，把模型输出转换为任意指定的 Java 类型。
 *
 * <p>
 * 这是 Spring AI 结构化输出体系中<b>最常用、功能最完整</b>的实现，也是 ChatClient
 * {@code .entity(XxxDto.class)} 背后的默认转换器。
 *
 * <p>
 * 核心思路分三步：
 * <ol>
 * <li><b>生成 Schema</b>：构造时依据目标类型反射生成 JSON Schema，缓存在 {@code jsonSchema} 字段</li>
 * <li><b>约束模型</b>：{@link #getFormat()} 把该 Schema 嵌入提示词，要求模型严格照此输出</li>
 * <li><b>清洗并解析</b>：{@link #convert(String)} 先用清洗链去掉思维链标签、Markdown
 * 围栏和多余空白，再用 Jackson 反序列化为目标对象</li>
 * </ol>
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code type} —— 目标类型（支持泛型，如 {@code List<ActorFilms>}）</li>
 * <li>{@code jsonMapper} —— Jackson 映射器，默认关闭「未知属性报错」</li>
 * <li>{@code jsonSchema} —— 构造时一次性生成并缓存，避免每次调用重复反射</li>
 * <li>{@code textCleaner} —— 解析前的文本清洗链，可自定义</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * // 普通类型
 * var conv = new BeanOutputConverter<>(ActorFilms.class);
 * // 泛型类型必须用 ParameterizedTypeReference，否则泛型信息会被擦除
 * var listConv = new BeanOutputConverter<>(new ParameterizedTypeReference<List<ActorFilms>>() {
 * });
 * }</pre>
 *
 * @param <T> 目标转换类型
 */
public class BeanOutputConverter<T> implements StructuredOutputConverter<T> {

	// 中文：日志器，仅用于 Schema 解析失败等异常场景的记录
	private final Log logger = LogFactory.getLog(BeanOutputConverter.class);

	/**
	 * The target class type reference to which the output will be converted.
	 */
	// 中文：目标类型。用 java.lang.reflect.Type 而非 Class，是为了保留泛型参数信息
	private final Type type;

	/** The JSON mapper used for deserialization and other JSON operations. */
	// 中文：Jackson 映射器，负责反序列化及 Schema 的 Map 化
	private final JsonMapper jsonMapper;

	/** Holds the generated JSON schema for the target type. */
	// 中文：目标类型对应的 JSON Schema，构造时生成并缓存（不可变），供 getFormat()/getJsonSchema() 复用
	private final String jsonSchema;

	/** The text cleaner used to preprocess LLM responses before parsing. */
	// 中文：解析前的文本清洗链；未显式传入时使用 createDefaultTextCleaner() 的默认组合
	private final ResponseTextCleaner textCleaner;

	/**
	 * Constructor to initialize with the target type's class.
	 * @param clazz The target type's class.
	 */
	// 中文：最常用的构造器——只传目标类。JSON 映射器与文本清洗器均走默认值。
	// 本类采用「重叠构造器（telescoping constructor）」模式，所有公开构造器最终都收敛到下面那个私有构造器
	public BeanOutputConverter(Class<T> clazz) {
		this(clazz, null, null);
	}

	/**
	 * Constructor to initialize with the target type's class, a custom JSON mapper, and a
	 * line endings normalizer to ensure consistent line endings on any platform.
	 * @param clazz The target type's class.
	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
	 */
	// 中文：目标类 + 自定义 Jackson 映射器（如需注册特殊模块或时间格式时使用）
	public BeanOutputConverter(Class<T> clazz, @Nullable JsonMapper jsonMapper) {
		this(clazz, jsonMapper, null);
	}

	/**
	 * Constructor to initialize with the target type's class, a custom JSON mapper, and a
	 * custom text cleaner.
	 * @param clazz The target type's class.
	 * @param jsonMapper Custom JSON mapper for JSON operations.
	 * @param textCleaner Custom text cleaner for preprocessing responses.
	 */
	// 中文：目标类 + 自定义映射器 + 自定义清洗器。
	// 内部先把 Class 包装成 ParameterizedTypeReference，从而与泛型入口统一到同一条链路
	public BeanOutputConverter(Class<T> clazz, @Nullable JsonMapper jsonMapper,
			@Nullable ResponseTextCleaner textCleaner) {
		this(ParameterizedTypeReference.forType(clazz), jsonMapper, textCleaner);
	}

	/**
	 * Constructor to initialize with the target class type reference.
	 * @param typeRef The target class type reference.
	 */
	// 中文：泛型场景入口。形如 List<Foo>、Map<String, Foo> 的目标类型必须走这里，
	// 通过匿名子类捕获泛型实参，规避 Java 的类型擦除
	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef) {
		this(typeRef, null, null);
	}

	/**
	 * Constructor to initialize with the target class type reference, a custom JSON
	 * mapper, and a line endings normalizer to ensure consistent line endings on any
	 * platform.
	 * @param typeRef The target class type reference.
	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
	 */
	// 中文：泛型类型 + 自定义 Jackson 映射器
	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef, @Nullable JsonMapper jsonMapper) {
		this(typeRef, jsonMapper, null);
	}

	/**
	 * Constructor to initialize with the target class type reference, a custom JSON
	 * mapper, and a custom text cleaner.
	 * @param typeRef The target class type reference.
	 * @param jsonMapper Custom JSON mapper for JSON operations.
	 * @param textCleaner Custom text cleaner for preprocessing responses.
	 */
	// 中文：泛型类型 + 自定义映射器 + 自定义清洗器。取出底层 Type 后交给私有构造器统一处理
	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef, @Nullable JsonMapper jsonMapper,
			@Nullable ResponseTextCleaner textCleaner) {
		this(typeRef.getType(), jsonMapper, textCleaner);
	}

	/**
	 * Constructor to initialize with the target class type reference, a custom JSON
	 * mapper, and a line endings normalizer to ensure consistent line endings on any
	 * platform.
	 * @param type The target class type.
	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
	 * @param textCleaner Custom text cleaner for preprocessing responses.
	 */
	// 中文：唯一的「真正干活」的私有构造器，所有公开构造器最终都汇聚到此处，
	// 保证初始化逻辑只有一份，不会出现遗漏字段的情况
	private BeanOutputConverter(Type type, @Nullable JsonMapper jsonMapper, @Nullable ResponseTextCleaner textCleaner) {
		// 中文：参数校验——目标类型是一切的基础，不允许为 null，快速失败
		Objects.requireNonNull(type, "Type cannot be null;");
		this.type = type;
		// 中文：可选参数的空值处理——传了就用传入的，没传则回退到默认实现
		this.jsonMapper = jsonMapper != null ? jsonMapper : getJsonMapper();
		this.textCleaner = textCleaner != null ? textCleaner : createDefaultTextCleaner();
		// 中文：在构造阶段一次性生成 Schema 并缓存。注意 generateSchema() 是 protected 可覆写方法，
		// 这里在构造器中调用它属于「构造期调用可覆写方法」，子类覆写时不可依赖自身尚未初始化的字段
		this.jsonSchema = Objects.requireNonNull(generateSchema(), "JSON schema cannot be null");
	}

	/**
	 * Creates the default text cleaner that handles common response formats from various
	 * AI models.
	 * <p>
	 * The default cleaner includes:
	 * <ul>
	 * <li>{@link ThinkingTagCleaner} - Removes thinking tags from models like Amazon Nova
	 * and Qwen. For models that don't generate thinking tags, this has minimal
	 * performance impact due to fast-path optimization.</li>
	 * <li>{@link MarkdownCodeBlockCleaner} - Removes markdown code block formatting.</li>
	 * <li>{@link WhitespaceCleaner} - Trims whitespace.</li>
	 * </ul>
	 * <p>
	 * To customize the cleaning behavior, provide a custom {@link ResponseTextCleaner}
	 * via the constructor.
	 * @return a composite text cleaner with default cleaning strategies
	 */
	// 中文：构建默认清洗链，顺序经过精心设计，不可随意调换：
	// ①先 trim，让后续的标签/围栏匹配不受首尾空白干扰
	// ②去思维链标签（<think> 等），推理模型必需
	// ③剥 Markdown 代码围栏 ```json
	// ④再次 trim，清除前几步留下的换行与空格，确保交给 Jackson 的是纯净 JSON
	private static ResponseTextCleaner createDefaultTextCleaner() {
		return CompositeResponseTextCleaner.builder()
			.addCleaner(new WhitespaceCleaner())
			.addCleaner(new ThinkingTagCleaner())
			.addCleaner(new MarkdownCodeBlockCleaner())
			.addCleaner(new WhitespaceCleaner()) // Final trim after all cleanups
			.build();
	}

	/**
	 * Generates the JSON schema for the target type.
	 * <p>
	 * This method can be overridden in subclasses to customize the JSON schema generation
	 * logic.
	 * @return the generated JSON schema
	 */
	// 中文：根据目标类型反射生成 JSON Schema。声明为 protected 是预留的扩展点，
	// 子类可覆写以定制 Schema（如增加字段描述、必填约束等）
	protected String generateSchema() {
		return JsonSchemaGenerator.generateForType(this.type);
	}

	/**
	 * Parses the given text to transform it to the desired target type.
	 * @param text The LLM output in string format.
	 * @return The parsed output in the desired target type.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T convert(String text) {
		// Clean the text using the configured text cleaner
		// 中文：第一步——先过清洗链，剔除思维链标签、Markdown 围栏与多余空白
		text = this.textCleaner.clean(text);

		// 中文：第二步——用 constructType 把 java.lang.reflect.Type 转成 Jackson 的 JavaType，
		// 这样才能正确还原 List<Foo> 这类泛型；强转 (T) 由 @SuppressWarnings("unchecked") 兜底，
		// 因为泛型信息在运行期无法被编译器校验
		return (T) this.jsonMapper.readValue(text, this.jsonMapper.constructType(this.type));
	}

	/**
	 * Configures and returns a JSON mapper for JSON operations.
	 * @return Configured JSON mapper.
	 */
	// 中文：构造默认 Jackson 映射器。两处关键配置：
	// ①自动装载类路径上可用的 Jackson 模块（如 JavaTimeModule），以支持 LocalDate 等类型
	// ②关闭 FAIL_ON_UNKNOWN_PROPERTIES——模型常多输出目标类没有的字段，
	//   若不关闭会直接抛异常，关闭后可容错地忽略这些多余字段
	protected JsonMapper getJsonMapper() {
		return JsonMapper.builder()
			.addModules(JacksonUtils.instantiateAvailableModules())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();
	}

	/**
	 * Provides the expected format of the response, instructing that it should adhere to
	 * the generated JSON schema.
	 * @return The instruction format string.
	 */
	@Override
	public String getFormat() {
		// 中文：提示词模板。除了要求输出合规 JSON、禁止解释性文字与 Markdown 围栏外，
		// 最关键的是把完整 JSON Schema 塞进提示词，让模型「照着字段定义填空」
		String template = """
				Your response should be in JSON format.
				Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
				Do not include markdown code blocks in your response.
				Remove the ```json markdown from the output.
				Here is the JSON Schema instance your output must adhere to:
				```%s```
				""";
		// 中文：填入构造时缓存好的 Schema，无需每次重新生成
		return String.format(template, this.jsonSchema);
	}

	/**
	 * Provides the generated JSON schema for the target type.
	 * @return The generated JSON schema.
	 */
	@Override
	// 中文：覆写父接口的默认实现，返回真实的 Schema 字符串。
	// 支持原生结构化输出的模型（如 OpenAI response_format）会直接把它下发给服务端做强约束
	public String getJsonSchema() {
		return this.jsonSchema;
	}

	// 中文：把 Schema 字符串解析为 Map 形式，便于某些需要以对象结构传参的模型 API 使用。
	// 解析失败说明 Schema 生成环节已出问题，属于不该发生的内部错误，
	// 故记录日志后包装为 IllegalStateException 抛出（快速失败，而非静默返回 null）
	public Map<String, Object> getJsonSchemaMap() {
		try {
			return this.jsonMapper.readValue(this.jsonSchema, Map.class);
		}
		catch (JacksonException ex) {
			logger.error("Could not parse the JSON Schema to a Map object", ex);
			throw new IllegalStateException(ex);
		}
	}

}
