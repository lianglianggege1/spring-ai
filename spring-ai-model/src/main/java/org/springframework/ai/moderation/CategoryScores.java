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
 * This class represents the scores for different categories of content. Each category has
 * a score ranging from 0.0 to 1.0. The scores represent the severity or intensity of the
 * content in each respective category.
 *
 * @author Ahmed Yousri
 * @author Ilayaperumal Gopinathan
 * @author Ricken Bazolo
 * @author Jonghoon Park
 * @since 1.0.0
 */
/**
 * 【中文说明】违规类别置信度分数表：给出每个审核维度的量化得分。
 *
 * <p>
 * 字段与 {@link Categories} 完全一一对应，区别在于类型是 {@code double}：
 * 取值范围 0.0 ~ 1.0，数值越高表示模型认为内容属于该类别的可能性/严重程度越高。
 *
 * <p>
 * 使用场景：{@code Categories} 用的是厂商内置阈值，而本类允许业务方自定阈值做更细的管控，例如：
 *
 * <pre>{@code
 * if (scores.getViolence() > 0.3) { // 比官方默认更严格
 * 	// 走人工复审
 * }
 * }</pre>
 *
 * <p>
 * 各字段含义（与 Categories 相同）：sexual 色情、hate 仇恨、harassment 骚扰、
 * selfHarm 自我伤害、sexualMinors 未成年人色情、hateThreatening 威胁性仇恨、
 * violenceGraphic 血腥暴力、selfHarmIntent 自伤意图、selfHarmInstructions 自伤教程、
 * harassmentThreatening 威胁性骚扰、violence 暴力、dangerousAndCriminalContent 危险与犯罪、
 * health 医疗、financial 金融、law 法律、pii 个人隐私信息。
 *
 * <p>
 * 同样是 {@code final} 类 + 全 {@code final} 字段的不可变值对象，须经 {@link #builder()} 创建。
 */
public final class CategoryScores {

	// 中文：以下 16 个字段为各违规类别的置信度分数，取值 0.0~1.0，默认 0.0。
	// 含义与 Categories 中的同名字段一一对应，此处不再逐条重复。

	private final double sexual;

	private final double hate;

	private final double harassment;

	private final double selfHarm;

	private final double sexualMinors;

	private final double hateThreatening;

	private final double violenceGraphic;

	private final double selfHarmIntent;

	private final double selfHarmInstructions;

	private final double harassmentThreatening;

	private final double violence;

	private final double dangerousAndCriminalContent;

	private final double health;

	private final double financial;

	private final double law;

	private final double pii;

