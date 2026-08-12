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

package org.springframework.ai.util.json.schema;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.KotlinModule;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.Nullness;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Utilities to generate JSON Schemas from Java types and method signatures. It's designed
 * to work well in the context of tool calling and structured outputs, aiming at ensuring
 * consistency and robustness across different model providers.
 * <p>
 * Metadata such as descriptions and required properties can be specified using one of the
 * following supported annotations:
 * <p>
 * <ul>
 * <li>{@code @ToolParam(required = ..., description = ...)}</li>
 * <li>{@code @JsonProperty(required = ...)}</li>
 * <li>{@code @JsonClassDescription(...)}</li>
 * <li>{@code @JsonPropertyDescription(...)}</li>
 * <li>{@code @Schema(required = ..., description = ...)}</li>
 * <li>{@code @Nullable}</li>
 * </ul>
 * <p>
 * If none of these annotations are present, the default behavior is to consider the
 * property as required and not to include a description.
 * <p>
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 * @since 1.0.0
 */
/**
 * 【中文说明】从 Java 类型与方法签名生成 JSON Schema 的核心工具类。
 *
 * <p>
 * 用途：这是「工具调用（Tool Calling）」与「结构化输出（Structured Output）」两大能力的基石。
 * 模型需要知道一个工具方法接受哪些参数、类型是什么、哪些必填，这些信息就由本类把 Java 反射信息
 * 翻译成模型能理解的 JSON Schema。
 *
 * <p>
 * 两个核心入口：
 * <ul>
 * <li>{@link #generateForMethodInput(Method, SchemaOption...)}：为方法的<b>入参列表</b>生成 Schema，
 * 手工拼装一个 type=object 的顶层结构，每个参数对应一个 property。</li>
 * <li>{@link #generateForType(Type, SchemaOption...)}：为<b>单个类型</b>生成 Schema，
 * 常用于结构化输出（约束模型返回某个 DTO 的形状）。</li>
 * </ul>
 *
 * <p>
 * 关键静态字段：
 * <ul>
 * <li>{@code PROPERTY_REQUIRED_BY_DEFAULT}：全局默认策略——所有属性默认必填，
 * 这是为了在不同厂商间保持一致与健壮（部分模型对 optional 字段处理不佳）。</li>
 * <li>{@code typeSchemaGenerator} / {@code subtypeSchemaGenerator}：两个预配置的生成器，
 * 区别仅在于后者去掉了 {@code $schema} 版本声明——因为它生成的是嵌套在大 Schema 里的子结构，
 * 子结构不应重复声明版本。</li>
 * </ul>
 *
 * <p>
 * 兼容性处理（阅读时值得留意的“坑点”）：会跳过 {@code ToolContext} 参数与 Kotlin 挂起函数的
 * {@code Continuation} 参数、移除 OpenAPI 的 {@code format} 关键字、把子 Schema 的 {@code $defs}
 * 上提到根节点等，都是为了适配各家模型的实际表现。
 *
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 * @since 1.0.0
 */
public final class JsonSchemaGenerator {

	/**
	 * To ensure consistency and robustness across different model providers, all
	 * properties in the JSON Schema are considered required by default. This behavior can
	 * be overridden by setting the {@link ToolParam#required()},
	 * {@link JsonProperty#required()}, or {@link Schema#requiredMode()}} annotation.
	 */
	// 全局默认：属性一律必填。设为常量而非可配置项，是为了保证跨厂商行为一致
	private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;

	// 顶层类型生成器：输出的 Schema 带 $schema 版本标识
	private static final SchemaGenerator typeSchemaGenerator;

	// 子类型生成器：用于方法参数等嵌套场景，输出不带 $schema 版本标识
	private static final SchemaGenerator subtypeSchemaGenerator;

	/*
	 * Initialize JSON Schema generators.
	 */
	// 静态初始化块：类加载时一次性构建两个生成器，后续复用（构建成本较高）
	static {
		// Jackson 模块：让生成器尊重 @JsonProperty 的 required 与字段顺序声明
		Module jacksonModule = new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
				JacksonOption.RESPECT_JSONPROPERTY_ORDER);
		// Swagger 模块：支持 @Schema 注解
		Module openApiModule = new Swagger2Module();
		// Spring AI 自有模块：支持 @ToolParam 注解。三元表达式根据默认必填策略选择构造方式
		Module springAiSchemaModule = PROPERTY_REQUIRED_BY_DEFAULT ? new SpringAiSchemaModule()
				: new SpringAiSchemaModule(SpringAiSchemaModule.Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);

