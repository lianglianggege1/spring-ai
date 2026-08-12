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

package org.springframework.ai.tool.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Miscellaneous tool utility methods. Mainly for internal use within the framework.
 *
 * @author Thomas Vitale
 */
/**
 * 【中文说明】{@code ToolUtils}：工具相关的<b>静态工具类</b>，主要供框架内部使用。
 *
 * <p>
 * <b>核心职责：</b>从一个 Java {@link Method} 上「解析」出构建工具所需的各类信息，
 * 统一封装「读取 {@code @Tool} 注解 → 取不到就走兜底逻辑」这套重复代码。
 * </p>
 *
 * <p>
 * <b>方法一览：</b>
 * </p>
 * <ul>
 * <li>{@link #getToolName(Method)} —— 解析工具名（注解 name → 方法名），并做命名规范校验。</li>
 * <li>{@link #getToolDescription(Method)} —— 解析工具描述。</li>
 * <li>{@link #getToolDescriptionFromName(String)} —— 由名称驼峰拆词生成描述。</li>
 * <li>{@link #getToolReturnDirect(Method)} —— 解析 returnDirect 开关。</li>
 * <li>{@link #getToolCallResultConverter(Method)} —— 反射实例化结果转换器。</li>
 * <li>{@link #getDuplicateToolNames} —— 检测一批 ToolCallback 中的重名工具。</li>
 * </ul>
 *
 * <p>
 * <b>设计要点：</b>类声明为 {@code final} + 私有构造器，是「不可被继承、不可被实例化」的
 * 纯静态工具类惯用写法。所有方法均使用 {@code AnnotatedElementUtils.findMergedAnnotation}
 * 而非 {@code method.getAnnotation}，以支持<b>组合注解</b>（元注解上的 {@code @Tool} 也能识别）。
 * </p>
 *
 * @author Thomas Vitale
 */
public final class ToolUtils {

	// 中文：使用 Apache Commons Logging 门面，Spring 生态的传统做法（可适配 Logback/Log4j2 等实现）。
	private static final Log logger = LogFactory.getLog(ToolUtils.class);

