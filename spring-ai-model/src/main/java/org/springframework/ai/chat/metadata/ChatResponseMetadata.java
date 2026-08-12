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
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.AbstractResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;
import org.springframework.util.Assert;

/**
 * Models common AI provider metadata returned in an AI response.
 *
 * @author John Blum
 * @author Thomas Vitale
 * @author Mark Pollack
 * @author Alexandros Pappas
 * @since 1.0.0
 */
/**
 * 【中文说明】**整次**聊天响应的元数据，汇总各家 AI 厂商响应中通用的附加信息。
 *
 * <p>
 * 与 {@link ChatGenerationMetadata} 的粒度区别：后者描述单条候选回复（如结束原因），
 * 本类描述整次调用（一次调用可能返回多条候选）。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code id}——本次请求的唯一标识，排查问题时可拿它找厂商对账；</li>
 * <li>{@code model}——实际处理请求的模型名（可能与请求指定的不同，例如别名被解析成具体版本）；</li>
 * <li>{@code rateLimit}——限流信息（剩余配额、重置时间），默认 {@link EmptyRateLimit}；</li>
 * <li>{@code usage}——token 用量，默认 {@link EmptyUsage}；</li>
 * <li>{@code promptMetadata}——提示词层面的元数据（如内容审核结果），默认空实例。</li>
 * </ul>
 * 所有引用类型字段都用"空对象"作为默认值而非 null，调用方因此可以放心链式调用，无需层层判空。
 *
 * <p>
 * 本类继承 {@code AbstractResponseMetadata}，从而额外具备 Map 风格的任意键值对存取能力
 * （父类中的 {@code map} 字段），用于承载厂商特有字段。
 *
 * <p>
 * 典型用法：{@code chatResponse.getMetadata().getUsage().getTotalTokens()}。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum、Thomas Vitale、Mark Pollack、Alexandros Pappas；@since 1.0.0。
 */
public class ChatResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {

	// 【中文】日志器，仅用于 Builder 中记录"忽略 null 值"的调试信息。
	private static final Log logger = LogFactory.getLog(ChatResponseMetadata.class);

	// 【中文】请求唯一标识；初始化为空字符串而非 null（原因见右侧英文注释：保持向后兼容）。
	private String id = ""; // Set to blank to preserve backward compat with previous

	// interface default methods

	// 【中文】实际处理请求的模型名称，同样默认空字符串。
	private String model = "";

	// 【中文】限流元数据，默认使用空对象 EmptyRateLimit，避免调用方拿到 null。
	private RateLimit rateLimit = new EmptyRateLimit();

	// 【中文】token 用量元数据，默认使用空对象 EmptyUsage（各项均为 0）。
	private Usage usage = new EmptyUsage();

	// 【中文】提示词元数据（如各条提示的审核过滤结果），默认为空实例。
	private PromptMetadata promptMetadata = PromptMetadata.empty();

	// 【中文】获取 Builder 的静态工厂方法。
	// 注意本类的构造器是默认的包外可见构造（未显式声明），字段则为 private，
	// 因此外部只能通过 Builder 设置字段值。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A unique identifier for the chat completion operation.
	 * @return unique operation identifier.
	 */
	// 【中文】获取本次聊天补全操作的唯一 id（厂商返回，未提供时为空字符串）。
	public String getId() {
		return this.id;
	}

	/**
	 * The model that handled the request.
	 * @return the model that handled the request.
	 */
	// 【中文】获取真正处理本次请求的模型名称，便于确认线上实际使用的模型版本。
	public String getModel() {
		return this.model;
	}

	/**
	 * Returns AI provider specific metadata on rate limits.
	 * @return AI provider specific metadata on rate limits.
	 * @see RateLimit
	 */
	// 【中文】获取限流元数据（剩余请求数/token 数及配额重置时间），可用于实现客户端侧的退避重试。
	public RateLimit getRateLimit() {
		return this.rateLimit;
	}

	/**
	 * Returns AI provider specific metadata on API usage.
	 * @return AI provider specific metadata on API usage.
	 * @see Usage
	 */
	// 【中文】获取 token 用量元数据，最常用的元数据之一（成本统计、配额监控）。
	public Usage getUsage() {
		return this.usage;
	}

	/**
	 * Returns the prompt metadata gathered by the AI during request processing.
	 * @return the prompt metadata.
	 */
	// 【中文】获取提示词相关的元数据（例如各条输入提示是否被内容审核系统标记）。
	public PromptMetadata getPromptMetadata() {
		return this.promptMetadata;
	}

