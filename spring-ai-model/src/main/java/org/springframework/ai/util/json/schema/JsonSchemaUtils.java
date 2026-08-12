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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.model.KotlinModule;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.KotlinDetector;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with JSON schemas.
 *
 * @author Guangdong Liu
 * @author Ilayaperumal Gopinathan
 * @author Sebastien Deleuze
 * @since 1.0.0
 */
/**
 * 【中文说明】JSON Schema 的辅助操作工具类。
 *
 * <p>
 * 用途：与 {@code JsonSchemaGenerator}（负责“生成”）互补，本类负责对已有 Schema 做“加工与修复”，
 * 以及提供一个轻量的 Schema 生成入口。
 *
 * <p>
 * 三大核心能力：
 * <ul>
 * <li>{@link #hoistDefsToRoot(ObjectNode, ObjectNode)}：把子 Schema 的 {@code $defs} 定义块上提到根
 * Schema，并处理<b>键名冲突</b>与 {@code $ref} 引用重写。这是本类最复杂、最值得细读的方法。</li>
 * <li>{@link #ensureValidInputSchema(String)}：规范化来自外部（如 MCP 工具）的 Schema，
 * 补齐 {@code type} 与 {@code properties} 字段。</li>
 * <li>{@link #getJsonSchema(Type)}：为给定类型生成 Draft 2020-12 的 Schema 节点。</li>
 * </ul>
 *
 * <p>
 * 关键字段：{@code jsonHelper} 与 {@code schemaGenerator} 均为静态共享实例，后者非线程安全，
 * 使用时需加锁（见 {@link #getJsonSchema}）。
 *
 * @author Guangdong Liu
 * @author Ilayaperumal Gopinathan
 * @author Sebastien Deleuze
 * @since 1.0.0
 */
public final class JsonSchemaUtils {

	// JSON 读写助手，用于 ensureValidInputSchema 中的字符串<->Map 互转
	private static final JsonHelper jsonHelper = new JsonHelper();

	// 共享的 Schema 生成器；注意它不是线程安全的，调用处需 synchronized
	private static final SchemaGenerator schemaGenerator;

	// 静态初始化：类加载时构建生成器配置。
	// 与 JsonSchemaGenerator 中的配置相似但更精简（未加载 SpringAiSchemaModule）
	static {
		// 尊重 @JsonProperty(required=...)
		JacksonSchemaModule jacksonModule = new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
		// 支持 Swagger 的 @Schema 注解
		Swagger2Module swaggerModule = new Swagger2Module();

		SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
				OptionPreset.PLAIN_JSON)
			.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
			.with(Option.PLAIN_DEFINITION_KEYS)
			.with(swaggerModule)
			.with(jacksonModule);

		// 条件装配 Kotlin 支持，类路径无 kotlin-reflect 时跳过
		if (KotlinDetector.isKotlinReflectPresent()) {
			configBuilder.with(new KotlinModule());
		}

