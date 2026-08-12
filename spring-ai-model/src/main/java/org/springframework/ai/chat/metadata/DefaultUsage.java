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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of the {@link Usage} interface.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link Usage} 接口的默认实现，是各厂商适配层最常用的用量载体。
 *
 * <p>
 * 关键字段：{@code promptTokens}（输入）、{@code completionTokens}（输出）、
 * {@code totalTokens}（合计）、{@code nativeUsage}（厂商原始用量对象），
 * 以及 2.0.0 新增的两个 prompt 缓存指标 {@code cacheReadInputTokens} / {@code cacheWriteInputTokens}。
 * 全部为 final，对象不可变。
 *
 * <p>
 * 设计要点：
 * <ul>
 * <li>提供 4 个重载构造器，通过 {@code this(...)} 层层委托到"全参构造器"，避免逻辑重复
 * （即 telescoping constructor / 构造器链）；</li>
 * <li>入参允许为 null，内部统一兜底为 0；{@code totalTokens} 传 null 时自动由输入+输出算出；</li>
 * <li>类上的 Jackson 注解（{@code @JsonPropertyOrder}、{@code @JsonProperty}、{@code @JsonCreator}）
 * 用于支持序列化/反序列化，字段顺序固定便于阅读，{@code @JsonInclude(NON_NULL)} 让 null 字段不出现在 JSON 中。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code new DefaultUsage(promptTokens, completionTokens)}。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Mark Pollack、Ilayaperumal Gopinathan；@since 1.0.0。
 */
@JsonPropertyOrder({ "promptTokens", "completionTokens", "totalTokens", "cacheReadInputTokens", "cacheWriteInputTokens",
		"nativeUsage" })
public class DefaultUsage implements Usage {

	// 【中文】输入（提示词）token 数，构造时已把 null 归一为 0，故对外保证非 null。
	private final Integer promptTokens;

	// 【中文】输出（生成内容）token 数，同样已做 null 归一。
	private final Integer completionTokens;

	// 【中文】总 token 数。注意这里用的是基本类型 int 而非 Integer——因为构造器保证它一定有值。
	private final int totalTokens;

	// 【中文】厂商原始用量对象，可为 null。
	private final @Nullable Object nativeUsage;

	// 【中文】prompt 缓存命中读取的 token 数，可为 null（表示厂商不支持或未命中）。
	private final @Nullable Long cacheReadInputTokens;

	// 【中文】写入 prompt 缓存的 token 数，可为 null。
	private final @Nullable Long cacheWriteInputTokens;