		// 建造者：指定 Schema 规范版本为 Draft 2020-12，预设为 PLAIN_JSON（纯净 JSON 风格）
		SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(
				SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
			.with(jacksonModule)
			.with(openApiModule)
			.with(springAiSchemaModule)
			// 输出额外的 OpenAPI format 值（如 date-time），提升类型表达力
			.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
			// 使用简洁的定义键名，避免 $defs 中出现冗长的全限定名
			.with(Option.PLAIN_DEFINITION_KEYS);

		// 条件装配：仅当类路径存在 kotlin-reflect 时才加载 Kotlin 模块，避免非 Kotlin 项目报错
		if (KotlinDetector.isKotlinReflectPresent()) {
			schemaGeneratorConfigBuilder.with(new KotlinModule());
		}

		SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
		typeSchemaGenerator = new SchemaGenerator(typeSchemaGeneratorConfig);

		// 复用同一个 builder，仅去掉 $schema 版本标识后再 build 一次；
		// 因为嵌套的子 Schema 若携带版本声明会导致部分校验器报错
		SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
			.without(Option.SCHEMA_VERSION_INDICATOR)
			.build();
		subtypeSchemaGenerator = new SchemaGenerator(subtypeSchemaGeneratorConfig);
	}

	// 私有构造器：纯静态工具类，禁止实例化
	private JsonSchemaGenerator() {
	}

	// 统一的生成入口，加 synchronized 是因为 victools 的 SchemaGenerator 并非线程安全，
	// 而这里的生成器是静态共享的，多线程并发生成时必须串行化
	private static ObjectNode generateSchema(SchemaGenerator generator, Type type) {
		synchronized (generator) {
			return generator.generateSchema(type);
		}
	}

	/**
	 * Generate a JSON Schema for a method's input parameters.
	 */
	// 为方法入参生成 Schema：手工拼装顶层 object 结构，逐个参数填充 properties 与 required
	public static String generateForMethodInput(Method method, SchemaOption... schemaOptions) {
		ObjectNode schema = JacksonUtils.getDefaultJsonMapper().createObjectNode();
		// 顶层声明 Schema 规范版本
		schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
		// 方法入参整体被建模为一个 JSON 对象
		schema.put("type", "object");
		// 预先创建 $defs 容器，供子 Schema 的复用定义上提到此处；若最终为空会在后面删除
		ObjectNode defs = schema.putObject("$defs");

		ObjectNode properties = schema.putObject("properties");
		// 必填参数名清单，最后转成 JSON 数组
		List<String> required = new ArrayList<>();

		for (int i = 0; i < method.getParameterCount(); i++) {
			// 注意：能否拿到真实参数名取决于编译时是否带 -parameters 选项，否则会是 arg0/arg1
			String parameterName = method.getParameters()[i].getName();
			// 用 getGenericParameterTypes 而非 getParameterTypes，以保留 List<String> 这类泛型信息
			Type parameterType = method.getGenericParameterTypes()[i];
			// 排除规则一：ToolContext 是框架传递上下文的特殊参数，对模型不可见
			if (parameterType instanceof Class<?> parameterClass
					&& ClassUtils.isAssignable(ToolContext.class, parameterClass)) {
				// A ToolContext method parameter is not included in the JSON Schema
				// generation.
				// It's a special type used by Spring AI to pass contextual data to tools
				// outside the model interaction flow.
				continue;
			}
			// A Kotlin suspend function carries a synthetic trailing Continuation
			// parameter that is not part of the tool contract and must not appear in
			// the generated schema.
			// 排除规则二：Kotlin 挂起函数会被编译器追加一个隐式的 Continuation 末位参数，
			// 它属于编译产物而非业务契约，必须跳过（判断条件锁定“最后一个参数”）
			if (KotlinDetector.isSuspendingFunction(method) && i == method.getParameterCount() - 1) {
				continue;
			}
			// 必填判定通过后记录参数名
			if (isMethodParameterRequired(method, i)) {
				required.add(parameterName);
			}
			// 用 subtype 生成器：产出的子 Schema 不带 $schema 版本标识
			ObjectNode parameterNode = generateSchema(subtypeSchemaGenerator, parameterType);
			// victools generates self-contained schemas where $defs and the $ref
			// pointers into them are rooted at the sub-schema. Inlining the
			// sub-schema under properties.<paramName> re-parents existing
			// "#/$defs/<Name>" refs to the outer root, leaving them unresolvable.
			// Hoist $defs to the outer root so those refs resolve again.
			// 关键修正：victools 生成的子 Schema 是自包含的，其 $defs 与指向它的 "#/$defs/xxx"
			// 引用都以子 Schema 为根。一旦把子 Schema 内联到 properties.<参数名> 下，
			// 这些 $ref 的锚点就变成了外层根节点从而失效。此处把 $defs 上提到外层根以修复引用
			JsonSchemaUtils.hoistDefsToRoot(schema, parameterNode);
			// Remove OpenAPI format as some LLMs (like Mistral) don't handle them.
			// 兼容性处理：部分模型（如 Mistral）无法正确处理 format 关键字，直接移除
			parameterNode.remove("format");
			String parameterDescription = getMethodParameterDescription(method, i);
			// 有描述才写入，避免产生 "description": "" 这样的噪声字段
			if (StringUtils.hasText(parameterDescription)) {
				parameterNode.put("description", parameterDescription);
			}
			properties.set(parameterName, parameterNode);
		}

		// 收尾清理：若没有任何子定义被上提，删除空的 $defs 节点，保持 Schema 干净
		if (defs.isEmpty()) {
			schema.remove("$defs");
		}

		// 把必填清单写成 JSON 数组（即便为空也保留 required 字段）
		var requiredArray = schema.putArray("required");
		required.forEach(requiredArray::add);

		// 应用调用方传入的可选后处理（禁止额外属性、类型值大写等）
		processSchemaOptions(schemaOptions, schema);

		// 输出格式化后的 JSON 字符串
		return schema.toPrettyString();
	}

