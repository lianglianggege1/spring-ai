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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ModelResponse;

/**
 * Represents a response from a moderation process, encapsulating the moderation metadata
 * and the generated content. This class provides access to both the single generation
 * result and a list containing that result, alongside the metadata associated with the
 * moderation response. Designed for flexibility, it allows retrieval of
 * moderation-specific metadata as well as the moderated content.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】审核响应：{@link ModerationModel#call} 的返回值，是最外层的结果容器。
 *
 * <p>
 * 内部只持有<b>一个</b> {@link Generation}（审核场景一次只处理一条输入），
 * 但为了兼容 {@code ModelResponse} 接口「结果可能有多个」的通用约定，
 * 同时提供了单个与列表两种读取方式。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code generation} —— 审核结果项，可为 null（如调用失败或无结果）</li>
 * <li>{@code moderationResponseMetadata} —— 响应级元数据，非空</li>
 * </ul>
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ModerationResponse resp = moderationModel.call(prompt);
 * Moderation m = resp.getResult().getOutput();
 * }</pre>
 */
public class ModerationResponse implements ModelResponse<Generation> {

	// 中文：响应级元数据，构造时必定赋值，非空
	private final ModerationResponseMetadata moderationResponseMetadata;

	// 中文：唯一的审核结果项。标注 @Nullable，取用前建议判空
	private final @Nullable Generation generation;

	// 中文：简易构造器，自动创建一个空的元数据对象，避免 metadata 为 null
	public ModerationResponse(@Nullable Generation generation) {
		this(generation, new ModerationResponseMetadata());
	}

	// 中文：完整构造器
	public ModerationResponse(@Nullable Generation generation, ModerationResponseMetadata moderationResponseMetadata) {
		this.moderationResponseMetadata = moderationResponseMetadata;
		this.generation = generation;
	}

	@Override
	// 中文：获取唯一结果项，可能为 null
	public @Nullable Generation getResult() {
		return this.generation;
	}

	@Override
	// 中文：以列表形式返回结果，用于适配 ModelResponse 的通用契约。
	// 空值处理：generation 为 null 时返回空列表而非含 null 的列表，保证调用方可安全遍历
	public List<Generation> getResults() {
		if (this.generation == null) {
			return Collections.emptyList();
		}
		return List.of(this.generation);
	}

	@Override
	// 中文：获取响应级元数据
	public ModerationResponseMetadata getMetadata() {
		return this.moderationResponseMetadata;
	}

	@Override
	public String toString() {
		return "ModerationResponse{" + "moderationResponseMetadata=" + this.moderationResponseMetadata
				+ ", generations=" + this.generation + '}';
	}

	@Override
	// 中文：按元数据 + 结果项做值比较。注意 Generation 未覆写 equals，
	// 因此这里实际退化为对 generation 的引用比较
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModerationResponse that)) {
			return false;
		}
		return Objects.equals(this.moderationResponseMetadata, that.moderationResponseMetadata)
				&& Objects.equals(this.generation, that.generation);
	}

	@Override
	// 中文：与 equals 保持字段一致
	public int hashCode() {
		return Objects.hash(this.moderationResponseMetadata, this.generation);
	}

}
