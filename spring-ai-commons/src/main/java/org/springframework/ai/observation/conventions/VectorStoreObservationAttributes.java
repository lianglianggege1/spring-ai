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

package org.springframework.ai.observation.conventions;

/**
 * Collection of attribute keys used in vector store observations (spans, metrics,
 * events). Based on the OpenTelemetry Semantic Conventions for Vector Databases.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/database">DB
 * Semantic Conventions</a>.
 */
/**
 * 向量存储观测（链路、指标、事件）使用的属性键集合。基于OpenTelemetry向量数据库语义约定。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/database">DB
 * 语义约定</a>.
 */
public enum VectorStoreObservationAttributes {

// @formatter:off

	// DB General

	/**
	 * The name of a collection (table, container) within the database.
	 */
	/**
	 * 数据库内集合（表、容器）的名称。
	 */
	DB_COLLECTION_NAME("db.collection.name"),

	/**
	 * The name of the database, fully qualified within the server address and port.
	 */
	/**
	 * 数据库名称，包含服务器地址与端口的完整限定名。
	 */
	DB_NAMESPACE("db.namespace"),

	/**
	 * The name of the operation or command being executed.
	 */
	/**
	 * 正在执行的操作或命令的名称。
	 */
	DB_OPERATION_NAME("db.operation.name"),

	/**
	 * The record identifier if present.
	 */
	/**
	 * 记录标识符（如果存在）。
	 */
	DB_RECORD_ID("db.record.id"),

	/**
	 * The database management system (DBMS) product as identified by the client instrumentation.
	 */
	/**
	 * 由客户端埋点识别的数据库管理系统（DBMS）产品。
	 */
	DB_SYSTEM("db.system"),

	// DB Search

	/**
	 * The metric used in similarity search.
	 */
	/**
	 * 相似度搜索所使用的度量方式。
	 */
	DB_SEARCH_SIMILARITY_METRIC("db.search.similarity_metric"),

	// DB Vector

	/**
	 * The dimension of the vector.
	 */
	/**
	 * 向量的维度。
	 */
	DB_VECTOR_DIMENSION_COUNT("db.vector.dimension_count"),

	/**
	 * The name field of the vector (e.g. a field name).
	 */
	/**
	 * 向量的名称字段（例如字段名）。
	 */
	DB_VECTOR_FIELD_NAME("db.vector.field_name"),

	/**
	 * The content of the search query being executed.
	 */
	/**
	 * 正在执行的搜索查询的内容。
	 */
	DB_VECTOR_QUERY_CONTENT("db.vector.query.content"),

	/**
	 * The metadata filters used in the search query.
	 */
	/**
	 * 搜索查询中使用的元数据过滤器。
	 */
	DB_VECTOR_QUERY_FILTER("db.vector.query.filter"),

	/**
	 * Returned documents from a similarity search query.
	 */
	/**
	 * 相似度搜索查询返回的文档。
	 */
	DB_VECTOR_QUERY_RESPONSE_DOCUMENTS("db.vector.query.response.documents"),

	/**
	 * Similarity threshold that accepts all search scores. A threshold value of 0.0
	 * means any similarity is accepted or disable the similarity threshold filtering.
	 * A threshold value of 1.0 means an exact match is required.
	 */
	/**
	 * 用于筛选搜索得分的相似度阈值。阈值为0.0代表接受任意相似度，即关闭相似度阈值过滤。
	 * 阈值为1.0则要求必须完全匹配。
	 */
	DB_VECTOR_QUERY_SIMILARITY_THRESHOLD("db.vector.query.similarity_threshold"),

	/**
	 * The top-k most similar vectors returned by a query.
	 */
	/**
	 * 查询返回的top‑k个最相似向量。
	 */
	DB_VECTOR_QUERY_TOP_K("db.vector.query.top_k");

	private final String value;

	VectorStoreObservationAttributes(String value) {
		this.value = value;
	}

	/**
	 * Return the string value of the attribute.
	 * @return the string value of the attribute
	 */
	/**
	 * 返回属性的字符串值。
	 * @return 属性的字符串值
	 */
	public String value() {
		return this.value;
	}

// @formatter:on

}
