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

package org.springframework.ai.chat.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * A template for creating prompts. It allows you to define a template string with
 * placeholders for variables, and then render the template with specific values for those
 * variables.
 */
/**
 * 提示词模板类，用于构建提示词模板。
 * 支持定义带有变量占位符的模板字符串，并可传入具体变量值渲染生成最终模板内容。
 */
/**
 * 【中文补充说明】PromptTemplate 是提示词工程的核心工具类：把"固定话术"与"动态变量"分离。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code template}——模板原文，含 {@code {变量名}} 形式的占位符；</li>
 * <li>{@code variables}——预置变量表（可在构造时给定，也可用 {@link #add} 追加）；</li>
 * <li>{@code renderer}——渲染引擎，默认使用基于 StringTemplate 的 {@code StTemplateRenderer}，
 * 采用策略模式，可替换为其它模板语法实现。</li>
 * </ul>
 *
 * <p>
 * 实现了两个"能力接口"：{@link PromptTemplateActions}（产出 {@link Prompt}）与
 * {@link PromptTemplateMessageActions}（产出 {@link Message}），
 * 因此既能 {@code render()} 出字符串，也能直接 {@code create()} 出可发送的 Prompt。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * PromptTemplate t = new PromptTemplate("请把下面内容翻译成{lang}：{text}");
 * Prompt p = t.create(Map.of("lang", "英文", "text", "你好"));
 * }</pre>
 *
 * <p>
 * 特色能力：变量值若是 Spring 的 {@link Resource}，渲染时会自动读取其文件内容再填入
 * （见 {@link #renderResource}），便于把长提示词放在外部文件中维护。
 */
public class PromptTemplate implements PromptTemplateActions, PromptTemplateMessageActions {

	// 【中文】日志器，仅用于渲染 Resource 失败时输出告警。
	private static final Log log = LogFactory.getLog(PromptTemplate.class);

	// 【中文】默认渲染引擎：StringTemplate（ST）实现，占位符语法为 {variable}。
	// 声明为 static final 常量，全局共享一个实例（渲染器无状态，可安全复用）。
	private static final TemplateRenderer DEFAULT_TEMPLATE_RENDERER = StTemplateRenderer.builder().build();

	/**
	 * If you're subclassing this class, re-consider using the built-in implementation
	 * together with the new PromptTemplateRenderer interface, designed to give you more
	 * flexibility and control over the rendering process.
	 */
	/**
	 * 若需要继承此类，建议优先结合内置实现与新增的PromptTemplateRenderer接口使用，
	 * 该接口能够让开发者在模板渲染过程中拥有更高的灵活度与管控权限。
	 */
	// 【中文】模板原文（含占位符），final 不可变。
	private final String template;

	// 【中文】预置变量表。注意声明处即初始化为 HashMap，构造器中用 putAll 灌入，
	// 而非直接引用外部传入的 Map——这是防御性拷贝，避免外部修改影响本对象。
	// 但字段本身只是 final 引用，内容仍可通过 add() 变更，因此本类并非严格不可变。
	private final Map<String, Object> variables = new HashMap<>();

	// 【中文】模板渲染引擎（策略模式）。
	private final TemplateRenderer renderer;

	// 【中文】便捷构造器：从 Spring Resource（如 classpath 下的 .st/.txt 文件）加载模板。
	public PromptTemplate(Resource resource) {
		this(resource, new HashMap<>(), DEFAULT_TEMPLATE_RENDERER);
	}

	// 【中文】便捷构造器：直接传入模板字符串，使用默认渲染器、无预置变量（最常用）。
	public PromptTemplate(String template) {
		this(template, new HashMap<>(), DEFAULT_TEMPLATE_RENDERER);
	}

	// 【中文】完整构造器（字符串版），包级私有：外部应使用上面的便捷构造器或 Builder。
	PromptTemplate(String template, Map<String, Object> variables, TemplateRenderer renderer) {
		// 【中文】模板必须非空白：空模板毫无意义，直接快速失败。
		Assert.hasText(template, "template cannot be null or empty");
		Assert.notNull(variables, "variables cannot be null");
		// 【中文】变量名（key）不能为 null——null 键无法与占位符匹配，属于必然的使用错误。
		// 注意只校验 key，value 允许为 null。
		Assert.noNullElements(variables.keySet(), "variables keys cannot be null");
		Assert.notNull(renderer, "renderer cannot be null");

		this.template = template;
		this.variables.putAll(variables);
		this.renderer = renderer;
	}

