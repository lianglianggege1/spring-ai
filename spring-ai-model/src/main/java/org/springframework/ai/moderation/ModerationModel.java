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

import org.springframework.ai.model.Model;

/**
 * The ModerationModel interface defines a generic AI model for moderation. It extends the
 * Model interface to handle the interaction with various types of AI models. It provides
 * a single method, call, which takes a ModerationPrompt as input and returns a
 * ModerationResponse.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】内容审核模型接口：内容合规检查能力的统一抽象。
 *
 * <p>
 * 它继承自 Spring AI 的通用泛型接口 {@code Model<TReq, TRes>}，把请求类型固化为
 * {@link ModerationPrompt}、响应类型固化为 {@link ModerationResponse}，
 * 从而与 ChatModel、ImageModel、EmbeddingModel 等保持一致的编程模型。
 *
 * <p>
 * 只有一个 {@code call} 方法，因此标注了 {@link FunctionalInterface}，可用 Lambda 实现，
 * 在单元测试中很方便地伪造一个审核模型。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * ModerationResponse resp = moderationModel.call(new ModerationPrompt("待检测的文本"));
 * boolean flagged = resp.getResult().getOutput().getResults().get(0).isFlagged();
 * }</pre>
 *
 * <p>
 * 具体实现由各厂商 starter 提供，例如 OpenAI 的 {@code OpenAiModerationModel}。
 */
@FunctionalInterface
public interface ModerationModel extends Model<ModerationPrompt, ModerationResponse> {

	// 中文：执行一次内容审核调用。同步阻塞，返回包含各违规类别判定与分数的完整响应
	ModerationResponse call(ModerationPrompt request);

}
