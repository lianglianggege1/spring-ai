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

package org.springframework.ai.chat.observation;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Handler for emitting the chat completion content to logs.
 *
 * @author Thomas Vitale
 * @author Jonatan Ivanov
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelCompletionObservationHandler 是一个观测处理器，负责把模型"生成的回复内容"打印到日志。
 *
 * <p>
 * 触发时机：实现 {@code ObservationHandler} 的 {@code onStop} 回调，即在一次对话观测结束时执行。
 *
 * <p>
 * 重要提醒：模型回复可能包含敏感/隐私数据，因此该 Handler 默认<b>不启用</b>，
 * 需要通过配置项显式开启（如 {@code spring.ai.chat.observations.log-completion=true}），
 * 且仅建议在开发调试环境使用。
 *
 * <p>
 * 与之对应的还有 {@code ChatModelPromptContentObservationHandler}，后者记录的是输入的提示词。
 */
public class ChatModelCompletionObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	private static final Log logger = LogFactory.getLog(ChatModelCompletionObservationHandler.class);

	// 中文说明：观测结束时回调。先做 isInfoEnabled 判断——这是日志的常规优化：
	// 若 INFO 级别未开启，就跳过后续的字符串拼接与流处理，避免无谓的性能开销。
	@Override
	public void onStop(ChatModelObservationContext context) {
		if (logger.isInfoEnabled()) {
			logger.info("Chat Model Completion:\n" + ObservabilityHelper.concatenateStrings(completion(context)));
		}
	}

	// 中文说明：从上下文中抽取所有生成结果的文本，返回字符串列表。
	// 空值处理是本方法的重点：
	// 1) response 可能为 null（请求失败或尚未回填）；
	// 2) results 可能为 null 或空集合；
	// 以上任一情况都返回不可变空列表 List.of()，保证调用方不必判空。
	private List<String> completion(ChatModelObservationContext context) {
		if (context.getResponse() == null || context.getResponse().getResults() == null
				|| CollectionUtils.isEmpty(context.getResponse().getResults())) {
			return List.of();
		}

		// 中文说明：过滤掉输出为空或纯空白的生成结果（如纯工具调用的响应就没有文本），只保留有实际内容的文本
		return context.getResponse()
			.getResults()
			.stream()
			.filter(generation -> generation.getOutput() != null
					&& StringUtils.hasText(generation.getOutput().getText()))
			.map(generation -> generation.getOutput().getText())
			.toList();
	}

	// 中文说明：类型守卫，只处理对话模型的观测上下文，其他类型的观测直接跳过
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ChatModelObservationContext;
	}

}
