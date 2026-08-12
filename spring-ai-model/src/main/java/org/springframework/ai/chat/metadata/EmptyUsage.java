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

package org.springframework.ai.chat.metadata;

import java.util.Map;

/**
 * A EmptyUsage implementation that returns zero for all property getters
 *
 * @author John Blum
 * @author Ilayaperumal Gopinathan
 * @since 0.7.0
 */
/**
 * 【中文说明】{@link Usage} 的"**空对象**"实现（Null Object 设计模式）：所有 token 数一律返回 0。
 *
 * <p>
 * 用途：当厂商响应里没有用量信息时，用它代替 null 作为默认值
 * （见 {@link ChatResponseMetadata} 中 {@code usage} 字段的初始化），
 * 这样调用方写 {@code metadata.getUsage().getTotalTokens()} 时不必层层判空，也不会抛 NPE，
 * 只会自然地得到 0。
 *
 * <p>
 * 注意：{@link #getTotalTokens()} 未被重写，会走接口的 default 实现（0 + 0 = 0），结果同样为 0。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author John Blum、Ilayaperumal Gopinathan；@since 0.7.0。
 */
public class EmptyUsage implements Usage {

	// 【中文】输入 token 恒为 0。
	@Override
	public Integer getPromptTokens() {
		return 0;
	}

	// 【中文】输出 token 恒为 0。
	@Override
	public Integer getCompletionTokens() {
		return 0;
	}

	// 【中文】原始用量对象返回不可变空 Map（Map.of()）而不是 null，同样是为了让调用方免于判空。
	@Override
	public Object getNativeUsage() {
		return Map.of();
	}

}