		SchemaGeneratorConfig config = configBuilder.build();
		schemaGenerator = new SchemaGenerator(config);
	}

	// 私有构造器：纯静态工具类，禁止实例化
	private JsonSchemaUtils() {
	}

	/**
	 * Moves any {@code $defs} block found on {@code subSchema} into the {@code $defs}
	 * block of {@code rootSchema}, creating one on the root if needed. On key collisions
	 * the existing root entry is reused when the two definitions are structurally equal;
	 * otherwise the incoming entry is renamed with a numeric suffix and every
	 * {@code "#/$defs/<oldKey>"} reference inside the inlined sub-schema and inside every
	 * definition inserted by this call is rewritten to point at the new key.
	 * <p>
	 * This is needed because victools generates self-contained schemas where
	 * {@code $defs} and the {@code $ref} pointers into them are rooted at the sub-schema.
	 * Inlining the sub-schema under {@code properties.<paramName>} re-parents existing
	 * {@code "#/$defs/<Name>"} refs to the outer root, leaving them unresolvable unless
	 * {@code $defs} is hoisted first.
	 * @param rootSchema the wrapper schema that will receive the hoisted definitions
	 * @param subSchema the per-parameter sub-schema whose {@code $defs} block is consumed
	 */
	/**
	 * 【中文说明】把子 Schema 的 {@code $defs} 定义上提到根 Schema，并修复引用。
	 *
	 * <p>
	 * 处理流程分四步：
	 * <ol>
	 * <li>从子 Schema 中摘除（remove）{@code $defs} 节点，无则直接返回。</li>
	 * <li>逐个搬运定义到根节点，遇到同名键时分三种情况处理：根上没有 → 直接放；
	 * 根上有且内容完全相同 → 复用不动；根上有但内容不同 → 改名并记录映射。</li>
	 * <li>若产生了改名，重写子 Schema 内所有指向旧键的 {@code $ref}。</li>
	 * <li>同样重写“本次新插入的定义”内部的 {@code $ref}。</li>
	 * </ol>
	 *
	 * <p>
	 * 第 4 步只处理本批次插入项是个重要细节：更早批次插入的定义已按各自的映射改写过，
	 * 若用当前批次的映射再改一次，会把那些本就合法地指向原始键名的引用改坏。
	 */
	public static void hoistDefsToRoot(ObjectNode rootSchema, ObjectNode subSchema) {
		// remove 会同时返回被移除的节点，一步完成“取出并摘除”
		JsonNode nestedDefs = subSchema.remove("$defs");
		// 子 Schema 没有 $defs（或类型异常），无需处理
		if (nestedDefs == null || !nestedDefs.isObject()) {
			return;
		}
		// 取根节点的 $defs；不存在则创建一个（putObject 会返回新建的节点）
		ObjectNode rootDefs = rootSchema.has("$defs") ? (ObjectNode) rootSchema.get("$defs")
				: rootSchema.putObject("$defs");
		// Collect all keys from this batch upfront so uniqueDefsKey can avoid claiming
		// a suffix that another pending entry in the same batch already occupies.
		// 预先收集本批次全部键名，供改名时避让——防止新名字撞上同批次中尚未搬运的另一个键
		Set<String> batchKeys = new HashSet<>();
		((ObjectNode) nestedDefs).properties().forEach(e -> batchKeys.add(e.getKey()));
		// 旧键名 -> 新键名 的映射；用 LinkedHashMap 保持插入顺序，便于调试复现
		Map<String, String> renames = new LinkedHashMap<>();
		// 记录本次调用真正插入到 rootDefs 的键，用于后续限定 $ref 重写范围
		List<String> insertedKeys = new ArrayList<>();
		((ObjectNode) nestedDefs).properties().forEach(entry -> {
			String key = entry.getKey();
			JsonNode value = entry.getValue();
			// 情况一：根上无同名定义，直接搬过去
			if (!rootDefs.has(key)) {
				rootDefs.set(key, value);
				insertedKeys.add(key);
				return;
			}
			// 情况二：同名且结构完全相等，说明是同一个类型，复用已有定义即可，无需改名
			if (rootDefs.get(key).equals(value)) {
				return;
			}
			// 情况三：同名但内容不同（真冲突），生成带数字后缀的新键名
			String renamed = uniqueDefsKey(rootDefs, key, batchKeys);
			rootDefs.set(renamed, value);
			renames.put(key, renamed);
			insertedKeys.add(renamed);
		});
		// 没有发生任何改名，引用天然有效，提前返回省去遍历开销
		if (renames.isEmpty()) {
			return;
		}
		// 修复子 Schema 内部指向旧键名的引用
		rewriteDefsRefs(subSchema, renames);
		// Only rewrite $refs inside the definitions inserted by THIS call. Entries added
		// to rootDefs by earlier hoistDefsToRoot calls have already had their own renames
		// applied; rewriting them again with the current batch's rename map would corrupt
		// any $refs that legitimately point at the original (pre-rename) keys.
		// 严格限定在本批次插入的定义上重写，理由见上方英文注释与中文 javadoc 的第 4 步说明
		for (String insertedKey : insertedKeys) {
			rewriteDefsRefs(rootDefs.get(insertedKey), renames);
		}
	}

	// 生成不冲突的新键名：从 base_2 开始递增，直到既不与根上已有键冲突，也不与本批次待搬运键冲突
	private static String uniqueDefsKey(ObjectNode rootDefs, String base, Set<String> batchKeys) {
		// 后缀从 2 起（原始键视为 1），符合 Foo、Foo_2、Foo_3 的直觉命名
		int suffix = 2;
		String candidate;
		do {
			candidate = base + "_" + suffix++;
		}
		// 双重去重条件缺一不可，否则可能占用同批次另一个键的名字
		while (rootDefs.has(candidate) || batchKeys.contains(candidate));
		return candidate;
	}

	// 递归遍历 JSON 树，把所有 "#/$defs/旧键" 形式的引用改写为新键
	private static void rewriteDefsRefs(@Nullable JsonNode node, Map<String, String> renames) {
		// 空值保护：递归入口可能传入 null（如 rootDefs.get 未命中）
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			ObjectNode object = (ObjectNode) node;
			JsonNode refNode = object.get("$ref");
			// 只处理字符串类型的 $ref
			if (refNode != null && refNode.isString()) {
				String ref = refNode.asString();
				String prefix = "#/$defs/";
				// 只改写本地 $defs 引用，外部 URL 引用不动
				if (ref.startsWith(prefix)) {
					String rest = ref.substring(prefix.length());
					// 引用可能是 "#/$defs/Foo" 或更深的 "#/$defs/Foo/properties/bar"，
					// 因此以第一个 '/' 为界切出定义键名，slash < 0 表示没有后续路径
					int slash = rest.indexOf('/');
					String key = slash < 0 ? rest : rest.substring(0, slash);
					String renamed = renames.get(key);
					// 命中改名映射才重写，并把原有的后续路径原样拼回
					if (renamed != null) {
						object.put("$ref", prefix + renamed + (slash < 0 ? "" : rest.substring(slash)));
					}
				}
			}
			// 继续深入所有子节点
			object.properties().forEach(e -> rewriteDefsRefs(e.getValue(), renames));
		}
		// 数组节点：逐个元素递归
		else if (node.isArray()) {
			node.forEach(child -> rewriteDefsRefs(child, renames));
		}
	}

	/**
	 * Ensures that the input schema is valid for AI model APIs. Many AI models require
	 * that the parameters object must have a "properties" field, even if it's empty. This
	 * method normalizes schemas from external sources (like MCP tools) that may not
	 * include this field.
	 * @param inputSchema the input schema as a JSON string
	 * @return a valid input schema as a JSON string with required fields
	 */
	// 规范化外部来源的 Schema：许多模型 API 要求 object 类型必须带 properties 字段（哪怕是空对象）
	public static String ensureValidInputSchema(String inputSchema) {
		// 空白输入原样返回（含 null），不做任何加工
		if (!StringUtils.hasText(inputSchema)) {
			return inputSchema;
		}

		Map<String, Object> schemaMap = jsonHelper.fromJsonToMap(inputSchema);

		// 解析结果为空（如 "{}"）：直接构造一个最小可用 Schema 返回
		if (schemaMap.isEmpty()) {
			// Create a minimal valid schema
			schemaMap = new java.util.HashMap<>();
			schemaMap.put("type", "object");
			schemaMap.put("properties", new java.util.HashMap<>());
			return jsonHelper.toJson(schemaMap);
		}

		// Ensure "type" field exists
		// 补齐 type：外部 Schema 常省略此字段，默认按对象处理
		if (!schemaMap.containsKey("type")) {
			schemaMap.put("type", "object");
		}

		// Ensure "properties" field exists for object types
		// 仅当类型确为 object 时才补 properties；注意用常量在前的 equals 写法规避 NPE
		if ("object".equals(schemaMap.get("type")) && !schemaMap.containsKey("properties")) {
			schemaMap.put("properties", new java.util.HashMap<>());
		}

		return jsonHelper.toJson(schemaMap);
	}

	/**
	 * Generates JSON Schema (version 2020_12) for the given class.
	 * @param inputType the input {@link Type} to generate JSON Schema from.
	 * @return the generated JSON Schema as a String.
	 * @since 2.0.0
	 */
	// 为给定类型生成 Draft 2020-12 的 Schema 节点
	public static ObjectNode getJsonSchema(Type inputType) {

		ObjectNode node;
		// 加锁原因：victools 的 SchemaGenerator 非线程安全，而此处是静态共享实例
		synchronized (schemaGenerator) {
			node = schemaGenerator.generateSchema(inputType);
		}

		// Void 特例：无返回值类型不会生成 properties，补一个空对象以满足模型 API 的格式要求
		if ((inputType == Void.class) && !node.has("properties")) {
			node.putObject("properties");
		}

		return node;
	}

}
