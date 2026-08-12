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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.metadata.ChatGenerationMetadata.Builder;

/**
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ChatGenerationMetadata.Builder} 的默认实现，用于构建
 * {@link DefaultChatGenerationMetadata}。
 *
 * <p>
 * 三个可变字段在声明时即初始化好容器（{@code HashMap}/{@code HashSet}），
 * 因此调用方可任意顺序、任意次数地追加数据；每个方法都返回 {@code this} 支持链式调用。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ChatGenerationMetadata md = ChatGenerationMetadata.builder()
 *         .finishReason("STOP")
 *         .metadata("logprobs", logprobs)
 *         .build();
 * }</pre>
 *
 * <p>
 * 注意：Builder 本身**非线程安全**，应在单线程内完成构建。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Christian Tzolov；@since 1.0.0。
 */

public class DefaultChatGenerationMetadataBuilder implements Builder {

	// 【中文】结束原因，可选项，未设置时为 null。
	private @Nullable String finishReason;

	// 【中文】自定义元数据容器，声明即初始化，避免使用前判空。
	private final Map<String, Object> metadata = new HashMap<>();

	// 【中文】内容审核标记容器；用 Set 可自动去重。
	private final Set<String> contentFilters = new HashSet<>();

	// 【中文】包级私有构造器：外部只能通过 ChatGenerationMetadata.builder() 获得实例。
	DefaultChatGenerationMetadataBuilder() {
	}

	// 【中文】设置结束原因（覆盖语义，多次调用以最后一次为准）。
	@Override
	public Builder finishReason(@Nullable String finishReason) {
		this.finishReason = finishReason;
		return this;
	}

	// 【中文】追加单条元数据；key 重复时后写入的值覆盖先前的值。
	@Override
	public <T> Builder metadata(String key, T value) {
		this.metadata.put(key, value);
		return this;
	}

	// 【中文】重载版本：批量追加（putAll 为合并语义，不会清空已有内容）。
	@Override
	public Builder metadata(Map<String, Object> metadata) {
		this.metadata.putAll(metadata);
		return this;
	}

	// 【中文】追加单个内容审核标记。
	@Override
	public Builder contentFilter(String contentFilter) {
		this.contentFilters.add(contentFilter);
		return this;
	}

	// 【中文】重载版本：批量追加内容审核标记（addAll 同样是合并语义）。
	@Override
	public Builder contentFilters(Set<String> contentFilters) {
		this.contentFilters.addAll(contentFilters);
		return this;
	}

	// 【中文】完成构建：把三个字段交给包级私有构造器生成不可变的元数据对象。
	// 返回类型声明为接口 ChatGenerationMetadata 而非具体实现类，便于后续替换实现。
	@Override
	public ChatGenerationMetadata build() {
		return new DefaultChatGenerationMetadata(this.metadata, this.finishReason, this.contentFilters);
	}

}
