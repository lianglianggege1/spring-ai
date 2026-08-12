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

package org.springframework.ai.tool.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

/**
 * An {@link ObservationFilter} to include the tool call content (input/output) in the
 * observation.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】工具调用<b>内容</b>观测过滤器：把工具的实际入参与执行结果补充到观测数据中。
 *
 * <p>
 * 为什么要独立成一个 Filter 而不放进默认 Convention？因为入参和结果<b>很可能包含敏感数据</b>
 * （用户身份、订单详情等），也可能体积很大。因此框架默认<b>不</b>记录它们， 只有使用者显式注册本过滤器（通常配合
 * {@code spring.ai.tool.observations.include-content=true} 配置）才会生效。
 *
 * <p>
 * 生产环境启用前请评估隐私合规与存储成本。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class ToolCallingContentObservationFilter implements ObservationFilter {

	/**
	 * 【中文说明】对观测上下文进行加工，追加入参与结果两个高基数标签。
	 * @param context 任意观测上下文
	 * @return 加工后的上下文；非工具调用类型则原样返回
	 */
	@Override
	public Observation.Context map(Observation.Context context) {
		// 类型守卫：ObservationFilter 是全局生效的，会收到所有类型的上下文，
		// 因此非工具调用的上下文必须原样放行，不做任何改动
		if (!(context instanceof ToolCallingObservationContext toolCallingObservationContext)) {
			return context;
		}

		// 入参：上下文保证其非 null（缺省为 "{}"），故无需判空，直接添加
		String toolCallArguments = toolCallingObservationContext.getToolCallArguments();
		toolCallingObservationContext
			.addHighCardinalityKeyValue(ToolCallingObservationDocumentation.HighCardinalityKeyNames.TOOL_CALL_ARGUMENTS
				.withValue(toolCallArguments));

		// 结果：可能为 null（工具无返回值或执行失败），需判空后再添加，
		// 否则会产生一个值为 null 的非法标签
		String toolCallResult = toolCallingObservationContext.getToolCallResult();
		if (toolCallResult != null) {
			toolCallingObservationContext
				.addHighCardinalityKeyValue(ToolCallingObservationDocumentation.HighCardinalityKeyNames.TOOL_CALL_RESULT
					.withValue(toolCallResult));
		}

		// 返回的是同一个对象（上面是就地修改），而非新建实例
		return toolCallingObservationContext;
	}

}
