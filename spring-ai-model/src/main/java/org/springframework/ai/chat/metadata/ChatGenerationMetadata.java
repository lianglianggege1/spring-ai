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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ResultMetadata;

/**
 *
 * Represents the metadata associated with the generation of a chat response.
 *
 * @author John Blum
 * @author Christian Tzolov
 * @since 0.7.0
 */
/**
 * 【中文说明】单条生成结果（{@code Generation}）的元数据接口。
 *
 * <p>
 * 用途：模型返回的每一个候选回复除了正文之外，还附带一些"附加信息"，本接口就是它们的载体。
 * 注意区分粒度——本接口描述的是**单条候选结果**，而 {@link ChatResponseMetadata} 描述的是
 * **整次响应**（含 token 用量、限流信息等）。
 *
 * <p>
 * 关键内容：
 * <ul>
 * <li>{@code finishReason}——结束原因，如 {@code STOP}（正常结束）、{@code LENGTH}（达到最大长度被截断）、
 * {@code TOOL_CALLS}（模型请求调用工具）、{@code CONTENT_FILTER}（被内容审核拦截），
 * 是判断回复是否完整的重要依据；</li>
 * <li>{@code contentFilters}——命中的内容过滤/审核标记集合；</li>
 * <li>其余以键值对形式存放的厂商自定义字段，通过 {@link #get(String)} 等 Map 风格方法访问。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code generation.getMetadata().getFinishReason()}；
 * 构造则用 {@code ChatGenerationMetadata.builder().finishReason("STOP").build()}。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum、Christian Tzolov；@since 0.7.0。
 */
public interface ChatGenerationMetadata extends ResultMetadata {

	// 【中文】空对象（Null Object 模式）：一个不含任何信息的元数据实例。
	// 当模型响应没有元数据时返回它而不是 null，调用方就无需到处判空，可直接安全调用其方法。
	ChatGenerationMetadata NULL = builder().build();

	/**
	 * Get the {@link String reason} this choice completed for the generation.
	 * @return the {@link String reason} this choice completed for the generation.
	 */
	// 【中文】获取本次生成的结束原因；返回值标注了 @Nullable，表示厂商未提供时可能为 null，取用前需判空。
	@Nullable String getFinishReason();

	// 【中文】获取命中的内容过滤（安全审核）标识集合，例如涉及暴力、仇恨等类别的标记。
	Set<String> getContentFilters();

	// 【中文】按 key 读取任意自定义元数据；泛型 <T> 由调用方指定返回类型，内部做强制类型转换，
	// 因此类型写错会抛 ClassCastException。key 不存在时返回 null（故标注 @Nullable）。
	<T> @Nullable T get(String key);

	// 【中文】判断是否存在指定 key 的元数据。
	boolean containsKey(String key);

	// 【中文】按 key 读取元数据，不存在时返回给定的默认值（避免手动判空）。
	<T> T getOrDefault(String key, T defaultObject);

	// 【中文】以 Map.Entry 集合的形式返回全部键值对，便于遍历打印所有元数据。
	Set<Entry<String, Object>> entrySet();

	// 【中文】返回全部元数据的 key 集合。
	Set<String> keySet();

	// 【中文】判断元数据是否为空（常量 NULL 对该方法返回 true）。
	boolean isEmpty();

	// 【中文】静态工厂方法：返回默认 Builder 实现。
	// 接口暴露 builder()、实现类隐藏在包内，这是 Spring AI 中反复出现的设计——调用方只依赖接口。
	static Builder builder() {
		return new DefaultChatGenerationMetadataBuilder();
	}

	/**
	 * @author Christian Tzolov
	 * @since 1.0.0
	 */
	/**
	 * 【中文说明】{@link ChatGenerationMetadata} 的建造者接口（Builder 模式）。
	 *
	 * <p>
	 * 所有配置方法都返回 {@code Builder} 自身以支持链式调用，最后由 {@link #build()} 生成不可变实例。
	 * 主要供各厂商的模型适配层在解析 API 响应时使用，业务代码一般只读不建。
	 *
	 * <p>
	 * 对应英文 javadoc 中的标签：@author Christian Tzolov；@since 1.0.0。
	 */
	public interface Builder {

		/**
		 * Set the reason this choice completed for the generation.
		 */
		// 【中文】设置结束原因；参数允许为 null（标注了 @Nullable），适配未返回该字段的厂商。
		Builder finishReason(@Nullable String finishReason);

		/**
		 * Add metadata to the Generation result.
		 */
		// 【中文】追加单条自定义元数据（键值对形式）。
		<T> Builder metadata(String key, T value);

		/**
		 * Add metadata to the Generation result.
		 */
		// 【中文】方法重载：一次性批量追加多条元数据（合并进已有的 Map，而非整体替换）。
		Builder metadata(Map<String, Object> metadata);

		/**
		 * Add content filter to the Generation result.
		 */
		// 【中文】追加单个内容过滤标记。
		Builder contentFilter(String contentFilter);

		/**
		 * Add content filters to the Generation result.
		 */
		// 【中文】方法重载：批量追加内容过滤标记。
		Builder contentFilters(Set<String> contentFilters);

		/**
		 * Build the Generation metadata.
		 */
		// 【中文】完成构建，返回不可变的元数据实例。
		ChatGenerationMetadata build();

	}

}
