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

package org.springframework.ai.image.observation;

import java.util.StringJoiner;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;

/**
 * Handler for emitting image prompt content to logs.
 *
 * @author Thomas Vitale
 * @author Jonatan Ivanov
 * @since 1.0.0
 */
/**
 * 将图像提示词内容输出到日志的观测处理器（ObservationHandler）。
 * <p>
 * 提示词属于潜在敏感数据，因此不作为观测标签上报，而是通过本处理器单独打印到日志，
 * 需要显式注册为 Bean 才会生效，默认不开启。
 * <p>
 * 工作方式：在观测结束（onStop）时，从上下文中取出 {@link org.springframework.ai.image.ImagePrompt}
 * 的全部消息文本，拼成 {@code ["提示词1", "提示词2"]} 形式以 INFO 级别输出。
 * <p>
 * 注意：生产环境启用前请评估提示词落盘带来的隐私与合规风险。
 *
 * @since 1.0.0
 */
public class ImageModelPromptContentObservationHandler implements ObservationHandler<ImageModelObservationContext> {

	// 日志器，使用 commons-logging 以兼容 Spring 生态各类日志实现
	private static final Log logger = LogFactory.getLog(ImageModelPromptContentObservationHandler.class);

	// 观测结束时回调：打印本次请求的全部提示词内容
	@Override
	public void onStop(ImageModelObservationContext context) {
		// 空值处理：提示词列表为空则直接跳过，不产生无意义日志
		if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
			// StringJoiner 指定分隔符、前后缀，拼出 ["a", "b"] 形式的字符串
			StringJoiner promptMessagesJoiner = new StringJoiner(", ", "[", "]");
			// 逐条取出消息文本并加上双引号
			context.getRequest()
				.getInstructions()
				.forEach(message -> promptMessagesJoiner.add("\"" + message.getText() + "\""));

			// 先判断日志级别再拼接输出，避免 INFO 未开启时做无谓的字符串拼接开销
			if (logger.isInfoEnabled()) {
				logger.info("Image Model Prompt Content:\n" + promptMessagesJoiner);
			}
		}
	}

	// 类型守卫：只处理图像模型的观测上下文，其它模态的观测会被忽略
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ImageModelObservationContext;
	}

}
