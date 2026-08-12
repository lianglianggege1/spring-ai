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

package org.springframework.ai.model.observation;

import io.micrometer.observation.Observation;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.util.Assert;

/**
 * Context used when sending a request to a machine learning model and waiting for a
 * response from the model provider.
 *
 * @param <REQ> type of the request object
 * @param <RES> type of the response object
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】ModelObservationContext 是模型调用的<b>可观测性上下文</b>，
 * 继承自 Micrometer 的 {@code Observation.Context}。
 *
 * <p>
 * 用途：在"发起模型请求 -> 等待厂商响应"这段过程中充当数据载体。Micrometer 会在
 * 观测开始时创建它、结束时读取它，各类 ObservationHandler / ObservationConvention
 * 则从中提取信息，生成链路追踪 span 的标签和指标（如模型名、token 用量、耗时）。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code request}（final，必填）—— 本次发送的请求对象；</li>
 * <li>{@code operationMetadata}（final，必填）—— 操作元数据，描述这是哪类 AI 操作
 * （chat/embedding 等）以及由哪个厂商提供；</li>
 * <li>{@code response}（可空、可变）—— 响应对象。之所以不是 final：创建上下文时调用
 * 尚未完成，响应只能在拿到结果后通过 setter 回填。因此读取时<b>必须判空</b>——
 * 若调用抛异常，该字段会一直保持 null。</li>
 * </ul>
 *
 * <p>
 * 关键泛型参数：{@code REQ}/{@code RES} 为请求与响应类型，使同一个上下文类可复用于
 * 聊天、嵌入、图像等各种模态的观测。
 *
 * <p>
 * 典型用法：各 Model 实现在 call() 内部构造本上下文并交给 Observation 包裹执行。
 *
 * @param <REQ> 请求对象的类型
 * @param <RES> 响应对象的类型
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class ModelObservationContext<REQ, RES> extends Observation.Context {

	// 【中文】本次调用的请求对象。final 且在构造时校验非空，保证观测数据完整。
	private final REQ request;

	// 【中文】AI 操作元数据：标识操作类型（如 chat）与厂商（如 openai），
	// 是生成低基数（low cardinality）指标标签的主要来源。
	private final AiOperationMetadata operationMetadata;

	// 【中文】响应对象。刻意设计为非 final 且 @Nullable：
	// 上下文在调用前创建，响应要等调用返回后才回填；若调用失败则永远为 null。
	private @Nullable RES response;

	// 【中文】构造器：只接收两个必填项，响应留待后续 setResponse 补充。
	public ModelObservationContext(REQ request, AiOperationMetadata operationMetadata) {
		// 参数校验：两者都是观测所必需的，缺失则快速失败，避免产生无意义的观测数据
		Assert.notNull(request, "request cannot be null");
		Assert.notNull(operationMetadata, "operationMetadata cannot be null");
		this.request = request;
		this.operationMetadata = operationMetadata;
	}

	// 【中文】获取请求对象，供 ObservationConvention 提取模型名、参数等标签。
	public REQ getRequest() {
		return this.request;
	}

	// 【中文】获取 AI 操作元数据（操作类型 + 厂商）。
	public AiOperationMetadata getOperationMetadata() {
		return this.operationMetadata;
	}

	// 【中文】获取响应对象。返回值标注 @Nullable：
	// 当调用尚未完成或已失败时为 null，调用方（如各类 Handler）必须先判空再使用。
	public @Nullable RES getResponse() {
		return this.response;
	}

	// 【中文】回填响应对象，通常在模型调用成功返回后由框架调用。
	public void setResponse(RES response) {
		// 参数校验：不允许显式塞入 null。
		// 即"未设置"（字段仍为 null）与"设置为 null"是两种不同语义，后者被禁止
		Assert.notNull(response, "response cannot be null");
		this.response = response;
	}

}
