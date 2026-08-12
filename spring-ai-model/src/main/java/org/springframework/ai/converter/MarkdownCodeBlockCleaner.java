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
 * A {@link ResponseTextCleaner} that removes markdown code block formatting from LLM
 * responses. This cleaner handles:
 * <ul>
 * <li>{@code ```json ... ```}</li>
 * <li>{@code ``` ... ```}</li>
 * </ul>
 *
 * @author liugddx
 * @since 1.1.0
 */
/**
 * 【中文说明】Markdown 代码围栏清洗器：剥掉模型输出外层的 <code>```</code> 包装。
 *
 * <p>
 * 大模型被要求返回 JSON 时，出于「排版习惯」经常自作主张地裹上 Markdown 代码块，例如：
 *
 * <pre>
 * ```json
 * {"name":"张三"}
 * ```
 * </pre>
 *
 * 这样的文本无法直接交给 JSON 解析器，本清洗器负责把首行的 <code>```json</code>（语言标识可有可无）
 * 与末尾的 <code>```</code> 去掉，只保留中间的真实内容。
 *
 * <p>
 * 支持的两种形态：
 * <ul>
 * <li>{@code ```json ... ```}（带语言标识）</li>
 * <li>{@code ``` ... ```}（不带语言标识）</li>
 * </ul>
 */
public class MarkdownCodeBlockCleaner implements ResponseTextCleaner {

	@Override
	// 中文：核心清洗逻辑——识别并剥离首尾的三反引号围栏
	public @Nullable String clean(@Nullable String text) {
		// 中文：空值/空串直接返回，避免后续 startsWith 等调用出错
		if (text == null || text.isEmpty()) {
			return text;
		}

		// Trim leading and trailing whitespace first
		// 中文：先修剪首尾空白，否则 startsWith("```") 会因前置换行而判断失败
		text = text.trim();

		// Check for and remove triple backticks
		// 中文：必须首尾同时命中 ``` 才认定为完整围栏，避免误伤正文中出现的反引号
		if (text.startsWith("```") && text.endsWith("```")) {
			// 中文：limit=2 只按第一个换行切成两段——[0]是围栏首行(如 ```json)，[1]是全部剩余内容
			String[] lines = text.split("\n", 2);
			String firstLine = lines[0].trim();
			if (lines.length > 1) {
				// Has the shape like ```[json] and content on following lines (captured
				// in lines[1])
				// 中文：常规多行形态，丢弃首行围栏，正文即 lines[1]
				text = lines[1];
			}
			else {
				// Single-line fenced block without line break, e.g.
				// ```{"key": "value"}```
				// Not a correct fenced block per-se, but can happen in practice
				// Strip the opening fence only; the trailing fence is removed below
				// 中文：单行退化形态（无换行），此处仅截掉开头 3 个反引号，
				// 结尾的反引号统一交给下面那行 substring 处理
				text = firstLine.substring(3);
			}

			// Remove trailing ```
			// 中文：截掉末尾 3 个字符，即收尾围栏
			text = text.substring(0, text.length() - 3);

			// Trim again to remove any potential whitespace
			// 中文：再次修剪，清除剥离围栏后残留的换行/空格
			text = text.trim();
		}

		return text;
	}

}
