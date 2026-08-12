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

package org.springframework.ai.chat.metadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ChatGenerationMetadata}.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ChatGenerationMetadata} 的默认实现类。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code metadata}——存放任意厂商自定义键值对；</li>
 * <li>{@code finishReason}——生成结束原因，可能为 null；</li>
 * <li>{@code contentFilters}——命中的内容审核标记集合。</li>
 * </ul>
 * 三个字段均为 final，且所有对外暴露集合的方法都返回 {@code Collections.unmodifiableXxx} 包装，
 * 从而保证该对象**事实上不可变**、可安全地在多线程间共享。
 *
 * <p>
 * 典型用法：不直接 new（构造器是包级私有），统一由
 * {@link DefaultChatGenerationMetadataBuilder} 构建。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Christian Tzolov；@since 1.0.0。
 */
public class DefaultChatGenerationMetadata implements ChatGenerationMetadata {

	// 【中文】自定义元数据键值对容器。
	private final Map<String, Object> metadata;

	// 【中文】结束原因；标注 @Nullable 表示允许为 null（厂商未返回时）。
	private final @Nullable String finishReason;

	// 【中文】内容审核标记集合。
	private final Set<String> contentFilters;

	/**
	 * Create a new {@link DefaultChatGenerationMetadata} instance.
	 * @param metadata the metadata map, must not be null
	 * @param finishReason the finish reason, may be null
	 * @param contentFilters the content filters, must not be null
	 * @throws IllegalArgumentException if metadata or contentFilters is null
	 */
	// 【中文】构造器为包级私有（无 public 修饰）：限制只能由同包下的 Builder 创建实例，
	// 从而强制外部走 ChatGenerationMetadata.builder() 这一统一入口。
	DefaultChatGenerationMetadata(Map<String, Object> metadata, @Nullable String finishReason,
			Set<String> contentFilters) {
		// 【中文】参数校验：metadata 与 contentFilters 不允许为 null（可以是空集合），
		// 否则后续每个方法都要判空。finishReason 则刻意不校验，因为它本就允许为 null。
		Assert.notNull(metadata, "Metadata must not be null");
		Assert.notNull(contentFilters, "Content filters must not be null");
		this.metadata = metadata;
		this.finishReason = finishReason;
		// 【中文】此处对 contentFilters 做了防御性拷贝（new HashSet<>），避免外部后续修改原集合影响本对象；
		// 注意 metadata 是直接引用赋值、未做拷贝——因为它由包内 Builder 独占创建，不会被外部持有。
		this.contentFilters = new HashSet<>(contentFilters);
	}

	// 【中文】按 key 取值并强转为调用方期望的类型 T。
	@Override
	public <T> @Nullable T get(String key) {
		// 【中文】未检查的类型转换（unchecked cast）：编译期无法校验，类型不匹配时会在运行时抛 ClassCastException。
		// 这是"以 Map 承载异构数据"的常见取舍——牺牲类型安全换取灵活性。
		return (T) this.metadata.get(key);
	}

	// 【中文】判断 key 是否存在。
	@Override
	public boolean containsKey(String key) {
		return this.metadata.containsKey(key);
	}

	// 【中文】取值，为 null（含 key 不存在）时返回默认值。
	@Override
	public <T> T getOrDefault(String key, T defaultObject) {
		T value = get(key);
		return value != null ? value : defaultObject;
	}

	// 【中文】返回键值对集合的**只读视图**：外部若尝试增删会抛 UnsupportedOperationException，
	// 以此保护内部状态不被篡改（下面 keySet、getContentFilters 同理）。
	@Override
	public Set<Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(this.metadata.entrySet());
	}

	// 【中文】返回所有 key 的只读视图。
	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.metadata.keySet());
	}

	// 【中文】判断自定义元数据是否为空（注意只看 metadata，不含 finishReason 和过滤标记）。
	@Override
	public boolean isEmpty() {
		return this.metadata.isEmpty();
	}

	// 【中文】返回生成结束原因，可能为 null。
	@Override
	public @Nullable String getFinishReason() {
		return this.finishReason;
	}

	// 【中文】返回内容审核标记的只读视图。
	@Override
	public Set<String> getContentFilters() {
		return Collections.unmodifiableSet(this.contentFilters);
	}

	// 【中文】哈希值由三个字段共同决定，与下面的 equals 保持一致。
	@Override
	public int hashCode() {
		return Objects.hash(this.metadata, this.finishReason, this.contentFilters);
	}

	// 【中文】值相等性判断。
	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		// 【中文】这里用 getClass() != obj.getClass() 而非 instanceof：要求两者是**完全相同**的类，
		// 子类实例与父类实例永不相等，相等关系更严格（对称性更容易保证）。
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DefaultChatGenerationMetadata other = (DefaultChatGenerationMetadata) obj;
		return Objects.equals(this.metadata, other.metadata) && Objects.equals(this.finishReason, other.finishReason)
				&& Objects.equals(this.contentFilters, other.contentFilters);
	}

	// 【中文】调试输出：只打印结束原因和两个集合的"条数"而非全部内容，防止日志过长。
	@Override
	public String toString() {
		return String.format("DefaultChatGenerationMetadata[finishReason='%s', filters=%d, metadata=%d]",
				this.finishReason, this.contentFilters.size(), this.metadata.size());
	}

}
