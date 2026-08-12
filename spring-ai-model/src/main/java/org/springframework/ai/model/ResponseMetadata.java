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

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Interface representing metadata associated with an AI model's response.
 *
 * @author Mark Pollack
 * @since 1.0.0
 */
/**
 * 【中文说明】ResponseMetadata 表示<b>整次</b>模型响应的元数据，本质是一个只读的
 * "字符串键 -> 任意值" 的映射容器。
 *
 * <p>
 * 用途：承载与业务输出无关、但对监控和排障很重要的信息，典型包括：
 * <ul>
 * <li>token 用量（Usage：prompt/completion/total tokens）；</li>
 * <li>限流配额（RateLimit：剩余请求数、重置时间）；</li>
 * <li>模型标识、请求 ID、服务端返回的原始头信息等。</li>
 * </ul>
 *
 * <p>
 * 设计要点：采用 Map 式的松散结构而非固定字段，因为各厂商返回的元数据字段千差万别；
 * 取值方法提供了四种语义，方便按需选择：
 * <ul>
 * <li>{@link #get(String)} —— 缺失返回 null（宽松）；</li>
 * <li>{@link #getRequired(Object)} —— 缺失抛异常（严格，用于必需项）；</li>
 * <li>{@code getOrDefault(key, value)} —— 缺失返回给定默认值；</li>
 * <li>{@code getOrDefault(key, supplier)} —— 缺失时才惰性计算默认值。</li>
 * </ul>
 *
 * <p>
 * 典型实现：{@link AbstractResponseMetadata}（提供通用 toString 等）、
 * {@link MutableResponseMetadata}（可写版本，供各模型实现填充数据）。
 *
 * @author Mark Pollack
 * @since 1.0.0
 */
public interface ResponseMetadata {

	/**
	 * Gets an entry from the context. Returns {@code null} when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @return entry or {@code null} if not present
	 */
	// 【中文】按 key 取值，取不到返回 null。
	// 方法自带泛型 <T> 且由调用方在赋值处推断目标类型，内部会做强制转换，
	// 因此若类型写错会在运行时抛 ClassCastException，使用时需自行保证类型正确。
	@Nullable <T> T get(String key);

	/**
	 * Gets an entry from the context. Throws exception when entry is not present.
	 * @param key key
	 * @param <T> value type
	 * @throws IllegalArgumentException if not present
	 * @return entry
	 */
	// 【中文】严格版取值：key 不存在时直接抛 IllegalArgumentException 而非返回 null。
	// 适用于"该元数据必然存在，缺失即属程序错误"的场景，可实现快速失败（fail-fast）。
	<T> T getRequired(Object key);

	/**
	 * Checks if context contains a key.
	 * @param key key
	 * @return {@code true} when the context contains the entry with the given key
	 */
	// 【中文】判断是否存在指定的 key。可用于在调用 getRequired 前做预检查。
	boolean containsKey(Object key);

	/**
	 * Returns an element or default if not present.
	 * @param key key
	 * @param defaultObject default object to return
	 * @param <T> value type
	 * @return object or default if not present
	 */
	// 【中文】取值，缺失时返回传入的默认对象。注意默认值是"立即求值"的，
	// 即无论 key 是否存在，defaultObject 都会先被构造出来。
	<T> T getOrDefault(Object key, T defaultObject);

	/**
	 * Returns an element or default if not present.
	 * @param key key
	 * @param defaultObjectSupplier supplier for default object to return
	 * @param <T> value type
	 * @return object or default if not present
	 * @since 1.11.0
	 */
	// 【中文】取值的惰性默认值版本（default 方法，子类无需实现）。
	// 与上面重载的区别：只有当 key 缺失时才调用 Supplier 生成默认值，
	// 适合默认值构造成本较高的场景，可避免不必要的开销。
	default <T> T getOrDefault(String key, Supplier<T> defaultObjectSupplier) {
		// 先尝试直接取值
		T value = get(key);
		// 仅在取不到（null）时才触发 supplier 计算，实现惰性求值
		return value != null ? value : defaultObjectSupplier.get();
	}

	// 【中文】返回全部"键值对"集合，便于遍历或整体导出所有元数据。
	Set<Map.Entry<String, Object>> entrySet();

	// 【中文】返回全部 key 的集合，便于查看当前响应携带了哪些元数据项。
	Set<String> keySet();

	/**
	 * Returns {@code true} if this map contains no key-value mappings.
	 * @return {@code true} if this map contains no key-value mappings
	 */
	// 【中文】判断元数据是否为空（不含任何键值对）。
	boolean isEmpty();

}