	/**
	 * Generate a JSON Schema for a class type.
	 */
	// 为单个 Java 类型生成完整 Schema（结构化输出场景的主入口）
	public static String generateForType(Type type, SchemaOption... schemaOptions) {
		// 参数校验：类型不可为空
		Assert.notNull(type, "type cannot be null");
		// 这里用 typeSchemaGenerator，产出的是带 $schema 版本标识的顶层 Schema
		ObjectNode schema = generateSchema(typeSchemaGenerator, type);
		// 特例兜底：Void 类型不会产生任何属性，但部分模型要求 object 必须带 properties 字段，
		// 因此补一个空对象避免请求被拒
		if ((type == Void.class) && !schema.has("properties")) {
			schema.putObject("properties");
		}
		processSchemaOptions(schemaOptions, schema);
		return schema.toPrettyString();
	}

	// 统一的可选项后处理：两个开关互不相关，可同时生效
	private static void processSchemaOptions(SchemaOption[] schemaOptions, ObjectNode schema) {
		// 默认行为是「禁止额外属性」，只有显式传入 ALLOW_... 才跳过（noneMatch 为真时执行禁止）
		if (Stream.of(schemaOptions)
			.noneMatch(option -> option == SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT)) {
			forbidAdditionalProperties(schema);
		}
		// 大写转换是可选增强，需显式传入才生效（anyMatch）
		if (Stream.of(schemaOptions).anyMatch(option -> option == SchemaOption.UPPER_CASE_TYPE_VALUES)) {
			convertTypeValuesToUpperCase(schema);
		}
	}

	/**
	 * Determines whether a property is required based on the presence of a series of *
	 * annotations.
	 *
	 * <p>
	 * <ul>
	 * <li>{@code @ToolParam(required = ...)}</li>
	 * <li>{@code @JsonProperty(required = ...)}</li>
	 * <li>{@code @Schema(required = ...)}</li>
	 * <li>{@code @Nullable}</li>
	 * </ul>
	 * <p>
	 *
	 * If none of these annotations are present, the default behavior is to consider the *
	 * property as required.
	 */
	// 方法参数的必填判定：与 AbstractSpringAiSchemaModule#checkRequired 同构，
	// 区别在于这里作用于 Parameter（方法参数），那里作用于字段/getter
	private static boolean isMethodParameterRequired(Method method, int index) {
		Parameter parameter = method.getParameters()[index];

		// 优先级 1：@ToolParam
		var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
		if (toolParamAnnotation != null) {
			return toolParamAnnotation.required();
		}

		// 优先级 2：@JsonProperty
		var propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
		if (propertyAnnotation != null) {
			return propertyAnnotation.required();
		}

		// 优先级 3：@Schema，同时兼容 requiredMode 新写法与已废弃的 required 旧写法
		var schemaAnnotation = parameter.getAnnotation(Schema.class);
		if (schemaAnnotation != null) {
			return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
					|| schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
		}

		// 优先级 4：参数的空安全标记，可空则非必填
		Nullness nullness = Nullness.forParameter(parameter);
		if (nullness == Nullness.NULLABLE) {
			return false;
		}

		// 无任何信号时，采用全局默认（必填）
		return PROPERTY_REQUIRED_BY_DEFAULT;
	}

	/**
	 * Determines a property description based on the presence of a series of annotations.
	 *
	 * <p>
	 * <ul>
	 * <li>{@code @ToolParam(description = ...)}</li>
	 * <li>{@code @JsonPropertyDescription(...)}</li>
	 * <li>{@code @Schema(description = ...)}</li>
	 * </ul>
	 * <p>
	 */
	// 方法参数的描述解析：按 @ToolParam -> @JsonPropertyDescription -> @Schema 的优先级依次尝试
	private static @Nullable String getMethodParameterDescription(Method method, int index) {
		Parameter parameter = method.getParameters()[index];

		// 优先级 1：Spring AI 自有注解，语义最贴合工具调用
		var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
		// 每一级都用 hasText 过滤空白值，空白视为“未提供”，继续向下一级回退
		if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
			return toolParamAnnotation.description();
		}

