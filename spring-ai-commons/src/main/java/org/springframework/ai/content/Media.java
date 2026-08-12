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

package org.springframework.ai.content;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * The Media class represents the data and metadata of a media attachment in a message. It
 * consists of a MIME type, raw data, and optional metadata such as id and name.
 *
 * <p>
 * Media objects can be used in the UserMessage class to attach various types of content
 * like images, documents, or videos. When interacting with AI models, the id and name
 * fields help track and reference specific media objects.
 *
 * <p>
 * The id field is typically assigned by AI models when they reference previously provided
 * media.
 *
 * <p>
 * The name field can be used to provide a descriptive identifier to the model, though
 * care should be taken to avoid prompt injection vulnerabilities. For amazon AWS the name
 * must only contain:
 * <ul>
 * <li>Alphanumeric characters
 * <li>Whitespace characters (no more than one in a row)
 * <li>Hyphens
 * <li>Parentheses
 * <li>Square brackets
 * </ul>
 * Note, this class does not directly enforce that restriction.
 *
 * <p>
 * If no name is provided, one will be automatically generated using the pattern:
 * {@code {mimeType.subtype}-{UUID}}
 *
 * <p>
 * This class includes a {@link Format} inner class that provides commonly used MIME types
 * as constants, organized by content category (documents, videos, images). These formats
 * can be used when constructing Media objects to ensure correct MIME type specification.
 *
 * <p>
 * This class is used as a parameter in the constructor of the UserMessage class.
 *
 * @author Christian Tzolov
 * @author Mark Pollack
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
/**
 * Media 类代表消息中媒体附件的数据与元信息。
 * 包含MIME类型、原始数据，以及id、name等可选元数据。
 *
 * <p>
 * Media 对象可用于 UserMessage，用来挂载图片、文档、视频等各类内容。
 * 和大模型交互时，id与name字段用于追踪、引用指定媒体资源。
 *
 * <p>
 * id 字段一般由大模型侧分配，用于引用已经上传过的媒体。
 *
 * <p>
 * name 字段用于向模型提供可读标识，但需要注意防范提示词注入风险。
 * 在 AWS 环境下，name仅允许使用以下字符：
 * <ul>
 * <li>字母数字字符</li>
 * <li>空白字符（不允许连续多个）</li>
 * <li>连字符</li>
 * <li>圆括号</li>
 * <li>方括号</li>
 * </ul>
 * 注意：本类不会强制校验该约束。
 *
 * <p>
 * 如果未传入name，会按照模板自动生成：{@code {mimeType.subtype}-{UUID}}
 *
 * <p>
 * 本类包含内部类 {@link Format}，提供常用MIME类型常量，按文档、视频、图片分类。
 * 构建Media对象时可以直接使用这些常量，保证MIME类型填写正确。
 *
 * <p>
 * 该类作为入参用于 UserMessage 的构造方法。
 *
 * @author Christian Tzolov
 * @author Mark Pollack
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
public class Media {

	private static final String NAME_PREFIX = "media-";

	/**
	 * An Id of the media object, usually defined when the model returns a reference to
	 * media it has been passed.
	 */
	/**
	 * 媒体对象的ID。通常在模型引用已传入的媒体资源时生成。
	 */
	private final @Nullable String id;

	private final MimeType mimeType;

	private final Object data;

	/**
	 * The name of the media object that can be referenced by the AI model.
	 * <p>
	 * Important security note: This field is vulnerable to prompt injections, as the
	 * model might inadvertently interpret it as instructions. It is recommended to
	 * specify neutral names.
	 *
	 * <p>
	 * The name must only contain:
	 * <ul>
	 * <li>Alphanumeric characters
	 * <li>Whitespace characters (no more than one in a row)
	 * <li>Hyphens
	 * <li>Parentheses
	 * <li>Square brackets
	 * </ul>
	 */
	/**
	 * 媒体对象的名称，可供AI模型进行引用。
	 * <p>
	 * 重要安全提示：该字段存在提示词注入风险，模型有可能误将名称识别为指令。
	 * 建议使用无特殊语义的中性名称。
	 * <p>
	 * 名称仅允许包含以下字符：
	 * <ul>
	 * <li>字母数字字符</li>
	 * <li>空白字符（不允许连续多个）</li>
	 * <li>连字符</li>
	 * <li>圆括号</li>
	 * <li>方括号</li>
	 * </ul>
	 */
	private final String name;

	/**
	 * Create a new Media instance.
	 * @param mimeType the media MIME type
	 * @param uri the URI for the media data
	 */
	/**
	 * 创建一个新的 Media 实例。
	 * @param mimeType 媒体资源的MIME类型
	 * @param uri 媒体数据的资源定位地址
	 */
	public Media(MimeType mimeType, URI uri) {
		Assert.notNull(mimeType, "MimeType must not be null");
		Assert.notNull(uri, "URI must not be null");
		this.mimeType = mimeType;
		this.id = null;
		this.data = uri.toString();
		this.name = generateDefaultName(mimeType);
	}

	/**
	 * Create a new Media instance.
	 * @param mimeType the media MIME type
	 * @param resource the media resource
	 */
	/**
	 * 创建一个新的 Media 实例。
	 * @param mimeType 媒体资源的MIME类型
	 * @param resource 媒体资源
	 */
	public Media(MimeType mimeType, Resource resource) {
		Assert.notNull(mimeType, "MimeType must not be null");
		Assert.notNull(resource, "Data must not be null");
		try {
			byte[] bytes = resource.getContentAsByteArray();
			this.mimeType = mimeType;
			this.id = null;
			this.data = bytes;
			this.name = generateDefaultName(mimeType);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new Media builder.
	 * @return a new Media builder instance
	 */
	/**
	 * 创建 Media 的构建器实例。
	 * @return 全新的 Media 构建器对象
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a new Media instance.
	 * @param mimeType the media MIME type
	 * @param data the media data
	 * @param id the media id
	 */
	/**
	 * 创建一个新的 Media 实例。
	 * @param mimeType 媒体资源的MIME类型
	 * @param data 媒体数据
	 * @param id 媒体ID
	 */
	private Media(MimeType mimeType, Object data, @Nullable String id, @Nullable String name) {
		Assert.notNull(mimeType, "MimeType must not be null");
		Assert.notNull(data, "Data must not be null");
		this.mimeType = mimeType;
		this.id = id;
		this.name = (name != null) ? name : generateDefaultName(mimeType);
		this.data = data;
	}

	private static String generateDefaultName(MimeType mimeType) {
		return NAME_PREFIX + mimeType.getSubtype() + "-" + java.util.UUID.randomUUID();
	}

	/**
	 * Get the media MIME type
	 * @return the media MIME type
	 */
	/**
	 * 获取媒体资源的MIME类型。
	 * @return 媒体的MIME类型
	 */
	public MimeType getMimeType() {
		return this.mimeType;
	}

	/**
	 * Get the media data object.
	 * @return a {@link String} (URI/URL/base64), a {@code byte[]}, or a
	 * {@link java.net.URL}
	 */
	/**
	 * 获取媒体数据对象。
	 * @return 可以是 {@link String}（URI/URL/base64）、字节数组 {@code byte[]} 或者 {@link java.net.URL}
	 */
	public Object getData() {
		return this.data;
	}

	/**
	 * Get the media data as a byte array
	 * @return the media data as a byte array
	 */
	/**
	 * 将媒体数据以字节数组形式获取。
	 * @return 字节数组格式的媒体数据
	 */
	public byte[] getDataAsByteArray() {
		if (this.data instanceof byte[]) {
			return (byte[]) this.data;
		}
		else {
			throw new IllegalStateException("Media data is not a byte[]");
		}
	}

	/**
	 * Get the media id
	 * @return the media id
	 */
	/**
	 * 获取媒体对象ID。
	 * @return 媒体ID
	 */
	public @Nullable String getId() {
		return this.id;
	}

	/**
	 * Get the media name.
	 * @return the media name
	 */
	/**
	 * 获取媒体对象名称。
	 * @return 媒体名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Builder class for Media.
	 */
	/**
	 * Media 的构建器类。
	 */
	public static final class Builder {

		private @Nullable String id;

		private @Nullable MimeType mimeType;

		private @Nullable Object data;

		private @Nullable String name;

		private Builder() {
		}

		/**
		 * Sets the MIME type for the media object.
		 * @param mimeType the media MIME type, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if mimeType is null
		 */
		/**
		 * 设置媒体对象的MIME类型。
		 * @param mimeType 媒体的MIME类型，不能为空
		 * @return 构建器实例
		 * @throws IllegalArgumentException 如果mimeType为空
		 */
		public Builder mimeType(MimeType mimeType) {
			Assert.notNull(mimeType, "MimeType must not be null");
			this.mimeType = mimeType;
			return this;
		}

		/**
		 * Sets the media data from a Resource.
		 * @param resource the media resource, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if resource is null or if reading the resource
		 * content fails
		 */
		/**
		 * 从Resource设置媒体数据。
		 * @param resource 媒体资源，不能为空
		 * @return 构建器实例
		 * @throws IllegalArgumentException 如果resource为空或读取资源内容失败
		 */
		public Builder data(Resource resource) {
			Assert.notNull(resource, "Data must not be null");
			try {
				this.data = resource.getContentAsByteArray();
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
			return this;
		}

		/**
		 * Sets the media data from a byte array.
		 * @param data the raw media bytes, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if data is null
		 */
		/**
		 * 通过字节数组设置媒体数据。
		 * @param data 媒体原始字节，不可为 null
		 * @return 构建器实例
		 * @throws IllegalArgumentException 当 data 为 null 时抛出
		 */
		public Builder data(byte[] data) {
			Assert.notNull(data, "Data must not be null");
			this.data = data;
			return this;
		}

		/**
		 * Sets the media data from a String. The value may be a URL string
		 * (http/https/s3), or a base64-encoded representation of the media content.
		 * @param data the media data string, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if data is null
		 */
		/**
		 * 通过字符串设置媒体数据。该值可以是URL字符串（http/https/s3），
		 * 也可以是媒体内容的base64编码字符串。
		 * @param data 媒体数据字符串，不可为 null
		 * @return 构建器实例
		 * @throws IllegalArgumentException 当 data 为 null 时抛出
		 */
		public Builder data(String data) {
			Assert.notNull(data, "Data must not be null");
			this.data = data;
			return this;
		}

		/**
		 * Sets the media data from a URI.
		 * @param uri the media URI, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if URI is null
		 */
		/**
		 * 通过URI设置媒体数据。
		 * @param uri 媒体资源URI，不可为 null
		 * @return 构建器实例
		 * @throws IllegalArgumentException 当 uri 为 null 时抛出
		 */
		public Builder data(URI uri) {
			Assert.notNull(uri, "URI must not be null");
			this.data = uri.toString();
			return this;
		}

		/**
		 * Sets the media data from a URL. The {@link URL} object is stored as-is,
		 * preserving its protocol for downstream security validation (e.g. blocking
		 * non-http/https schemes or internal addresses).
		 * @param url the media URL, must not be null
		 * @return the builder instance
		 * @throws IllegalArgumentException if URL is null
		 */
		/**
		 * 通过URL设置媒体数据。{@link URL} 对象将原样存储，
		 * 保留协议信息，用于下游安全校验（例如拦截非http/https协议、内网地址）。
		 * @param url 媒体资源URL，不可为 null
		 * @return 构建器实例
		 * @throws IllegalArgumentException 当 url 为 null 时抛出
		 */
		public Builder data(URL url) {
			Assert.notNull(url, "URL must not be null");
			this.data = url;
			return this;
		}

		/**
		 * Sets the ID for the media object. The ID is typically assigned by AI models
		 * when they return a reference to previously provided media content.
		 * @param id the media identifier
		 * @return the builder instance
		 */
		/**
		 * 设置媒体对象的ID。
		 * 该ID一般由AI模型分配，用于引用之前已经传入的媒体内容。
		 * @param id 媒体资源标识符
		 * @return 构建器实例
		 */
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the name for the media object.
		 * <p>
		 * Important security note: This field is vulnerable to prompt injections, as the
		 * model might inadvertently interpret it as instructions. It is recommended to
		 * specify neutral names.
		 *
		 * <p>
		 * The name must only contain:
		 * <ul>
		 * <li>Alphanumeric characters
		 * <li>Whitespace characters (no more than one in a row)
		 * <li>Hyphens
		 * <li>Parentheses
		 * <li>Square brackets
		 * </ul>
		 * @param name the media name
		 * @return the builder instance
		 */
		/**
		 * 设置媒体对象的名称。
		 * <p>
		 * 重要安全提示：该字段存在提示词注入风险，模型有可能误将该名称识别为指令。
		 * 建议使用无特殊语义的中性名称。
		 *
		 * <p>
		 * 名称仅允许包含以下字符：
		 * <ul>
		 * <li>字母数字字符</li>
		 * <li>空白字符（不允许连续多个）</li>
		 * <li>连字符</li>
		 * <li>圆括号</li>
		 * <li>方括号</li>
		 * </ul>
		 * @param name 媒体名称
		 * @return 构建器实例
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Builds a new Media instance with the configured properties.
		 * @return a new Media instance
		 * @throws IllegalArgumentException if mimeType or data are null
		 */
		/**
		 * 根据已配置的属性构建新的 Media 实例。
		 * @return 全新的 Media 对象
		 * @throws IllegalArgumentException 当MIME类型或数据为null时抛出
		 */
		public Media build() {
			Assert.state(this.mimeType != null, "MimeType must not be null");
			Assert.state(this.data != null, "Data must not be null");
			return new Media(this.mimeType, this.data, this.id, this.name);
		}

	}

	/**
	 * Common media formats.
	 */
	public static class Format {

		// -----------------
		// Document formats
		// -----------------
		/**
		 * Public constant mime type for {@code application/pdf}.
		 */
		public static final MimeType DOC_PDF = MimeType.valueOf("application/pdf");

		/**
		 * Public constant mime type for {@code text/csv}.
		 */
		public static final MimeType DOC_CSV = MimeType.valueOf("text/csv");

		/**
		 * Public constant mime type for {@code application/msword}.
		 */
		public static final MimeType DOC_DOC = MimeType.valueOf("application/msword");

		/**
		 * Public constant mime type for
		 * {@code application/vnd.openxmlformats-officedocument.wordprocessingml.document}.
		 */
		public static final MimeType DOC_DOCX = MimeType
			.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

		/**
		 * Public constant mime type for {@code application/vnd.ms-excel}.
		 */
		public static final MimeType DOC_XLS = MimeType.valueOf("application/vnd.ms-excel");

		/**
		 * Public constant mime type for
		 * {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}.
		 */
		public static final MimeType DOC_XLSX = MimeType
			.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		/**
		 * Public constant mime type for {@code text/html}.
		 */
		public static final MimeType DOC_HTML = MimeType.valueOf("text/html");

		/**
		 * Public constant mime type for {@code text/plain}.
		 */
		public static final MimeType DOC_TXT = MimeType.valueOf("text/plain");

		/**
		 * Public constant mime type for {@code text/markdown}.
		 */
		public static final MimeType DOC_MD = MimeType.valueOf("text/markdown");

		// -----------------
		// Video Formats
		// -----------------
		/**
		 * Public constant mime type for {@code video/x-matros}.
		 */
		public static final MimeType VIDEO_MKV = MimeType.valueOf("video/x-matros");

		/**
		 * Public constant mime type for {@code video/quicktime}.
		 */
		public static final MimeType VIDEO_MOV = MimeType.valueOf("video/quicktime");

		/**
		 * Public constant mime type for {@code video/mp4}.
		 */
		public static final MimeType VIDEO_MP4 = MimeType.valueOf("video/mp4");

		/**
		 * Public constant mime type for {@code video/webm}.
		 */
		public static final MimeType VIDEO_WEBM = MimeType.valueOf("video/webm");

		/**
		 * Public constant mime type for {@code video/x-flv}.
		 */
		public static final MimeType VIDEO_FLV = MimeType.valueOf("video/x-flv");

		/**
		 * Public constant mime type for {@code video/mpeg}.
		 */
		public static final MimeType VIDEO_MPEG = MimeType.valueOf("video/mpeg");

		/**
		 * Public constant mime type for {@code video/mpeg}.
		 */
		public static final MimeType VIDEO_MPG = MimeType.valueOf("video/mpeg");

		/**
		 * Public constant mime type for {@code video/x-ms-wmv}.
		 */
		public static final MimeType VIDEO_WMV = MimeType.valueOf("video/x-ms-wmv");

		/**
		 * Public constant mime type for {@code video/3gpp}.
		 */
		public static final MimeType VIDEO_THREE_GP = MimeType.valueOf("video/3gpp");

		// -----------------
		// Image Formats
		// -----------------
		/**
		 * Public constant mime type for {@code image/png}.
		 */
		public static final MimeType IMAGE_PNG = MimeType.valueOf("image/png");

		/**
		 * Public constant mime type for {@code image/jpeg}.
		 */
		public static final MimeType IMAGE_JPEG = MimeType.valueOf("image/jpeg");

		/**
		 * Public constant mime type for {@code image/gif}.
		 */
		public static final MimeType IMAGE_GIF = MimeType.valueOf("image/gif");

		/**
		 * Public constant mime type for {@code image/webp}.
		 */
		public static final MimeType IMAGE_WEBP = MimeType.valueOf("image/webp");

	}

}
