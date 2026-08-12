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

import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.util.CollectionUtils;

/**
 * Handler for emitting the chat prompt content to logs.
 *
 * @author Thomas Vitale
 * @author Jonatan Ivanov
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelPromptContentObservationHandler 是一个观测处理器，负责把发送给模型的"提示词内容"打印到日志。
 *
 * <p>
 * 触发时机：观测结束时（{@code onStop}）统一输出，而非请求发出时。
 *
 * <p>
 * 重要提醒：提示词往往包含用户输入、业务数据甚至个人隐私，因此该 Handler 默认<b>不启用</b>，
 * 需通过配置项显式开启（如 {@code spring.ai.chat.observations.log-prompt=true}），
 * 生产环境请谨慎使用。
 *
 * <p>
 * 与 {@code ChatModelCompletionObservationHandler}（记录模型回复）配套使用，一个记录输入、一个记录输出。
 */
public class ChatModelPromptContentObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	private static final Log logger = LogFactory.getLog(ChatModelPromptContentObservationHandler.class);

	// 中文说明：观测结束时回调。同样先判断 isInfoEnabled，避免日志未开启时做无谓的拼接开销
	@Override
	public void onStop(ChatModelObservationContext context) {
		if (logger.isInfoEnabled()) {
			logger.info("Chat Model Prompt Content:\n" + ObservabilityHelper.concatenateStrings(prompt(context)));
		}
	}

	// 中文说明：抽取 Prompt 中所有消息（instructions）的文本内容。
	// 注意这里只取 getRequest()，而请求在上下文创建时就已确定，故无需像 response 那样判 null，
	// 只需用 CollectionUtils.isEmpty 处理"消息列表为空"的情况，为空则返回不可变空列表。
	private List<String> prompt(ChatModelObservationContext context) {
		if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
			return List.of();
		}

		// 中文说明：Message 继承自 Content 接口，这里统一按 Content::getText 提取纯文本
		return context.getRequest().getInstructions().stream().map(Content::getText).toList();
	}

	// 中文说明：类型守卫，只处理对话模型的观测上下文
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ChatModelObservationContext;
	}

}
