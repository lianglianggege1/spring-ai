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
 * The Categories class represents a set of categories used to classify content. Each
 * category can be either true (indicating that the content belongs to the category) or
 * false (indicating that the content does not belong to the category).
 *
 * @author Ahmed Yousri
 * @author Ilayaperumal Gopinathan
 * @author Ricken Bazolo
 * @since 1.0.0
 */
/**
 * 【中文说明】违规类别命中表：以布尔开关的形式，逐项标明内容命中了哪些违规类别。
 *
 * <p>
 * 每个字段对应一个审核维度，{@code true} 表示内容属于该类别，{@code false} 表示不属于。
 * 它与 {@link CategoryScores} 字段一一对应——本类给的是「是否命中」的结论，
 * 后者给的是「命中程度」的分数。
 *
 * <p>
 * 字段大致可分为三组：
 * <ul>
 * <li><b>OpenAI 标准类别</b>：sexual、hate、harassment、selfHarm、violence 及其
 * 更细分的变体（如 sexualMinors、hateThreatening、violenceGraphic、selfHarmIntent、
 * selfHarmInstructions、harassmentThreatening）</li>
 * <li><b>其他厂商扩展</b>：dangerousAndCriminalContent（危险与犯罪内容，Mistral 等）</li>
 * <li><b>专业领域提示</b>：health（医疗）、financial（金融）、law（法律）、pii（个人隐私信息），
 * 这类并非「有害」，而是提示内容涉及需要谨慎处理的专业或敏感领域</li>
 * </ul>
 *
 * <p>
 * 设计特点：{@code final} 类 + 全 {@code final} 字段 + 私有构造器，是<b>不可变值对象</b>，
 * 只能经 {@link #builder()} 创建。字段众多且类型相同，正是采用 Builder 模式的典型理由——
 * 若用构造器传 16 个 boolean，调用方极易错位。
 */
public final class Categories {

	// 中文：以下 16 个字段均为「是否命中该违规类别」的布尔标记，默认 false。

	// 中文：色情内容
	private final boolean sexual;

	// 中文：仇恨言论
	private final boolean hate;

	// 中文：骚扰内容
	private final boolean harassment;

	// 中文：自我伤害相关内容
	private final boolean selfHarm;

	// 中文：涉及未成年人的色情内容（最严重的类别之一）
	private final boolean sexualMinors;

	// 中文：带威胁性质的仇恨言论
	private final boolean hateThreatening;

	// 中文：血腥、图像化的暴力描写
	private final boolean violenceGraphic;

	// 中文：表达自我伤害意图
	private final boolean selfHarmIntent;

	// 中文：提供自我伤害的具体方法或教程
	private final boolean selfHarmInstructions;

	// 中文：带威胁性质的骚扰
	private final boolean harassmentThreatening;

	// 中文：暴力内容
	private final boolean violence;

	// 中文：危险及犯罪相关内容（部分厂商的扩展类别）
	private final boolean dangerousAndCriminalContent;

	// 中文：涉及医疗健康建议的内容
	private final boolean health;

	// 中文：涉及金融投资建议的内容
	private final boolean financial;

	// 中文：涉及法律建议的内容
	private final boolean law;

	// 中文：涉及个人身份隐私信息（Personally Identifiable Information）
	private final boolean pii;

	// 中文：私有构造器，从 Builder 逐字段拷贝，保证外部只能通过 Builder 创建
	private Categories(Builder builder) {
		this.sexual = builder.sexual;
		this.hate = builder.hate;
		this.harassment = builder.harassment;
		this.selfHarm = builder.selfHarm;
		this.sexualMinors = builder.sexualMinors;
		this.hateThreatening = builder.hateThreatening;
		this.violenceGraphic = builder.violenceGraphic;
		this.selfHarmIntent = builder.selfHarmIntent;
		this.selfHarmInstructions = builder.selfHarmInstructions;
		this.harassmentThreatening = builder.harassmentThreatening;
		this.violence = builder.violence;
		this.dangerousAndCriminalContent = builder.dangerousAndCriminalContent;
		this.health = builder.health;
		this.financial = builder.financial;
		this.law = builder.law;
		this.pii = builder.pii;
	}

	// 中文：创建建造器的静态入口
	public static Builder builder() {
		return new Builder();
	}

	// 中文：以下均为各类别的只读访问器（boolean 惯例用 isXxx 命名），语义见对应字段注释
	public boolean isSexual() {
		return this.sexual;
	}

	public boolean isHate() {
		return this.hate;
	}

	public boolean isHarassment() {
		return this.harassment;
	}

	public boolean isSelfHarm() {
		return this.selfHarm;
	}

	public boolean isSexualMinors() {
		return this.sexualMinors;
	}

	public boolean isHateThreatening() {
		return this.hateThreatening;
	}

	public boolean isViolenceGraphic() {
		return this.violenceGraphic;
	}

	public boolean isSelfHarmIntent() {
		return this.selfHarmIntent;
	}

	public boolean isSelfHarmInstructions() {
		return this.selfHarmInstructions;
	}

	public boolean isHarassmentThreatening() {
		return this.harassmentThreatening;
	}

	public boolean isViolence() {
		return this.violence;
	}

	public boolean isDangerousAndCriminalContent() {
		return this.dangerousAndCriminalContent;
	}

	public boolean isHealth() {
		return this.health;
	}

	public boolean isFinancial() {
		return this.financial;
	}