	// 中文：私有构造器，从 Builder 逐字段拷贝
	private CategoryScores(Builder builder) {
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

	// 中文：以下均为各类别分数的只读访问器，返回 0.0~1.0 的置信度
	public double getSexual() {
		return this.sexual;
	}

	public double getHate() {
		return this.hate;
	}

	public double getHarassment() {
		return this.harassment;
	}

	public double getSelfHarm() {
		return this.selfHarm;
	}

	public double getSexualMinors() {
		return this.sexualMinors;
	}

	public double getHateThreatening() {
		return this.hateThreatening;
	}

	public double getViolenceGraphic() {
		return this.violenceGraphic;
	}

	public double getSelfHarmIntent() {
		return this.selfHarmIntent;
	}

	public double getSelfHarmInstructions() {
		return this.selfHarmInstructions;
	}

	public double getHarassmentThreatening() {
		return this.harassmentThreatening;
	}

	public double getViolence() {
		return this.violence;
	}

	public double getDangerousAndCriminalContent() {
		return this.dangerousAndCriminalContent;
	}

	public double getHealth() {
		return this.health;
	}

	public double getFinancial() {
		return this.financial;
	}

	public double getLaw() {
		return this.law;
	}

	public double getPii() {
		return this.pii;
	}

	@Override
	// 中文：值相等比较。此处统一用 Double.compare(...) == 0 而非 ==，
	// 这是浮点比较的标准做法——它能正确处理 NaN 与 +0.0/-0.0 这两类特殊值
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CategoryScores that)) {
			return false;
		}
		return Double.compare(that.sexual, this.sexual) == 0 && Double.compare(that.hate, this.hate) == 0
				&& Double.compare(that.harassment, this.harassment) == 0
				&& Double.compare(that.selfHarm, this.selfHarm) == 0
				&& Double.compare(that.sexualMinors, this.sexualMinors) == 0
				&& Double.compare(that.hateThreatening, this.hateThreatening) == 0
				&& Double.compare(that.violenceGraphic, this.violenceGraphic) == 0
				&& Double.compare(that.selfHarmIntent, this.selfHarmIntent) == 0
				&& Double.compare(that.selfHarmInstructions, this.selfHarmInstructions) == 0
				&& Double.compare(that.harassmentThreatening, this.harassmentThreatening) == 0
				&& Double.compare(that.violence, this.violence) == 0
				&& Double.compare(that.dangerousAndCriminalContent, this.dangerousAndCriminalContent) == 0
				&& Double.compare(that.health, this.health) == 0 && Double.compare(that.financial, this.financial) == 0
				&& Double.compare(that.law, this.law) == 0 && Double.compare(that.pii, this.pii) == 0;
	}

	@Override
	// 中文：与 equals 使用相同的 16 个字段（double 会自动装箱为 Double）
	public int hashCode() {
		return Objects.hash(this.sexual, this.hate, this.harassment, this.selfHarm, this.sexualMinors,
				this.hateThreatening, this.violenceGraphic, this.selfHarmIntent, this.selfHarmInstructions,
				this.harassmentThreatening, this.violence, this.dangerousAndCriminalContent, this.health,
				this.financial, this.law, this.pii);
	}

	@Override
	// 中文：调试输出，平铺展示全部类别分数
	public String toString() {
		return "CategoryScores{" + "sexual=" + this.sexual + ", hate=" + this.hate + ", harassment=" + this.harassment
				+ ", selfHarm=" + this.selfHarm + ", sexualMinors=" + this.sexualMinors + ", hateThreatening="
				+ this.hateThreatening + ", violenceGraphic=" + this.violenceGraphic + ", selfHarmIntent="
				+ this.selfHarmIntent + ", selfHarmInstructions=" + this.selfHarmInstructions
				+ ", harassmentThreatening=" + this.harassmentThreatening + ", violence=" + this.violence
				+ ", dangerousAndCriminalContent=" + this.dangerousAndCriminalContent + ", health=" + this.health
				+ ", financial=" + this.financial + ", law=" + this.law + ", pii=" + this.pii + '}';
	}

	/**
	 * 【中文说明】{@link CategoryScores} 的建造器。
	 *
	 * <p>
	 * 所有字段可选，未设置的分数保持 double 默认值 {@code 0.0}。每个 setter 返回
	 * {@code this} 以支持链式调用，通常由各厂商的响应转换器在解析 API 返回体时填充。
	 */
	public static final class Builder {

		// 中文：与外部类一一对应的暂存字段，默认 0.0
		private double sexual;

		private double hate;

		private double harassment;

		private double selfHarm;

		private double sexualMinors;

		private double hateThreatening;

		private double violenceGraphic;

		private double selfHarmIntent;

		private double selfHarmInstructions;

		private double harassmentThreatening;

		private double violence;

		private double dangerousAndCriminalContent;

		private double health;

		private double financial;

		private double law;

		private double pii;

		// 中文：以下均为链式 setter，逐一设置对应类别的置信度分数
		public Builder sexual(double sexual) {
			this.sexual = sexual;
			return this;
		}

		public Builder hate(double hate) {
			this.hate = hate;
			return this;
		}

		public Builder harassment(double harassment) {
			this.harassment = harassment;
			return this;
		}

		public Builder selfHarm(double selfHarm) {
			this.selfHarm = selfHarm;
			return this;
		}

		public Builder sexualMinors(double sexualMinors) {
			this.sexualMinors = sexualMinors;
			return this;
		}

		public Builder hateThreatening(double hateThreatening) {
			this.hateThreatening = hateThreatening;
			return this;
		}

		public Builder violenceGraphic(double violenceGraphic) {
			this.violenceGraphic = violenceGraphic;
			return this;
		}

		public Builder selfHarmIntent(double selfHarmIntent) {
			this.selfHarmIntent = selfHarmIntent;
			return this;
		}

		public Builder selfHarmInstructions(double selfHarmInstructions) {
			this.selfHarmInstructions = selfHarmInstructions;
			return this;
		}

		public Builder harassmentThreatening(double harassmentThreatening) {
			this.harassmentThreatening = harassmentThreatening;
			return this;
		}

		public Builder violence(double violence) {
			this.violence = violence;
			return this;
		}

		public Builder dangerousAndCriminalContent(double dangerousAndCriminalContent) {
			this.dangerousAndCriminalContent = dangerousAndCriminalContent;
			return this;
		}

		public Builder health(double health) {
			this.health = health;
			return this;
		}

		public Builder financial(double financial) {
			this.financial = financial;
			return this;
		}

		public Builder law(double law) {
			this.law = law;
			return this;
		}

		public Builder pii(double pii) {
			this.pii = pii;
			return this;
		}

		// 中文：构建不可变的 CategoryScores 实例
		public CategoryScores build() {
			return new CategoryScores(this);
		}

	}

}
