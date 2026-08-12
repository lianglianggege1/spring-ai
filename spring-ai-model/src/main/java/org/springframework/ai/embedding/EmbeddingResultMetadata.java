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

package org.springframework.ai.embedding;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.model.ResultMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Metadata associated with the embedding result.
 *
 * @author Christian Tzolov
 * @author Jihoon Kim
 */
/**
 * 单条嵌入结果的元数据：描述"这条向量是由什么源数据生成的"。
 *
 * <p>
 * 与 {@link EmbeddingResponseMetadata}（响应级、含 token 用量）不同，本类是<b>结果级</b>的，
 * 面向多模态嵌入场景——同一次调用里可能混有文本、图片、音频等不同模态的输入。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code modalityType}：源数据模态（文本/图片/音频/视频）；</li>
 * <li>{@code documentId}：源文档 ID，便于把向量回溯到原始文档；</li>
 * <li>{@code mimeType}：源数据的 MIME 类型，如 {@code text/plain}、{@code image/png}；</li>
 * <li>{@code documentData}：源数据本体，<b>可为 null</b>（通常不回传以节省内存）。</li>
 * </ul>
 *
 * <p>
 * 特别注意 {@code EMPTY} 常量：它是"无元数据"场景下的共享空对象，被
 * {@link Embedding} 用作默认值。该字段只声明为 {@code static} 而<b>没有 final</b>，
 * 理论上可被外部改写，属于设计上的瑕疵，使用时不应对其重新赋值。
 *
 * @author Christian Tzolov
 * @author Jihoon Kim
 */
public class EmbeddingResultMetadata implements ResultMetadata {

	// 共享的空元数据实例（空对象模式），供 Embedding 等处作默认值使用
	// 注意：此处缺少 final 修饰，属历史遗留，切勿对其重新赋值
	public static EmbeddingResultMetadata EMPTY = new EmbeddingResultMetadata();

	/**
	 * The {@link ModalityType} of the source data used to generate the embedding.
	 */
	// 生成该向量的源数据模态类型
	private final ModalityType modalityType;

	// 源文档 ID，用于把向量回溯到原始文档
	private final String documentId;

	// 源数据的 MIME 类型
	private final MimeType mimeType;

	// 源数据本体；允许为 null，多数实现不回传以节省内存
	private final @Nullable Object documentData;

	// 无参构造器：生成"纯文本 + 空 ID + text/plain + 无数据"的默认元数据，即 EMPTY 的内容
	public EmbeddingResultMetadata() {
		this("", ModalityType.TEXT, MimeTypeUtils.TEXT_PLAIN, null);
	}

	// 全参构造器：对两个关键枚举/类型字段做非空校验，documentData 则显式允许为 null
	public EmbeddingResultMetadata(String documentId, ModalityType modalityType, MimeType mimeType,
			@Nullable Object documentData) {
		// 参数校验：模态与 MIME 类型是判定语义的关键，不允许缺失
		Assert.notNull(modalityType, "ModalityType must not be null");
		Assert.notNull(mimeType, "MimeType must not be null");

		this.documentId = documentId;
		this.modalityType = modalityType;
		this.mimeType = mimeType;
		this.documentData = documentData;
	}

	// 返回源数据的模态类型
	public ModalityType getModalityType() {
		return this.modalityType;
	}

	// 返回源数据的 MIME 类型
	public MimeType getMimeType() {
		return this.mimeType;
	}

	// 返回源文档 ID
	public String getDocumentId() {
		return this.documentId;
	}

	// 返回源数据本体，可能为 null
	public @Nullable Object getDocumentData() {
		return this.documentData;
	}

	/**
	 * 模态类型枚举：标识生成向量的源数据属于哪一类媒体，支撑多模态嵌入。
	 */
	public enum ModalityType {

		TEXT, IMAGE, AUDIO, VIDEO

	}

	/**
	 * 模态推断工具类：根据源数据的 {@link MimeType} 反推其 {@link ModalityType}。
	 *
	 * <p>
	 * 内部预置了四个通配 MIME 类型常量（{@code text/*}、{@code image/*} 等），
	 * 通过 {@code isCompatibleWith} 做通配匹配。
	 */
	public static class ModalityUtils {

		// 以下四个通配 MIME 常量用于按大类匹配，例如 text/* 可匹配 text/plain、text/html 等
		private static final MimeType TEXT_MIME_TYPE = MimeTypeUtils.parseMimeType("text/*");

		private static final MimeType IMAGE_MIME_TYPE = MimeTypeUtils.parseMimeType("image/*");

		private static final MimeType VIDEO_MIME_TYPE = MimeTypeUtils.parseMimeType("video/*");

		private static final MimeType AUDIO_MIME_TYPE = MimeTypeUtils.parseMimeType("audio/*");

		/**
		 * Infers the {@link ModalityType} of the source data used to generate the
		 * embedding using the source data {@link MimeType}.
		 * @param mimeType the {@link MimeType} of the source data.
		 * @return Returns the {@link ModalityType} of the source data used to generate
		 * the embedding.
		 */
		// 根据 MIME 类型推断模态；无法识别的类型会抛出 IllegalArgumentException
		public static ModalityType getModalityType(MimeType mimeType) {

			// 空值处理：MIME 缺失时保守地按文本处理（文本是最常见的嵌入输入）
			if (mimeType == null) {
				return ModalityType.TEXT;
			}

			// 依次做通配匹配；注意 TEXT 放在最后判断，因为它的匹配范围最容易误命中
			if (mimeType.isCompatibleWith(IMAGE_MIME_TYPE)) {
				return ModalityType.IMAGE;
			}
			else if (mimeType.isCompatibleWith(AUDIO_MIME_TYPE)) {
				return ModalityType.AUDIO;
			}
			else if (mimeType.isCompatibleWith(VIDEO_MIME_TYPE)) {
				return ModalityType.VIDEO;
			}
			else if (mimeType.isCompatibleWith(TEXT_MIME_TYPE)) {
				return ModalityType.TEXT;
			}

			// 四大类都不匹配（如 application/pdf）则视为不支持，快速失败而非静默降级
			throw new IllegalArgumentException("Unsupported MimeType: " + mimeType);
		}

	}

}
