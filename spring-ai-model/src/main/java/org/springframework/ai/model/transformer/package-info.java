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

/**
 * 【中文说明】本包提供基于大模型的<b>文档元数据增强器</b>（DocumentTransformer 实现），
 * 用于 RAG 数据摄取（ETL）管道中对文档做二次加工。
 *
 * <p>
 * 主要成员：
 * <ul>
 * <li>{@code KeywordMetadataEnricher} —— 抽取关键词写入 {@code excerpt_keywords}；</li>
 * <li>{@code SummaryMetadataEnricher} —— 生成摘要，并支持写入相邻文档的摘要，
 * 缓解文档切分后上下文缺失的问题。</li>
 * </ul>
 *
 * <p>
 * 共同特点：都会为<b>每篇文档发起一次模型调用</b>，因此处理大批量文档时需注意耗时与成本；
 * 且均为"就地修改"传入文档的元数据。
 *
 * <p>
 * 包级 {@code @NullMarked}：本包内类型默认非空，仅显式标注 {@code @Nullable} 处可为 null。
 */

@NullMarked
package org.springframework.ai.model.transformer;

import org.jspecify.annotations.NullMarked;
