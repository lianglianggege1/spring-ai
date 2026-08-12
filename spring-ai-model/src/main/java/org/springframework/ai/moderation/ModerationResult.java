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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Represents the result of a moderation process, indicating whether content was flagged,
 * the categories of moderation, and detailed scores for each category. This class is
 * designed to be constructed via its Builder inner class.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】单条审核结果：针对一段文本给出的违规判定详情。
 *
 * <p>
 * 三个字段构成「由粗到细」的三层信息：
 * <ul>
 * <li>{@code flagged} —— 总开关，true 表示该内容整体被判定为违规，是业务代码最常用的字段</li>
 * <li>{@code categories} —— 各违规类别的布尔判定（是否命中仇恨、暴力、色情等）</li>
 * <li>{@code categoryScores} —— 各违规类别的置信度分数（0~1 的 double），
 * 需要自定义阈值时用它</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * if (result.isFlagged()) {
 * 	// 拦截并提示
 * }
 * }</pre>
 *
 * <p>
 * 注意：类虽为 {@code final} 且构造器私有（须经 Builder 创建），但字段并非 final 且提供了
 * setter，因此它<b>不是严格的不可变对象</b>——这与同包的 {@code Moderation} 不同。
 */
public final class ModerationResult {

	// 中文：是否被判定为违规的总结论
	private boolean flagged;

	// 中文：各违规类别的命中情况（布尔），可为 null
	private @Nullable Categories categories;

	// 中文：各违规类别的置信度分数，可为 null
	private @Nullable CategoryScores categoryScores;

	// 中文：私有构造器，从 Builder 逐字段拷贝；此处无必填校验，三项均可缺省
	private ModerationResult(Builder builder) {
		this.flagged = builder.flagged;
		this.categories = builder.categories;
		this.categoryScores = builder.categoryScores;
	}

	// 中文：创建建造器的静态入口
	public static Builder builder() {
		return new Builder();
	}

	// 中文：读取违规总结论（boolean 类型的 getter 惯例用 isXxx 命名）
	public boolean isFlagged() {
		return this.flagged;
	}

	// 中文：修改违规总结论
	public void setFlagged(boolean flagged) {
		this.flagged = flagged;
	}

	// 中文：读取类别命中详情，可能为 null，使用前需判空
	public @Nullable Categories getCategories() {
		return this.categories;
	}

	// 中文：设置类别命中详情
	public void setCategories(Categories categories) {
		this.categories = categories;
	}

	// 中文：读取类别分数详情，可能为 null，使用前需判空
	public @Nullable CategoryScores getCategoryScores() {
		return this.categoryScores;
	}

	// 中文：设置类别分数详情
	public void setCategoryScores(CategoryScores categoryScores) {
		this.categoryScores = categoryScores;
	}

	@Override
	// 中文：值相等比较。flagged 是基本类型故用 ==，另两个对象字段用可空安全的 Objects.equals
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModerationResult that)) {
			return false;
		}
		return this.flagged == that.flagged && Objects.equals(this.categories, that.categories)
				&& Objects.equals(this.categoryScores, that.categoryScores);
	}

	@Override
	// 中文：与 equals 保持字段一致
	public int hashCode() {
		return Objects.hash(this.flagged, this.categories, this.categoryScores);
	}

	@Override
	// 中文：调试输出，一次性展示总结论与两类明细
	public String toString() {
		return "ModerationResult{" + "flagged=" + this.flagged + ", categories=" + this.categories + ", categoryScores="
				+ this.categoryScores + '}';
	}

	/**
	 * 【中文说明】{@link ModerationResult} 的建造器，三个字段均为可选。
	 */
	public static final class Builder {

		// 中文：boolean 默认 false，即默认「未违规」
		private boolean flagged;

		private @Nullable Categories categories;

		private @Nullable CategoryScores categoryScores;

		// 中文：设置违规总结论
		public Builder flagged(boolean flagged) {
			this.flagged = flagged;
			return this;
		}

		// 中文：设置各类别命中情况
		public Builder categories(Categories categories) {
			this.categories = categories;
			return this;
		}

		// 中文：设置各类别置信度分数
		public Builder categoryScores(CategoryScores categoryScores) {
			this.categoryScores = categoryScores;
			return this;
		}

		// 中文：构建实例
		public ModerationResult build() {
			return new ModerationResult(this);
		}

	}

}