	/**
	 * Regular expression pattern for recommended tool names. Tool names should contain
	 * only alphanumeric characters, underscores, hyphens, and dots for maximum
	 * compatibility across different LLMs.
	 */
	// 中文：推荐的工具命名正则 —— 仅允许「字母、数字、下划线、点、连字符」。
	// 注意它只用于"告警"而非"拦截"，不合规仅打 warn 日志，不会抛异常。
	private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\.-]+$");

	// 中文：私有构造器，防止这个纯静态工具类被实例化。
	private ToolUtils() {
	}

	// 中文：解析工具名称。优先级：@Tool(name) → 方法名。
	public static String getToolName(Method method) {
		Assert.notNull(method, "method cannot be null");
		// 中文：findMergedAnnotation 支持"合并注解/组合注解"语义，
		// 即便 @Tool 是标注在另一个自定义注解上（作为元注解），也能被找到并合并属性。
		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
		String toolName;
		// 中文：分支一 —— 方法上没有 @Tool 注解，直接用方法名兜底。
		if (tool == null) {
			toolName = method.getName();
		}
		else {
			// 中文：分支二 —— 有注解但 name 为空串（默认值）时，同样回退到方法名。
			toolName = StringUtils.hasText(tool.name()) ? tool.name() : method.getName();
		}
		// 中文：命名规范校验（仅告警，不阻断流程）。
		validateToolName(toolName);
		return toolName;
	}

	// 中文：由工具名生成描述 —— 把驼峰命名拆成空格分隔的词组。
	// 例如 "getCurrentWeather" -> "get Current Weather"，作为缺省描述比原始方法名更可读。
	public static String getToolDescriptionFromName(String toolName) {
		Assert.hasText(toolName, "toolName cannot be null or empty");
		return ParsingUtils.reConcatenateCamelCase(toolName, " ");
	}

	// 中文：解析工具描述。优先级：@Tool(description) → 方法名。
	public static String getToolDescription(Method method) {
		Assert.notNull(method, "method cannot be null");
		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
		// 中文：无注解时，用驼峰拆词后的方法名作为描述（可读性更好）。
		if (tool == null) {
			return ParsingUtils.reConcatenateCamelCase(method.getName(), " ");
		}
		// 中文：注意这里的兜底与上面不同 —— 有注解但 description 为空时，
		// 返回的是"原始方法名"（未做驼峰拆词），与无注解分支的行为略有差异。
		return StringUtils.hasText(tool.description()) ? tool.description() : method.getName();
	}

	// 中文：解析 returnDirect 开关。无 @Tool 注解时默认 false（结果回灌给模型）。
	// 使用 "tool != null &&" 的短路写法同时完成空值处理与取值。
	public static boolean getToolReturnDirect(Method method) {
		Assert.notNull(method, "method cannot be null");
		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
		return tool != null && tool.returnDirect();
	}

	// 中文：解析并实例化"工具结果转换器"。
	public static ToolCallResultConverter getToolCallResultConverter(Method method) {
		Assert.notNull(method, "method cannot be null");
		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
		// 中文：无注解时返回默认的 JSON 转换器。
		if (tool == null) {
			return new DefaultToolCallResultConverter();
		}
		// 中文：注解里只能携带 Class 对象，所以必须在运行期反射实例化。
		var type = tool.resultConverter();
		try {
			// 中文：要求转换器必须提供"无参构造器"（可以是非 public 的声明构造器）。
			return type.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			// 中文：把反射相关的受检异常统一包装成 IllegalArgumentException，
			// 并保留原始异常作为 cause，便于定位（常见原因：转换器没有无参构造器）。
			throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + type, e);
		}
	}

	// 中文：找出一批 ToolCallback 中"重名"的工具名列表。
	// 工具名在同一次请求中必须唯一，否则模型无法区分该调用哪个，因此注册前需要做这项校验。
	public static List<String> getDuplicateToolNames(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		return toolCallbacks.stream()
			// 中文：第一步 —— 按工具名分组并计数，得到 Map<工具名, 出现次数>。
			.collect(Collectors.groupingBy(toolCallback -> toolCallback.getToolDefinition().name(),
					Collectors.counting()))
			.entrySet()
			.stream()
			// 中文：第二步 —— 只保留出现次数大于 1 的条目，即重复项。
			.filter(entry -> entry.getValue() > 1)
			// 中文：第三步 —— 只取工具名，丢弃计数。
			.map(Map.Entry::getKey)
			.toList();
	}

	// 中文：上一个方法的可变参数（varargs）重载，方便直接传入若干个 ToolCallback，
	// 内部转成 List 后复用同一份实现，避免逻辑重复。
	public static List<String> getDuplicateToolNames(ToolCallback... toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		return getDuplicateToolNames(Arrays.asList(toolCallbacks));
	}

	/**
	 * Validates that a tool name follows recommended naming conventions. Logs a warning
	 * if the tool name contains characters that may not be compatible with some LLMs.
	 * @param toolName the tool name to validate
	 */
	// 中文：校验工具名是否符合推荐命名规范。
	// 重要：这是"软校验" —— 不合规只打 warn 日志，不抛异常、不阻断，
	// 因为部分模型其实能接受特殊字符，框架不做强制限制以保持兼容性。
	private static void validateToolName(String toolName) {
		Assert.hasText(toolName, "Tool name cannot be null or empty");
		// 中文：先判断 isWarnEnabled 再做正则匹配，避免日志级别未开启时白白执行正则，属于性能优化写法。
		if (logger.isWarnEnabled() && !RECOMMENDED_NAME_PATTERN.matcher(toolName).matches()) {
			logger.warn("Tool name '" + toolName + "' may not be compatible with some LLMs (e.g., OpenAI). "
					+ "Consider using only alphanumeric characters, underscores, hyphens, and dots.");
		}
	}

}