	public boolean isLaw() {
		return this.law;
	}

	public boolean isPii() {
		return this.pii;
	}

	@Override
	// 中文：值相等比较——16 个 boolean 字段全部相同才判定相等。
	// 因均为基本类型，直接用 == 且短路求值，无需 Objects.equals
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Categories that)) {
			return false;
		}
		return this.sexual == that.sexual && this.hate == that.hate && this.harassment == that.harassment
				&& this.selfHarm == that.selfHarm && this.sexualMinors == that.sexualMinors
				&& this.hateThreatening == that.hateThreatening && this.violenceGraphic == that.violenceGraphic
				&& this.selfHarmIntent == that.selfHarmIntent && this.selfHarmInstructions == that.selfHarmInstructions
				&& this.harassmentThreatening == that.harassmentThreatening && this.violence == that.violence
				&& this.dangerousAndCriminalContent == that.dangerousAndCriminalContent && this.health == that.health
				&& this.financial == that.financial && this.law == that.law && this.pii == that.pii;
	}

	@Override
	// 中文：与 equals 使用完全相同的 16 个字段，保证哈希契约一致（boolean 会被自动装箱）
	public int hashCode() {
		return Objects.hash(this.sexual, this.hate, this.harassment, this.selfHarm, this.sexualMinors,
				this.hateThreatening, this.violenceGraphic, this.selfHarmIntent, this.selfHarmInstructions,
				this.harassmentThreatening, this.violence, this.dangerousAndCriminalContent, this.health,
				this.financial, this.law, this.pii);
	}

	@Override
	// 中文：调试输出，平铺展示全部类别的命中情况
	public String toString() {
		return "Categories{" + "sexual=" + this.sexual + ", hate=" + this.hate + ", harassment=" + this.harassment
				+ ", selfHarm=" + this.selfHarm + ", sexualMinors=" + this.sexualMinors + ", hateThreatening="
				+ this.hateThreatening + ", violenceGraphic=" + this.violenceGraphic + ", selfHarmIntent="
				+ this.selfHarmIntent + ", selfHarmInstructions=" + this.selfHarmInstructions
				+ ", harassmentThreatening=" + this.harassmentThreatening + ", violence=" + this.violence
				+ ", dangerousAndCriminalContent=" + this.dangerousAndCriminalContent + ", health=" + this.health
				+ ", financial=" + this.financial + ", law=" + this.law + ", pii=" + this.pii + '}';
	}

	/**
	 * 【中文说明】{@link Categories} 的建造器。
	 *
	 * <p>
	 * 所有字段均为可选，未显式设置的类别保持 boolean 默认值 {@code false}（即未命中）。
	 * 每个 setter 都返回 {@code this}，支持链式调用，例如：
	 *
	 * <pre>{@code
	 * Categories.builder().hate(true).violence(true).build();
	 * }</pre>
	 */
	public static final class Builder {

		// 中文：与外部类一一对应的暂存字段，默认 false
		private boolean sexual;

		private boolean hate;

		private boolean harassment;

		private boolean selfHarm;

		private boolean sexualMinors;

		private boolean hateThreatening;

		private boolean violenceGraphic;

		private boolean selfHarmIntent;

		private boolean selfHarmInstructions;

		private boolean harassmentThreatening;

		private boolean violence;

		private boolean dangerousAndCriminalContent;

		private boolean health;

		private boolean financial;

		private boolean law;

		private boolean pii;

		// 中文：以下均为链式 setter，逐一设置对应类别是否命中，语义参见外部类同名字段
		public Builder sexual(boolean sexual) {
			this.sexual = sexual;
			return this;
		}

		public Builder hate(boolean hate) {
			this.hate = hate;
			return this;
		}

		public Builder harassment(boolean harassment) {
			this.harassment = harassment;
			return this;
		}

		public Builder selfHarm(boolean selfHarm) {
			this.selfHarm = selfHarm;
			return this;
		}

		public Builder sexualMinors(boolean sexualMinors) {
			this.sexualMinors = sexualMinors;
			return this;
		}

		public Builder hateThreatening(boolean hateThreatening) {
			this.hateThreatening = hateThreatening;
			return this;
		}

		public Builder violenceGraphic(boolean violenceGraphic) {
			this.violenceGraphic = violenceGraphic;
			return this;
		}

		public Builder selfHarmIntent(boolean selfHarmIntent) {
			this.selfHarmIntent = selfHarmIntent;
			return this;
		}

		public Builder selfHarmInstructions(boolean selfHarmInstructions) {
			this.selfHarmInstructions = selfHarmInstructions;
			return this;
		}

		public Builder harassmentThreatening(boolean harassmentThreatening) {
			this.harassmentThreatening = harassmentThreatening;
			return this;
		}

		public Builder violence(boolean violence) {
			this.violence = violence;
			return this;
		}

		public Builder dangerousAndCriminalContent(boolean dangerousAndCriminalContent) {
			this.dangerousAndCriminalContent = dangerousAndCriminalContent;
			return this;
		}

		public Builder health(boolean health) {
			this.health = health;
			return this;
		}

		public Builder financial(boolean financial) {
			this.financial = financial;
			return this;
		}

		public Builder law(boolean law) {
			this.law = law;
			return this;
		}

		public Builder pii(boolean pii) {
			this.pii = pii;
			return this;
		}

		// 中文：构建不可变的 Categories 实例
		public Categories build() {
			return new Categories(this);
		}

	}

}
