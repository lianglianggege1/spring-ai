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

package org.springframework.ai.model;

/**
 * Description of an embedding model.
 *
 * @author Christian Tzolov
 */
/**
 * 【中文说明】EmbeddingModelDescription 是<b>嵌入（向量化）模型</b>专用的描述接口，
 * 在 {@link ModelDescription} 基础上补充了"向量维度"这一嵌入模型特有属性。
 *
 * <p>
 * 关键方法 {@link #getDimensions()}：返回该模型输出向量的维度，例如 OpenAI 的
 * text-embedding-3-small 为 1536。这个信息非常重要——向量数据库建表时必须预先知道维度，
 * 且不同维度的向量之间无法计算相似度。
 *
 * <p>
 * 注意默认值约定：default 实现返回 <b>-1</b>，作为"未知/未声明维度"的哨兵值（sentinel），
 * 而不是 0 或抛异常。调用方拿到 -1 时应理解为"该模型未声明维度"，
 * 通常需要通过实际发起一次嵌入调用来探测真实维度。
 *
 * <p>
 * 典型用法：厂商的嵌入模型枚举实现本接口，同时给出 getName() 与 getDimensions()。
 *
 * @author Christian Tzolov
 */
public interface EmbeddingModelDescription extends ModelDescription {

	// 【中文】返回嵌入向量的维度数。默认返回 -1 表示"维度未知"，
	// 使用前建议判断返回值是否大于 0 再做后续处理（如校验与向量库配置是否匹配）。
	default int getDimensions() {
		return -1;
	}

}