		// 优先级 2：Jackson 的描述注解
		var jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
		if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
			return jacksonAnnotation.value();
		}

		// 优先级 3：Swagger 的 @Schema 描述
		var schemaAnnotation = parameter.getAnnotation(Schema.class);
		if (schemaAnnotation != null && StringUtils.hasText(schemaAnnotation.description())) {
			return schemaAnnotation.description();
		}

		// 三级都没有：返回 null，调用方据此不写 description 字段
		return null;
	}

	/**
	 * Recursively adds {@code "additionalProperties": false} to all object schemas (nodes
	 * with a {@code "properties"} key) that do not already define
	 * {@code "additionalProperties"}. The guard preserves {@code Map<K,V>} schemas where
	 * {@code "additionalProperties"} is a type reference rather than a boolean.
	 */
	// 递归给所有对象型 Schema 加上 "additionalProperties": false，
	// 目的是让模型严格按定义输出，不要臆造额外字段（OpenAI strict 模式的硬性要求）
	private static void forbidAdditionalProperties(ObjectNode node) {
		// 两个条件缺一不可：既要是对象型（有 properties），又要尚未定义 additionalProperties。
		// 后半个条件是关键保护——Map<K,V> 生成的 Schema 会把 additionalProperties 用作类型引用，
		// 若强行覆盖成 false 会破坏 Map 的语义
		if (node.has("properties") && !node.has("additionalProperties")) {
			node.put("additionalProperties", false);
		}
		// 深度遍历所有子节点，确保嵌套对象也被处理
		node.properties().forEach(entry -> {
			JsonNode value = entry.getValue();
			// 子节点是对象：直接递归
			if (value.isObject()) {
				forbidAdditionalProperties((ObjectNode) value);
			}
			// 子节点是数组（如 anyOf/oneOf 列表）：遍历其中的对象元素再递归
			else if (value.isArray()) {
				value.forEach(element -> {
					if (element.isObject()) {
						forbidAdditionalProperties((ObjectNode) element);
					}
				});
			}
		});
	}

	// 递归把所有 "type" 的值转为大写，如 "string" -> "STRING"。
	// 这是为 Vertex AI/Gemini 等要求 OpenAPI 大写类型名的厂商做的方言适配
	public static void convertTypeValuesToUpperCase(ObjectNode node) {
		if (node.isObject()) {
			node.properties().forEach(entry -> {
				JsonNode value = entry.getValue();
				// 对象子节点：递归下钻
				if (value.isObject()) {
					convertTypeValuesToUpperCase((ObjectNode) value);
				}
				// 数组子节点：遍历元素后递归
				else if (value.isArray()) {
					value.forEach(element -> {
						if (element.isObject() || element.isArray()) {
							convertTypeValuesToUpperCase((ObjectNode) element);
						}
					});
				}
				// 命中目标：键名为 "type" 且值为字符串时执行大写转换。
				// 使用 Locale.ROOT 是为了规避土耳其语等区域下 i/I 转换异常的经典陷阱
				else if (value.isString() && entry.getKey().equals("type")) {
					String oldValue = node.get("type").asString();
					node.put("type", oldValue.toUpperCase(Locale.ROOT));
				}
			});
		}
		// 入参本身是数组时的分支处理
		else if (node.isArray()) {
			node.forEach(element -> {
				if (element.isObject() || element.isArray()) {
					convertTypeValuesToUpperCase((ObjectNode) element);
				}
			});
		}
	}

	/**
	 * Options for generating JSON Schemas.
	 */
	/**
	 * 【中文说明】Schema 生成的可选开关，以可变参数形式传入两个 generateForXxx 方法。
	 *
	 * <p>
	 * 两个选项彼此独立，可同时传入。注意它们的默认语义方向相反：
	 * 「禁止额外属性」是默认开启的（传 ALLOW_... 才关闭），
	 * 而「类型大写」是默认关闭的（传 UPPER_... 才开启）。
	 */
	public enum SchemaOption {

		/**
		 * Allow an object to contain additional key/values not defined in the schema.
		 */
		// 放宽约束：允许对象携带 Schema 未定义的额外键值（即不写入 additionalProperties: false）
		ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,

		/**
		 * Convert all "type" values to upper case.
		 */
		// 厂商方言：把所有 type 值转为大写，供 Gemini 等要求 OpenAPI 大写枚举的模型使用
		UPPER_CASE_TYPE_VALUES

	}

}
