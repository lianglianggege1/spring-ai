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

package org.springframework.ai.converter;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for cleaning LLM response text before parsing. Different
 * implementations can handle various response formats and patterns from different AI
 * models.
 *
 * @author liugddx
 * @since 1.1.0
 */
/**
 * 【中文说明】响应文本清洗策略接口（策略模式）：在正式解析前，先剔除模型输出中的干扰内容。
 *
 * <p>
 * 不同厂商的模型即便被要求「只输出 JSON」，实际返回里也常混入额外内容，例如：
 * <ul>
 * <li>Markdown 代码围栏：<code>```json ... ```</code>——由
 * {@code MarkdownCodeBlockCleaner} 处理</li>
 * <li>思维链标签：<code>&lt;think&gt;...&lt;/think&gt;</code>——由 {@code ThinkingTagCleaner}
 * 处理</li>
 * <li>首尾多余空白——由 {@code WhitespaceCleaner} 处理</li>
 * </ul>
 * 每个实现只负责一种清洗动作，再由 {@code CompositeResponseTextCleaner} 串成责任链依次执行，
 * 这样可以按模型特性自由组合，而不必写死在解析器里。
 *
 * <p>
 * 本接口标注了 {@link FunctionalInterface}，因此可直接用 Lambda 实现，例如
 * {@code ResponseTextCleaner c = t -> t == null ? null : t.strip();}。
 *
 * <p>
 * 空值约定：参数与返回值均标注 {@link Nullable}，实现类必须能接受 null 输入并安全返回。
 */
@FunctionalInterface
public interface ResponseTextCleaner {

	/**
	 * Clean the given text by removing unwanted patterns, tags, or formatting.
	 * @param text the raw text from LLM response
	 * @return the cleaned text ready for parsing
	 */
	/**
	 * 【中文说明】清洗文本：移除不需要的模式、标签或格式包装。
	 *
	 * <p>
	 * 实现需保持「幂等且宽容」——输入为 null 或不含目标模式时应原样返回，不得抛异常。
	 * @param text 大模型返回的原始文本，可为 null
	 * @return 清洗后可直接交给解析器的文本，可为 null
	 */
	@Nullable String clean(@Nullable String text);

}