	// 【中文】值相等性判断：五个核心字段全部相等才视为同一对象。
	@Override
	public boolean equals(@Nullable Object o) {
		// 【中文】先比较引用，同一对象直接短路返回 true（性能优化）。
		if (this == o) {
			return true;
		}
		// 【中文】instanceof 模式匹配（Java 16+）：类型判断与变量绑定一步完成，
		// 顺带覆盖了 o == null 的情况（null instanceof 恒为 false）。
		if (!(o instanceof ChatResponseMetadata that)) {
			return false;
		}
		// 【中文】用 Objects.equals 逐字段比较，可安全处理字段为 null 的情况。
		return Objects.equals(this.id, that.id) && Objects.equals(this.model, that.model)
				&& Objects.equals(this.rateLimit, that.rateLimit) && Objects.equals(this.usage, that.usage)
				&& Objects.equals(this.promptMetadata, that.promptMetadata);
	}

	// 【中文】与 equals 保持一致：使用相同的字段集合计算哈希值（重写 equals 必须同时重写 hashCode）。
	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.model, this.rateLimit, this.usage, this.promptMetadata);
	}

	// 【中文】格式化输出，复用父类定义的 AI_METADATA_STRING 模板，仅展示 id、用量、限流三项关键信息。
	@Override
	public String toString() {
		return AI_METADATA_STRING.formatted(getId(), getUsage(), getRateLimit());
	}

	/**
	 * 【中文说明】{@link ChatResponseMetadata} 的建造者。
	 *
	 * <p>
	 * 实现方式较特别：内部先 new 出一个待填充的 {@code ChatResponseMetadata} 实例，
	 * 各配置方法直接修改它的私有字段（内部类可访问外部类的私有成员），{@code build()} 直接把它返回。
	 * 好处是代码简洁，代价是构建出的对象并非严格不可变，且同一个 Builder 多次 build() 会返回同一实例。
	 */
	public static final class Builder {

		// 【中文】待构建（边构建边填充）的目标对象。
		private final ChatResponseMetadata chatResponseMetadata;

		// 【中文】构造器为 public，因此除了 ChatResponseMetadata.builder() 之外也可直接 new Builder()。
		public Builder() {
			this.chatResponseMetadata = new ChatResponseMetadata();
		}

		// 【中文】批量拷贝自定义键值对元数据到父类的 map 中（putAll 是合并语义，不会清空已有内容）。
		public Builder metadata(Map<String, Object> mapToCopy) {
			this.chatResponseMetadata.map.putAll(mapToCopy);
			return this;
		}

		// 【中文】添加单条自定义键值对元数据。
		public Builder keyValue(String key, @Nullable Object value) {
			Assert.notNull(key, "Key must not be null"); // Defensive check
			// 【中文】空值处理：value 为 null 时**不写入** map，只打一条 debug 日志。
			// 这样可以避免 map 中出现大量无意义的 null 值，同时又不打断链式调用（不抛异常）。
			if (value != null) {
				this.chatResponseMetadata.map.put(key, value);
			}
			else {
				// 【中文】先判断日志级别再拼接字符串，避免 debug 未开启时做无谓的字符串拼接开销。
				if (logger.isDebugEnabled()) {
					logger.debug("Ignore null value for key [" + key + "]");
				}
			}
			return this;
		}

		// 【中文】设置请求唯一 id。
		public Builder id(String id) {
			this.chatResponseMetadata.id = id;
			return this;
		}

		// 【中文】设置实际处理请求的模型名称。
		public Builder model(String model) {
			this.chatResponseMetadata.model = model;
			return this;
		}

		// 【中文】设置限流元数据。
		public Builder rateLimit(RateLimit rateLimit) {
			this.chatResponseMetadata.rateLimit = rateLimit;
			return this;
		}

		// 【中文】设置 token 用量元数据。
		public Builder usage(Usage usage) {
			this.chatResponseMetadata.usage = usage;
			return this;
		}

		// 【中文】设置提示词元数据。
		public Builder promptMetadata(PromptMetadata promptMetadata) {
			this.chatResponseMetadata.promptMetadata = promptMetadata;
			return this;
		}

		// 【中文】完成构建：直接返回内部持有的实例（未做防御性拷贝，注意别复用同一个 Builder 构建多个对象）。
		public ChatResponseMetadata build() {
			return this.chatResponseMetadata;
		}

	}

}
