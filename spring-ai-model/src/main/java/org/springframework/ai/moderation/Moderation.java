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

package org.springframework.ai.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * The Moderation class represents the result of a moderation process. It contains the
 * moderation ID, model, and a list of moderation results. To create an instance of
 * Moderation, use the Builder class.
 *
 * @author Ahmed Yousri
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
/**
 * 【中文说明】审核结果聚合对象：一次审核调用返回的完整数据。
 *
 * <p>
 * 对应 OpenAI Moderation API 的响应体结构，包含三部分：
 * <ul>
 * <li>{@code id} —— 本次审核请求的唯一标识，便于追溯排查</li>
 * <li>{@code model} —— 实际执行审核的模型名称</li>
 * <li>{@code results} —— 审核结果列表，每个元素是一条
 * {@link ModerationResult}（含是否违规、命中类别、各类别分数）</li>
 * </ul>
 *
 * <p>
 * 设计特点：类声明为 {@code final}、字段全部 {@code final}、构造器私有，是一个<b>不可变值对象</b>，
 * 只能通过 {@link #builder()} 创建，天然线程安全。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * Moderation m = Moderation.builder().id("modr-123").model("text-moderation-007").results(list).build();
 * boolean bad = m.getResults().get(0).isFlagged();
 * }</pre>
 */
public final class Moderation {

	// 中文：本次审核请求的唯一 ID
	private final String id;

	// 中文：实际使用的审核模型名称
	private final String model;

	// 中文：审核结果列表，通常与输入条数一一对应
	private final List<ModerationResult> results;

	// 中文：私有构造器，只接受 Builder，外部无法直接 new
	private Moderation(Builder builder) {
		// 中文：必填校验——id 与 model 缺一不可。使用 Assert.state 在构建时快速失败，
		// 避免产生「半成品」对象流入后续逻辑（校验通过后编译器也能确认其非空）
		Assert.state(builder.id != null, "id is required");
		Assert.state(builder.model != null, "model is required");
		this.id = builder.id;
		this.model = builder.model;
		// 中文：注意此处直接引用 Builder 的列表，未做防御性拷贝，
		// 因此 build() 之后若外部仍持有并修改该列表，会影响本对象
		this.results = builder.moderationResultList;
	}

	// 中文：创建建造器的静态入口
	public static Builder builder() {
		return new Builder();
	}

	// 中文：获取审核请求 ID
	public String getId() {
		return this.id;
	}

	// 中文：获取审核模型名称
	public String getModel() {
		return this.model;
	}

	// 中文：获取审核结果列表
	public List<ModerationResult> getResults() {
		return this.results;
	}

	@Override
	// 中文：调试输出。此处先把 List 转成数组再用 Arrays.toString，
	// 效果与直接打印 List 类似，属于写法习惯
	public String toString() {
		return "Moderation{" + "id='" + this.id + '\'' + ", model='" + this.model + '\'' + ", results="
				+ Arrays.toString(this.results.toArray()) + '}';
	}

	@Override
	// 中文：值对象的相等性——三个字段全部相等才视为同一对象
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Moderation that)) {
			return false;
		}
		return Objects.equals(this.id, that.id) && Objects.equals(this.model, that.model)
				&& Objects.equals(this.results, that.results);
	}

	@Override
	// 中文：与 equals 使用相同字段，满足 equals/hashCode 契约
	public int hashCode() {
		return Objects.hash(this.id, this.model, this.results);
	}

	/**
	 * 【中文说明】{@link Moderation} 的建造器。
	 *
	 * <p>
	 * 字段在此处均可为 null，真正的必填校验推迟到 {@link #build()} 触发的私有构造器中执行，
	 * 这样调用方可以按任意顺序链式赋值。
	 */
	public static final class Builder {

		// 中文：暂存字段，标注 @Nullable 表示构建过程中允许尚未赋值
		private @Nullable String id;

		private @Nullable String model;

		// 中文：结果列表默认初始化为空列表，因此它不是必填项
		private List<ModerationResult> moderationResultList = new ArrayList<>();

		// 中文：设置审核请求 ID（必填）
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		// 中文：设置审核模型名称（必填）
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		// 中文：设置审核结果列表（可选，默认空列表）
		public Builder results(List<ModerationResult> results) {
			this.moderationResultList = results;
			return this;
		}

		// 中文：构建不可变实例，必填校验在构造器内完成，缺失时抛 IllegalStateException
		public Moderation build() {
			return new Moderation(this);
		}

	}

}
