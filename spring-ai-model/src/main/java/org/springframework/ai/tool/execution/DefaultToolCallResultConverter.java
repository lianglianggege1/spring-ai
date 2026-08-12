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

package org.springframework.ai.tool.execution;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.util.JsonHelper;

/**
 * A default implementation of {@link ToolCallResultConverter}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ToolCallResultConverter} 的默认实现，也是框架未显式配置转换器时使用的兜底实现。
 *
 * <p>
 * 转换规则按优先级分三种情况：
 * <ol>
 * <li><b>返回类型为 void</b>：模型仍需要一个反馈，因此统一返回 JSON 字符串 {@code "Done"}；</li>
 * <li><b>结果是图片</b>（{@link RenderedImage}）：编码为 PNG 再转 Base64， 输出
 * {@code {"mimeType":"image/png","data":"..."}} 结构，便于多模态模型识别；</li>
 * <li><b>其他所有情况</b>：直接序列化为 JSON。</li>
 * </ol>
 *
 * <p>
 * 类被声明为 {@code final}，表示不打算被继承；需要自定义行为时应另行实现
 * {@link ToolCallResultConverter} 接口，而非继承本类。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class DefaultToolCallResultConverter implements ToolCallResultConverter {

	// 静态共享的 JSON 序列化辅助类（无状态，可安全复用）
	private static final JsonHelper jsonHelper = new JsonHelper();

	private static final Log logger = LogFactory.getLog(DefaultToolCallResultConverter.class);

	/**
	 * 【中文说明】按「void → 图片 → 通用 JSON」三级分支完成结果转换。
	 * @param result 工具返回的原始对象，可为 null
	 * @param returnType 工具方法返回类型，可为 null
	 * @return 回传给模型的字符串
	 */
	@Override
	public String convert(@Nullable Object result, @Nullable Type returnType) {
		// 分支一：void 方法没有返回值，但仍需给模型一个「执行成功」的确认信号
		// 注意这里用 Void.TYPE（即 void.class）而非 Void.class，二者不同
		if (returnType == Void.TYPE) {
			logger.debug("The tool has no return type. Converting to conventional response.");
			return jsonHelper.toJson("Done");
		}
		// 分支二：图片结果特殊处理，转为 Base64 编码的 PNG
		if (result instanceof RenderedImage) {
			// 预分配 4KB 缓冲区，减少写入过程中的扩容次数
			final var buf = new ByteArrayOutputStream(1024 * 4);
			try {
				ImageIO.write((RenderedImage) result, "PNG", buf);
			}
			catch (IOException e) {
				// 编码失败不抛异常，而是返回描述性文本 —— 让模型「知道」失败原因并可自行决策
				return "Failed to convert tool result to a base64 image: " + e.getMessage();
			}
			final var imgB64 = Base64.getEncoder().encodeToString(buf.toByteArray());
			// 输出为带 mimeType 的结构化 JSON，符合多模态内容的常见约定
			return jsonHelper.toJson(Map.of("mimeType", "image/png", "data", imgB64));
		}
		else {
			// 分支三：通用情况，序列化为 JSON
			// 第二个参数 forwardIfValidJson=true：若 result 本身已是合法的 JSON 字符串，
			// 则原样透传，避免被二次转义成 "\"{...}\"" 这种双重编码的结果
			logger.debug("Converting tool result to JSON.");
			return jsonHelper.toJson(result, true);
		}
	}

}
