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

package org.springframework.ai.util.json;

import java.lang.reflect.Type;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.util.JsonHelper;

/**
 * Utilities to perform parsing operations between JSON and Java.
 *
 * @deprecated Use {@link JacksonUtils} or {@link JsonHelper} instead
 */
/**
 * 【中文说明】JSON 与 Java 对象互转的工具类（<b>已整体废弃</b>）。
 *
 * <p>
 * 用途：历史上是 Spring AI 中工具调用与结构化输出的 JSON 解析入口。现已标注
 * {@code @Deprecated(forRemoval = true)}，即“计划在未来版本移除”，新代码请改用
 * {@link JacksonUtils}（获取 JsonMapper）或 {@link JsonHelper}（读写 JSON）。
 *
 * <p>
 * 当前实现要点：本类已被彻底“空心化”——所有静态方法都只是转调内部的 {@code jsonHelper} 或
 * {@link JacksonUtils}，自身不再持有任何解析逻辑。这是一种典型的<b>废弃过渡适配层</b>写法：
 * 保留旧 API 签名以维持二进制兼容，同时把真正实现迁移到新类，方便老用户平滑升级。
 *
 * <p>
 * 关键字段：{@code jsonHelper} 是 {@code private static final} 的共享实例（无状态，线程安全）。
 *
 * @deprecated 请改用 {@link JacksonUtils} 或 {@link JsonHelper}
 */
@Deprecated(forRemoval = true)
public final class JsonParser {

	// 委托目标：所有实例方法调用最终都落到这个共享的 JsonHelper 上
	private static final JsonHelper jsonHelper = new JsonHelper();

	// 私有构造器：纯静态工具类，禁止实例化
	private JsonParser() {
	}

	/**
	 * Returns a Jackson {@link JsonMapper} instance tailored for JSON-parsing operations
	 * for tool calling and structured output.
	 * @deprecated Use {@link JacksonUtils#getDefaultJsonMapper} instead
	 */
	@Deprecated(forRemoval = true)
	// 直接转调 JacksonUtils，返回框架预配置好的 JsonMapper（已注册所需模块）
	public static JsonMapper getJsonMapper() {
		return JacksonUtils.getDefaultJsonMapper();
	}

	/**
	 * Converts a JSON string to a Java object.
	 * @deprecated Use {@link JsonHelper#fromJson(String, Class)} instead
	 */
	@Deprecated(forRemoval = true)
	// 重载一：按 Class 反序列化，适用于非泛型的简单类型；返回值可能为 null
	public static <T> @Nullable T fromJson(String json, Class<T> type) {
		return jsonHelper.fromJson(json, type);
	}

	/**
	 * Converts a JSON string to a Java object.
	 * @deprecated Use {@link JsonHelper#fromJson(String, Type)} instead
	 */
	@Deprecated(forRemoval = true)
	// 重载二：按 java.lang.reflect.Type 反序列化，可承载带泛型参数的类型信息
	public static <T> @Nullable T fromJson(String json, Type type) {
		return jsonHelper.fromJson(json, type);
	}

	/**
	 * Converts a JSON string to a Java object.
	 * @deprecated Use
	 * {@link JsonHelper#fromJson(String, org.springframework.core.ParameterizedTypeReference)}
	 * instead
	 */
	@Deprecated(forRemoval = true)
	// 重载三：接收 Jackson 的 TypeReference（匿名子类捕获泛型），
	// 内部用 getType() 拆出底层 Type 再委托，解决泛型擦除下的 List<Foo> 等类型还原问题
	public static <T> @Nullable T fromJson(String json, TypeReference<T> type) {
		return jsonHelper.fromJson(json, type.getType());
	}

	/**
	 * Converts a Java object to a JSON string if it's not already a valid JSON string.
	 * @deprecated Use {@link JsonHelper#toJson(Object, boolean)} instead
	 */
	@Deprecated(forRemoval = true)
	// 序列化为 JSON 字符串；第二个参数 true 表示“若入参本身已是合法 JSON 字符串则原样返回”，
	// 避免出现被二次转义的双重编码问题
	public static String toJson(@Nullable Object object) {
		return jsonHelper.toJson(object, true);
	}

	/**
	 * Convert a Java Object to a typed Object. Based on the implementation in
	 * MethodToolCallback.
	 * @deprecated Use {@link JsonHelper#convertToTypedObject} instead
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Deprecated(forRemoval = true)
	// 把松散的 Java 对象（如 Map/List）转换成目标类型实例，
	// 主要用于工具调用时把模型返回的参数映射成方法形参类型
	public static Object toTypedObject(Object value, Class<?> type) {
		return jsonHelper.convertToTypedObject(value, type);
	}

}