	// 【中文】完整构造器（Resource 版）：从外部资源读取模板内容。
	PromptTemplate(Resource resource, Map<String, Object> variables, TemplateRenderer renderer) {
		Assert.notNull(resource, "resource cannot be null");
		Assert.notNull(variables, "variables cannot be null");
		Assert.noNullElements(variables.keySet(), "variables keys cannot be null");
		Assert.notNull(renderer, "renderer cannot be null");

		// 【中文】try-with-resources：InputStream 会在块结束时自动关闭，无需手写 finally。
		try (InputStream inputStream = resource.getInputStream()) {
			// 【中文】注意这里用的是 Charset.defaultCharset()（JVM 默认编码）而非固定 UTF-8，
			// 若运行环境默认编码不是 UTF-8，中文模板可能出现乱码——部署时值得留意。
			this.template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
			Assert.hasText(this.template, "template cannot be null or empty");
		}
		catch (IOException ex) {
			// 【中文】把受检异常 IOException 包装成非受检异常抛出，避免污染调用方的方法签名。
			throw new RuntimeException("Failed to read resource", ex);
		}
		this.variables.putAll(variables);
		this.renderer = renderer;
	}

	// 【中文】追加/覆盖一个模板变量（key 相同则覆盖）。
	public void add(String name, Object value) {
		this.variables.put(name, value);
	}

	// 【中文】返回未渲染的模板原文。
	public String getTemplate() {
		return this.template;
	}

	// From PromptTemplateStringActions.

	// 【中文】用预置变量渲染模板，返回最终字符串。
	@Override
	public String render() {
		// Process internal variables to handle Resources before rendering
		// 【中文】渲染前的预处理：把值为 Resource 的变量替换成其文件内容（文本），
		// 其余变量原样保留。这样模板里就能直接引用外部文件了。
		Map<String, @Nullable Object> processedVariables = new HashMap<>();
		for (Entry<String, Object> entry : this.variables.entrySet()) {
			if (entry.getValue() instanceof Resource resource) {
				processedVariables.put(entry.getKey(), renderResource(resource));
			}
			else {
				processedVariables.put(entry.getKey(), entry.getValue());
			}
		}
		// 【中文】真正的渲染动作委托给 renderer（策略对象）完成。
		return this.renderer.apply(this.template, processedVariables);
	}

	// 【中文】用"预置变量 + 临时追加变量"渲染模板。
	@Override
	public String render(Map<String, Object> additionalVariables) {
		Map<String, @Nullable Object> combinedVariables = new HashMap<>();
		// 【中文】先拷贝预置变量，再用传入的变量覆盖同名项——
		// 即"临时变量优先级高于预置变量"，且全程不修改 this.variables（本次渲染不留副作用）。
		Map<String, Object> mergedVariables = new HashMap<>(this.variables);
		// variables + additionalVariables => mergedVariables
		// 【中文】空值/空集合处理：传 null 或空 Map 时跳过合并，等价于只用预置变量渲染。
		if (additionalVariables != null && !additionalVariables.isEmpty()) {
			mergedVariables.putAll(additionalVariables);
		}

		// 【中文】与 render() 中相同的 Resource 展开逻辑。
		for (Entry<String, Object> entry : mergedVariables.entrySet()) {
			if (entry.getValue() instanceof Resource resource) {
				combinedVariables.put(entry.getKey(), renderResource(resource));
			}
			else {
				combinedVariables.put(entry.getKey(), entry.getValue());
			}
		}

		return this.renderer.apply(this.template, combinedVariables);
	}

	/**
	 * 【中文说明】把一个 {@link Resource} 读成字符串，供变量替换使用。
	 *
	 * <p>
	 * 该方法对各种异常情况都做了"降级"处理而非抛出异常：资源为 null 或为空返回空串，
	 * 读取失败则打告警日志并返回一段占位提示文本，避免因单个变量读取失败导致整次对话中断。
	 */
	private String renderResource(Resource resource) {
		// 【中文】空值保护（尽管调用点已用 instanceof 判断过，这里仍防御一次）。
		if (resource == null) {
			return "";
		}

		try {
			// Handle ByteArrayResource specially
			// 【中文】ByteArrayResource 单独处理：直接按 UTF-8 解码其字节数组，
			// 避免走下面 exists()/contentLength() 的通用分支（内存资源无需这些判断）。
			if (resource instanceof ByteArrayResource byteArrayResource) {
				return new String(byteArrayResource.getByteArray(), StandardCharsets.UTF_8);
			}
			// If the resource exists but is empty
			// 【中文】资源不存在或长度为 0 时返回空串。
			if (!resource.exists() || resource.contentLength() == 0) {
				return "";
			}
			// For other Resource types or as fallback
			// 【中文】其它类型资源的通用读取路径，此处明确指定 UTF-8（与构造器中用默认编码不同）。
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			// 【中文】读取失败不抛异常，而是记录告警 + 返回提示文本，保证渲染流程能继续走完。
			if (log.isWarnEnabled()) {
				log.warn("Failed to render resource: " + resource.getDescription(), e);
			}
			return "[Unable to render resource: " + resource.getDescription() + "]";
		}
	}

	// From PromptTemplateMessageActions.

	// 【中文】渲染后包装成一条 UserMessage（用户消息）。
	// 注意：本类默认产出的都是 UserMessage，子类 SystemPromptTemplate/AssistantPromptTemplate
	// 会重写这些方法以产出对应角色的消息。
	@Override
	public Message createMessage() {
		return new UserMessage(render());
	}

