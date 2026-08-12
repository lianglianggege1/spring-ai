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

import org.springframework.ai.model.ModelResult;

/**
 * The Generation class represents a response from a moderation process. It encapsulates
 * the moderation generation metadata and the moderation object.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】审核结果的「单条生成项」包装，是 {@link ModerationResponse} 与
 * {@link Moderation} 之间的一层。
 *
 * <p>
 * 它实现 {@code ModelResult<Moderation>}，把「真正的审核数据」（{@link #getOutput()}）
 * 与「该条结果的元数据」（{@link #getMetadata()}）配对，这是 Spring AI 各模态统一的结果结构。
 *
 * <p>
 * 注意本包中的类名就叫 {@code Generation}，与 {@code org.springframework.ai.chat.model.Generation}
 * <b>同名不同包</b>，阅读源码时切勿混淆。
 *
 * <p>
 * 完整取值链路：
 * {@code response.getResult().getOutput().getResults().get(0).isFlagged()}
 */
public class Generation implements ModelResult<Moderation> {

	// 中文：空对象（Null Object）模式——用一个匿名空实现作为「无元数据」的默认值，
	// 声明为 static final 全局共享一份，避免每次 new，也让 getMetadata() 永不返回 null
	private static final ModerationGenerationMetadata NONE = new ModerationGenerationMetadata() {
	};

	// 中文：本条结果的元数据，默认指向上面的空对象
	private ModerationGenerationMetadata moderationGenerationMetadata = NONE;

	// 中文：核心载荷——审核结果对象，final 不可替换
	private final Moderation moderation;

	// 中文：简易构造器，元数据保持默认空对象
	public Generation(Moderation moderation) {
		this.moderation = moderation;
	}

	// 中文：完整构造器，同时指定审核结果与元数据
	public Generation(Moderation moderation, ModerationGenerationMetadata moderationGenerationMetadata) {
		this.moderation = moderation;
		this.moderationGenerationMetadata = moderationGenerationMetadata;
	}

	// 中文：流式（fluent）风格的元数据设置方法——注意它返回 this 而非新对象，
	// 属于「就地修改」，并非不可变对象的 withXxx 语义
	public Generation generationMetadata(ModerationGenerationMetadata moderationGenerationMetadata) {
		this.moderationGenerationMetadata = moderationGenerationMetadata;
		return this;
	}

	@Override
	// 中文：取出真正的审核结果对象
	public Moderation getOutput() {
		return this.moderation;
	}

	@Override
	// 中文：取出本条结果的元数据，因有空对象兜底，返回值不会为 null
	public ModerationGenerationMetadata getMetadata() {
		return this.moderationGenerationMetadata;
	}

	@Override
	// 中文：便于调试的字符串输出。注意本类未覆写 equals/hashCode，比较时走的是引用相等
	public String toString() {
		return "Generation{" + "moderationGenerationMetadata=" + this.moderationGenerationMetadata + ", moderation="
				+ this.moderation + '}';
	}

}
