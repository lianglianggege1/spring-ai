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

package org.springframework.ai.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * 【中文说明】AbstractResponseMetadata 是响应元数据的<b>抽象基类</b>，为各厂商的
 * XxxChatResponseMetadata 提供开箱即用的 Map 存取实现，避免重复造轮子。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@link #AI_METADATA_STRING} —— 统一的 toString 格式模板，子类拼装可读输出时复用；</li>
 * <li>{@link #map} —— 底层存储，使用 {@link ConcurrentHashMap} 保证并发安全（流式响应中
 * 元数据可能在不同线程被写入/读取）。声明为 protected 是为了让子类能直接填充数据。</li>
 * </ul>
 *
 * <p>
 * 值得注意的设计细节：本类<b>并未</b>显式 implements {@link ResponseMetadata}，
 * 而是"鸭子类型"式地提供了同名方法，由具体子类去声明实现该接口，从而在复用代码的同时
 * 保留子类对接口实现方式的自由度。
 *
 * <p>
 * 典型用法：子类在构造或解析 HTTP 响应时通过 {@code map.put(...)} 写入 usage、rateLimit 等，
 * 对外则只暴露只读的 get 系列方法。
 */
public class AbstractResponseMetadata {

	/**
	 * AI metadata string format.
	 */
	// 【中文】元数据的标准字符串格式模板，使用位置参数（%1$s 等）便于子类用 String.format 拼接，
	// 依次对应：响应 id、token 用量 usage、限流信息 rateLimit。
	protected static final String AI_METADATA_STRING = "{ id: %1$s, usage: %2$s, rateLimit: %3$s }";

	/**
	 * Metadata map.
	 */
	// 【中文】实际存放元数据键值对的容器。选用 ConcurrentHashMap 而非 HashMap，
	// 是为了应对响应式/流式场景下的跨线程访问；注意 ConcurrentHashMap 不允许 null 键或 null 值。
	protected final Map<String, Object> map = new ConcurrentHashMap<>();

	/**
	 * Create a new {@link AbstractResponseMetadata} instance.
	 */
	// 【中文】无参构造：显式写出以便子类调用 super()，同时保证 JavaDoc 完整。
	public AbstractResponseMetadata() {
	}

	/**
	 * Gets an entry from the context. Returns {@code null} when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @return entry or {@code null} if not present
	 */
	// 【中文】按 key 取值，缺失返回 null。
	// 这里的 (T) 是"未检查的强制转换"：编译期无法校验，实际类型由调用方在接收变量处决定，
	// 若与存入类型不符会在赋值瞬间抛 ClassCastException。
	public <T> @Nullable T get(String key) {
		return (T) this.map.get(key);
	}

	/**
	 * Gets an entry from the context. Throws exception when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @return entry
	 * @throws IllegalArgumentException if not present
	 */
	// 【中文】严格版取值：缺失时抛异常而非返回 null，实现 fail-fast。
	public <T> T getRequired(Object key) {
		T object = (T) this.map.get(key);
		// 空值校验：key 不存在时直接抛出 IllegalArgumentException，并在消息中带上 key 便于排查
		if (object == null) {
			throw new IllegalArgumentException("Context does not have an entry for key [" + key + "]");
		}
		return object;
	}

	/**
	 * Checks if context contains a key.
	 * @param key key
	 * @return {@code true} when the context contains the entry with the given key
	 */
	// 【中文】判断是否包含指定 key。
	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	/**
	 * Returns an element or default if not present.
	 * @param key key
	 * @param defaultObject default object to return
	 * @param <T> value type
	 * @return object or default if not present
	 */
	// 【中文】取值，缺失时返回给定默认值，避免调用方到处写判空。
	public <T> T getOrDefault(Object key, T defaultObject) {
		return (T) this.map.getOrDefault(key, defaultObject);
	}

	// 【中文】返回所有键值对。此处用 Collections.unmodifiableMap 包一层再取 entrySet，
	// 目的是返回<b>只读视图</b>，防止外部代码绕过本类直接修改内部 map（保护封装性）。
	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableMap(this.map).entrySet();
	}

	// 【中文】返回所有 key 的只读视图，同样做了不可变包装以防外部篡改。
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

	// 【中文】判断当前是否没有任何元数据。
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

}