	// 【中文】渲染成带多媒体附件（图片、音频等）的多模态用户消息。
	@Override
	public Message createMessage(List<Media> mediaList) {
		return UserMessage.builder().text(render()).media(mediaList).build();
	}

	// 【中文】用临时变量渲染后包装成用户消息。
	@Override
	public Message createMessage(Map<String, Object> additionalVariables) {
		return new UserMessage(render(additionalVariables));
	}

	// From PromptTemplateActions.

	// 【中文】渲染后直接产出可发送的 Prompt。
	// 传入 new HashMap<>() 而非调用无参 render()，二者效果等价（空的追加变量表）。
	@Override
	public Prompt create() {
		return new Prompt(render(new HashMap<>()));
	}

	// 【中文】渲染并附带模型参数产出 Prompt。
	@Override
	public Prompt create(ChatOptions modelOptions) {
		return Prompt.builder().content(render(new HashMap<>())).chatOptions(modelOptions).build();
	}

	// 【中文】用临时变量渲染产出 Prompt（最常用的一个重载）。
	@Override
	public Prompt create(Map<String, Object> additionalVariables) {
		return new Prompt(render(additionalVariables));
	}

	// 【中文】用临时变量渲染 + 指定模型参数，产出 Prompt。
	@Override
	public Prompt create(Map<String, Object> additionalVariables, ChatOptions modelOptions) {
		return Prompt.builder().content(render(additionalVariables)).chatOptions(modelOptions).build();
	}

	// 【中文】以当前模板为基础生成一个预填充的 Builder，便于复制后局部修改（如只换渲染器）。
	public Builder mutate() {
		return new Builder().template(this.template).variables(this.variables).renderer(this.renderer);
	}

	// Builder

	// 【中文】获取空白 Builder 的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@link PromptTemplate} 的建造者。
	 *
	 * <p>
	 * <b>互斥约束</b>：{@code template}（模板字符串）与 {@code resource}（模板文件）
	 * 两者**只能设置其一**，且必须设置其一，否则 {@link #build()} 会抛异常（详见该方法内注释）。
	 *
	 * <p>
	 * 字段均为 {@code protected}、类未声明 final，因此子类（如 SystemPromptTemplate.Builder）
	 * 可以继承并扩展。
	 */
	public static class Builder {

		// 【中文】模板字符串，与下面的 resource 互斥。
		protected @Nullable String template;

		// 【中文】模板资源文件，与上面的 template 互斥。
		protected @Nullable Resource resource;

		// 【中文】预置变量，默认空 Map。
		protected Map<String, Object> variables = new HashMap<>();

		// 【中文】渲染器，默认使用 ST 实现。
		protected TemplateRenderer renderer = DEFAULT_TEMPLATE_RENDERER;

		// 【中文】构造器为 protected：外部通过 builder() 获取，子类可通过 super() 调用。
		protected Builder() {
		}

		// 【中文】设置模板字符串，入参处即校验非空白（尽早失败，便于定位问题）。
		public Builder template(String template) {
			Assert.hasText(template, "template cannot be null or empty");
			this.template = template;
			return this;
		}

		// 【中文】设置模板资源文件。
		public Builder resource(Resource resource) {
			Assert.notNull(resource, "resource cannot be null");
			this.resource = resource;
			return this;
		}

		// 【中文】设置预置变量（覆盖语义：整体替换，而非合并）。
		// 注意此处直接引用外部 Map，未做拷贝——不过 PromptTemplate 构造器里会 putAll 到自己的 Map，
		// 因此最终对象仍是安全的。
		public Builder variables(Map<String, Object> variables) {
			Assert.notNull(variables, "variables cannot be null");
			Assert.noNullElements(variables.keySet(), "variables keys cannot be null");
			this.variables = variables;
			return this;
		}

		// 【中文】替换渲染器，可用于改变占位符语法。
		public Builder renderer(TemplateRenderer renderer) {
			Assert.notNull(renderer, "renderer cannot be null");
			this.renderer = renderer;
			return this;
		}

		// 【中文】完成构建，这里集中处理 template / resource 的**互斥约束**。
		public PromptTemplate build() {
			// 【中文】两者都设置了：语义冲突（到底以哪个为准？），直接报错。
			if (this.template != null && this.resource != null) {
				throw new IllegalArgumentException("Only one of template or resource can be set");
			}
			// 【中文】只设置了 resource：走资源版构造器。
			else if (this.resource != null) {
				return new PromptTemplate(this.resource, this.variables, this.renderer);
			}
			// 【中文】只设置了 template：走字符串版构造器。
			else if (this.template != null) {
				return new PromptTemplate(this.template, this.variables, this.renderer);
			}
			else {
				// 【中文】两者都没设置：构建器状态不完整，抛 IllegalStateException
				// （区别于上面参数冲突时抛的 IllegalArgumentException，异常类型选择很讲究）。
				throw new IllegalStateException("Neither template nor resource is set");
			}
		}

	}

}