	/**
	 * Create a new DefaultUsage with promptTokens, completionTokens, totalTokens and
	 * native {@link Usage} object.
	 * @param promptTokens the number of tokens in the prompt, or {@code null} if not
	 * available
	 * @param completionTokens the number of tokens in the generation, or {@code null} if
	 * not available
	 * @param totalTokens the total number of tokens, or {@code null} to calculate from
	 * promptTokens and completionTokens
	 * @param nativeUsage the native usage object returned by the model provider, or
	 * {@code null} to return the map of prompt, completion and total tokens.
	 */
	// 【中文】四参构造器：不关心缓存指标时使用，内部把两个缓存参数传 null 后委托给全参构造器。
	public DefaultUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens,
			@Nullable Integer totalTokens, @Nullable Object nativeUsage) {
		this(promptTokens, completionTokens, totalTokens, nativeUsage, null, null);
	}

	/**
	 * Create a new DefaultUsage with all fields including prompt cache metrics.
	 * @param promptTokens the number of tokens in the prompt, or {@code null} if not
	 * available
	 * @param completionTokens the number of tokens in the generation, or {@code null} if
	 * not available
	 * @param totalTokens the total number of tokens, or {@code null} to calculate from
	 * promptTokens and completionTokens
	 * @param nativeUsage the native usage object returned by the model provider, or
	 * {@code null} to return the map of prompt, completion and total tokens.
	 * @param cacheReadInputTokens the number of input tokens read from prompt cache, or
	 * {@code null} if not available
	 * @param cacheWriteInputTokens the number of input tokens written to prompt cache, or
	 * {@code null} if not available
	 * @since 2.0.0
	 */
	// 【中文】全参构造器（其余构造器最终都委托到这里），是唯一真正执行赋值逻辑的地方。
	public DefaultUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens,
			@Nullable Integer totalTokens, @Nullable Object nativeUsage, @Nullable Long cacheReadInputTokens,
			@Nullable Long cacheWriteInputTokens) {
		// 【中文】空值处理：厂商未返回时统一按 0 处理，保证 getter 永不返回 null。
		this.promptTokens = promptTokens != null ? promptTokens : 0;
		this.completionTokens = completionTokens != null ? completionTokens : 0;
		// 【中文】totalTokens 的兜底策略：厂商给了就用厂商的（有些厂商的总数并不等于简单相加，
		// 例如包含了推理 token），没给才用"输入+输出"自行计算。
		this.totalTokens = totalTokens != null ? totalTokens
				: calculateTotalTokens(this.promptTokens, this.completionTokens);
		this.nativeUsage = nativeUsage;
		// 【中文】缓存相关指标保持原样（允许为 null），以便区分"不支持该特性"与"值为 0"。
		this.cacheReadInputTokens = cacheReadInputTokens;
		this.cacheWriteInputTokens = cacheWriteInputTokens;
	}

	/**
	 * Create a new DefaultUsage with promptTokens and completionTokens.
	 * @param promptTokens the number of tokens in the prompt, or {@code null} if not
	 * available
	 * @param completionTokens the number of tokens in the generation, or {@code null} if
	 * not available
	 */
	// 【中文】最常用的两参构造器：只给输入/输出 token，总数自动计算，无原始对象。
	public DefaultUsage(Integer promptTokens, Integer completionTokens) {
		this(promptTokens, completionTokens, null, null);
	}

	/**
	 * Create a new DefaultUsage with promptTokens, completionTokens, and totalTokens.
	 * @param promptTokens the number of tokens in the prompt, or {@code null} if not
	 * available
	 * @param completionTokens the number of tokens in the generation, or {@code null} if
	 * not available
	 * @param totalTokens the total number of tokens, or {@code null} to calculate from
	 * promptTokens and completionTokens
	 */
	// 【中文】三参构造器：厂商同时返回了总数时使用，避免框架自行相加与厂商口径不一致。
	public DefaultUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
		this(promptTokens, completionTokens, totalTokens, null);
	}

	/**
	 * Create a new DefaultUsage with promptTokens, completionTokens, and totalTokens.
	 * This constructor is used for JSON deserialization and handles both the new format
	 * with completionTokens and the legacy format with generationTokens.
	 * @param promptTokens the number of tokens in the prompt
	 * @param completionTokens the number of tokens in the completion (new format)
	 * @param totalTokens the total number of tokens
	 * @param nativeUsage the native usage object
	 * @return a new DefaultUsage instance
	 */
	// 【中文】@JsonCreator：告诉 Jackson 反序列化时调用这个静态工厂方法来创建对象，
	// 而不是去找无参构造器 + setter（本类字段是 final，没有 setter）。
	// 每个参数上的 @JsonProperty 指明它对应 JSON 中的哪个字段名。
	// 英文 javadoc 提到它还兼容历史格式（旧版字段名为 generationTokens）。
	@JsonCreator
	public static DefaultUsage fromJson(@JsonProperty("promptTokens") Integer promptTokens,
			@JsonProperty("completionTokens") Integer completionTokens,
			@JsonProperty("totalTokens") Integer totalTokens, @JsonProperty("nativeUsage") Object nativeUsage,
			@JsonProperty("cacheReadInputTokens") @Nullable Long cacheReadInputTokens,
			@JsonProperty("cacheWriteInputTokens") @Nullable Long cacheWriteInputTokens) {
		return new DefaultUsage(promptTokens, completionTokens, totalTokens, nativeUsage, cacheReadInputTokens,
				cacheWriteInputTokens);
	}

	// 【中文】返回输入 token 数，保证非 null。
	@Override
	@JsonProperty("promptTokens")
	public Integer getPromptTokens() {
		return this.promptTokens;
	}

	// 【中文】返回输出 token 数，保证非 null。
	@Override
	@JsonProperty("completionTokens")
	public Integer getCompletionTokens() {
		return this.completionTokens;
	}

	// 【中文】重写接口的 default 方法：直接返回构造时算好的字段，而非每次现算。
	// 返回类型是 Integer，此处 int 字段会自动装箱。
	@Override
	@JsonProperty("totalTokens")
	public Integer getTotalTokens() {
		return this.totalTokens;
	}

	// 【中文】返回厂商原始用量对象。
	// @JsonInclude(NON_NULL)：序列化为 JSON 时，若该值为 null 则整个字段不输出，保持 JSON 简洁。
	@Override
	@JsonProperty("nativeUsage")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public @Nullable Object getNativeUsage() {
		return this.nativeUsage;
	}

	// 【中文】返回缓存命中读取的 token 数（可能为 null）。
	@Override
	@JsonProperty("cacheReadInputTokens")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public @Nullable Long getCacheReadInputTokens() {
		return this.cacheReadInputTokens;
	}

	// 【中文】返回写入缓存的 token 数（可能为 null）。
	@Override
	@JsonProperty("cacheWriteInputTokens")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public @Nullable Long getCacheWriteInputTokens() {
		return this.cacheWriteInputTokens;
	}

	// 【中文】总 token 的默认计算方式：输入 + 输出。
	// 由于调用点已保证两个参数非 null，这里无需再判空即可安全拆箱相加。
	private Integer calculateTotalTokens(Integer promptTokens, Integer completionTokens) {
		return promptTokens + completionTokens;
	}

	// 【中文】值相等性判断：六个字段全部相等才算相等。
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DefaultUsage that = (DefaultUsage) o;
		// 【中文】注意 totalTokens 是基本类型 int，用 == 直接比较值；
		// 其余包装类型/对象字段则必须用 Objects.equals 以避免 NPE 和引用比较陷阱。
		return this.totalTokens == that.totalTokens && Objects.equals(this.promptTokens, that.promptTokens)
				&& Objects.equals(this.completionTokens, that.completionTokens)
				&& Objects.equals(this.nativeUsage, that.nativeUsage)
				&& Objects.equals(this.cacheReadInputTokens, that.cacheReadInputTokens)
				&& Objects.equals(this.cacheWriteInputTokens, that.cacheWriteInputTokens);
	}

	// 【中文】手写哈希：经典的 31 倍累乘算法（31 是奇素数，JVM 可优化为移位减法，且能降低哈希冲突）。
	// 使用 Objects.hashCode 而非直接调 hashCode，可安全处理 null 字段（null 返回 0）。
	@Override
	public int hashCode() {
		int result = Objects.hashCode(this.promptTokens);
		result = 31 * result + Objects.hashCode(this.completionTokens);
		result = 31 * result + this.totalTokens;
		result = 31 * result + Objects.hashCode(this.nativeUsage);
		result = 31 * result + Objects.hashCode(this.cacheReadInputTokens);
		result = 31 * result + Objects.hashCode(this.cacheWriteInputTokens);
		return result;
	}

	// 【中文】输出可读的用量摘要。
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DefaultUsage{");
		sb.append("promptTokens=").append(this.promptTokens);
		sb.append(", completionTokens=").append(this.completionTokens);
		sb.append(", totalTokens=").append(this.totalTokens);
		// 【中文】两个缓存指标仅在非 null 时才拼进字符串，避免打印出大量 "=null" 的噪音；
		// 同时 nativeUsage 完全不打印，防止厂商原始对象过大污染日志。
		if (this.cacheReadInputTokens != null) {
			sb.append(", cacheReadInputTokens=").append(this.cacheReadInputTokens);
		}
		if (this.cacheWriteInputTokens != null) {
			sb.append(", cacheWriteInputTokens=").append(this.cacheWriteInputTokens);
		}
		sb.append('}');
		return sb.toString();
	}

}
