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
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * 【中文说明】MutableResponseMetadata 是 {@link ResponseMetadata} 的<b>可变实现</b>，
 * 在只读接口之上额外提供了 put / remove / clear / computeIfAbsent 等写入能力。
 *
 * <p>
 * 用途：模型实现类在解析厂商 API 返回结果时，需要一个"能往里塞数据"的元数据容器；
 * 而暴露给业务方的 {@link ResponseMetadata} 接口本身只有读方法，从而在类型层面区分了
 * "生产者可写、消费者只读"这两种视角。
 *
 * <p>
 * 关键字段：私有的 {@link ConcurrentHashMap} 保证并发安全，尤其适配流式响应下
 * 元数据被增量累积的场景。注意 ConcurrentHashMap 不接受 null 值。
 *
 * <p>
 * 典型用法：{@code new MutableResponseMetadata().put("usage", usage).put("id", id)}，
 * put 返回 this 支持链式调用。
 */
public class MutableResponseMetadata implements ResponseMetadata {

	// 【中文】底层存储容器。相比父级抽象类的 protected 字段，这里设为 private，
	// 强制外部只能通过本类方法访问；如确需原始 Map 可用 getRawMap()。
	private final Map<String, Object> map = new ConcurrentHashMap<>();

	/**
	 * Puts an element to the context.
	 * @param key key
	 * @param object value
	 * @param <T> value type
	 * @return this for chaining
	 */
	// 【中文】写入一个元数据条目。返回 this 以支持链式调用（fluent API 风格）。
	// 注意：底层为 ConcurrentHashMap，传入 null 值会抛 NullPointerException。
	public <T> MutableResponseMetadata put(String key, T object) {
		this.map.put(key, object);
		return this;
	}

	/**
	 * Gets an entry from the context. Returns {@code null} when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @return entry or {@code null} if not present
	 */
	// 【中文】按 key 取值，缺失返回 null（未检查的强制转换，类型由调用方保证）。
	@Override
	@Nullable public <T> T get(String key) {
		return (T) this.map.get(key);
	}

	/**
	 * Removes an entry from the context.
	 * @param key key by which to remove an entry
	 * @return the previous value associated with the key, or null if there was no mapping
	 * for the key
	 */
	// 【中文】移除指定条目，并返回它之前对应的值；若原本就不存在则返回 null。
	// 这是 ResponseMetadata 只读接口之外新增的写操作之一。
	public Object remove(Object key) {
		return this.map.remove(key);
	}

	/**
	 * Gets an entry from the context. Throws exception when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @throws IllegalArgumentException if not present
	 * @return entry
	 */
	// 【中文】严格版取值：key 不存在时抛 IllegalArgumentException（快速失败）。
	@Override
	public <T> T getRequired(Object key) {
		T object = (T) this.map.get(key);
		// 空值校验：缺失即视为调用方错误，直接抛异常并附带 key 信息
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
	@Override
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
	// 【中文】取值，缺失时返回给定默认值。
	@Override
	public <T> T getOrDefault(Object key, T defaultObject) {
		return (T) this.map.getOrDefault(key, defaultObject);
	}

	// 【中文】返回键值对的<b>只读视图</b>：即使本类本身可变，也不允许外部通过 entrySet 直接改内容。
	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableMap(this.map).entrySet();
	}

	// 【中文】返回 key 集合的只读视图。
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

	// 【中文】判断元数据是否为空。
	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	/**
	 * Returns an element or calls a mapping function if entry not present. The function
	 * will insert the value to the map.
	 * @param key key
	 * @param mappingFunction mapping function
	 * @param <T> value type
	 * @return object or one derived from the mapping function if not present
	 */
	// 【中文】"取不到就计算并写入"：key 存在时直接返回旧值；不存在时调用 mappingFunction
	// 生成新值，<b>存入 map 后</b>再返回。常用于惰性初始化累加器（如流式场景累积 usage）。
	// 参数用 Function<Object, ? extends T>（PECS 中的生产者通配符），允许传入返回 T 子类型的函数。
	public <T> T computeIfAbsent(String key, Function<Object, ? extends T> mappingFunction) {
		return (T) this.map.computeIfAbsent(key, mappingFunction);
	}

	/**
	 * Clears the entries from the context.
	 */
	// 【中文】清空所有元数据条目，一般用于对象复用或重置场景。
	public void clear() {
		this.map.clear();
	}

	// 【中文】直接返回内部可变 Map 的引用（注意：不是只读副本！）。
	// 这是一个"逃逸出口"，主要给框架内部做批量填充用；业务代码应避免使用，
	// 否则可能绕过封装意外修改元数据内容。
	public Map<String, Object> getRawMap() {
		return this.map;
	}

}
