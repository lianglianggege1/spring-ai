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
 * A {@link ResponseTextCleaner} that trims leading and trailing whitespace from text.
 *
 * @author liugddx
 * @since 1.1.0
 */
/**
 * 【中文说明】空白字符清洗器：去掉文本首尾的空格、换行、制表符等。
 *
 * <p>
 * 这是最简单的一个 {@link ResponseTextCleaner} 实现，通常放在清洗责任链的
 * <b>最后一环</b>——前面的清洗器（去代码围栏、去思维链标签）执行完后往往会残留换行和空格，
 * 由它做最终修剪，保证交给 JSON 解析器的字符串以 <code>{</code> 或 <code>[</code> 开头。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * new CompositeResponseTextCleaner(new ThinkingTagCleaner(), new MarkdownCodeBlockCleaner(),
 * 		new WhitespaceCleaner());
 * }</pre>
 */
public class WhitespaceCleaner implements ResponseTextCleaner {

	@Override
	// 中文：null 直接透传（不抛异常），非 null 才调用 trim()，符合接口的「宽容处理」约定
	public @Nullable String clean(@Nullable String text) {
		return text != null ? text.trim() : text;
	}

}
