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

package org.springframework.ai.util.json.schema;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.springframework.core.KotlinDetector;
import org.springframework.core.Nullness;

/**
 * Abstract base for JSON Schema Generator Modules in Spring AI.
 * <p>
 * Provides shared logic for description resolution and required-field determination,
 * delegating annotation-specific lookups to subclasses via
 * {@link #resolveToolParamDescription} and {@link #resolveToolParamRequired}.
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】Spring AI 的 JSON Schema 生成模块抽象基类（模板方法模式）。
 *
 * <p>
 * 用途：作为 victools jsonschema-generator 的 {@link Module} 实现，向生成器注入两条自定义规则：
 * <ol>
 * <li><b>描述解析</b>：从工具参数注解中提取字段的 description，写入 Schema。</li>
 * <li><b>必填判定</b>：综合多种注解与空安全信息，决定字段是否进入 Schema 的 {@code required} 列表。</li>
 * </ol>
 *
 * <p>
 * 设计模式：典型的<b>模板方法</b>——本类固化了通用流程（{@link #checkRequired} 的多级判定顺序），
 * 而把“具体读哪个注解”下放给子类的两个抽象方法：{@link #resolveToolParamDescription}
 * 与 {@link #resolveToolParamRequired}。这样不同来源的工具参数注解可以复用同一套判定逻辑。
 *
 * <p>
 * 关键字段：{@code requiredByDefault} —— 当所有注解都未表态时的兜底策略。默认为 true（字段必填），
 * 除非构造时传入 {@link Option#PROPERTY_REQUIRED_FALSE_BY_DEFAULT}。
 *
 * <p>
 * 典型用法：由 {@code SpringAiSchemaModule} 等子类继承，再注册到
 * {@code SchemaGeneratorConfigBuilder} 中。
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
public abstract class AbstractSpringAiSchemaModule implements Module {

	// 兜底策略：无任何注解表态时，字段是否视为必填
	private final boolean requiredByDefault;

	// 构造器：用可变参数接收选项。noneMatch 的含义是——
	// 只要没传 PROPERTY_REQUIRED_FALSE_BY_DEFAULT，requiredByDefault 就为 true（默认必填）
	protected AbstractSpringAiSchemaModule(Option... options) {
		this.requiredByDefault = Stream.of(options)
			.noneMatch(option -> option == Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);
	}

	@Override
	// Module 接口入口：由生成器回调。注意这里只对「字段」生效（forFields），不处理 getter 方法
	public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
		this.applyToConfigBuilder(builder.forFields());
	}

	// 私有同名重载：把两个自定义解析器挂载到字段配置部分
	private void applyToConfigBuilder(SchemaGeneratorConfigPart<FieldScope> configPart) {
		// 注册描述解析器（方法引用形式的回调）
		configPart.withDescriptionResolver(this::resolveDescription);
		// 注册必填判定器
		configPart.withRequiredCheck(this::checkRequired);
	}

	/**
	 * Extract the description from the tool-param annotation for the given member, or
	 * return {@code null} if the annotation is absent or its description is blank.
	 */
	// 抽象钩子一：由子类决定从哪个注解读取描述；无注解或描述为空白时返回 null
	protected abstract @Nullable String resolveToolParamDescription(MemberScope<?, ?> member);

	/**
	 * Extract the required flag from the tool-param annotation for the given member, or
	 * return {@code null} if the annotation is absent.
	 */
	// 抽象钩子二：由子类读取注解上的 required 标志。
	// 返回 Boolean 包装类型而非基本类型，是为了用 null 表达“该注解不存在/未表态”这第三种状态
	protected abstract @Nullable Boolean resolveToolParamRequired(MemberScope<?, ?> member);

	// 描述解析：目前只是直接转调抽象钩子，保留此层是为了后续扩展（如加入回退策略）
	private @Nullable String resolveDescription(MemberScope<?, ?> member) {
		return resolveToolParamDescription(member);
	}

	/**
	 * Determines whether a property is required based on the presence of a series of
	 * annotations.
	 * <p>
	 * <ul>
	 * <li>tool-param annotation ({@code required = ...})</li>
	 * <li>{@code @JsonProperty(required = ...)}</li>
	 * <li>{@code @Schema(required = ...)}</li>
	 * <li>{@code @Nullable}</li>
	 * </ul>
	 * <p>
	 * If none of these annotations are present, the default behavior is to consider the
	 * property as required, unless the {@link Option#PROPERTY_REQUIRED_FALSE_BY_DEFAULT}
	 * option is set.
	 */
	@SuppressWarnings("deprecation") // Schema.required() kept for backwards compatibility
										// with pre-requiredMode usages
	// 必填判定核心：按优先级从高到低依次检查多种“信号源”，一旦某级给出明确答案立即返回
	private boolean checkRequired(MemberScope<?, ?> member) {
		// 优先级 1：工具参数注解（子类提供），显式声明最优先
		Boolean toolParamRequired = resolveToolParamRequired(member);
		if (toolParamRequired != null) {
			return toolParamRequired;
		}

		// 优先级 2：Jackson 的 @JsonProperty(required = ...)
		// getAnnotationConsideringFieldAndGetter 会同时查看字段与其 getter，避免注解写在 getter 上时漏判
		var propertyAnnotation = member.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
		if (propertyAnnotation != null) {
			return propertyAnnotation.required();
		}

		// 优先级 3：Swagger 的 @Schema
		var schemaAnnotation = member.getAnnotationConsideringFieldAndGetter(Schema.class);
		if (schemaAnnotation != null) {
			// 三个条件取或：新版的 requiredMode 为 REQUIRED 或 AUTO，或旧版已废弃的 required() 为 true。
			// 兼容新旧两套 API 的写法，这也是方法上 @SuppressWarnings("deprecation") 的由来
			return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
					|| schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
		}

		// 优先级 4：空安全信息（@Nullable / @NullMarked 等），借助 Spring 的 Nullness 工具统一解析
		Nullness nullness;
		// 字段成员：读取字段上的可空性
		if (member instanceof FieldScope fs) {
			nullness = Nullness.forField(fs.getRawMember());
		}
		// 方法成员：读取返回值的可空性
		else if (member instanceof MethodScope ms) {
			nullness = Nullness.forMethodReturnType(ms.getRawMember());
		}
		else {
			// 防御性分支：MemberScope 目前只有上述两种实现，走到这里说明库升级引入了新类型
			throw new IllegalStateException("Unsupported member type: " + member);
		}
		// 可空即非必填
		if (nullness == Nullness.NULLABLE) {
			return false;
		}
		// Kotlin 特例：Kotlin 有自己的可空性与默认参数语义，
		// 这里返回 false 让专门的 KotlinModule 去做更准确的判断，避免两边冲突
		if (KotlinDetector.isKotlinReflectPresent()
				&& KotlinDetector.isKotlinType(member.getDeclaringType().getErasedType())) {
			// Defer to KotlinModule for additional checks like default values
			return false;
		}

		// 所有信号源都未表态，使用构造时确定的兜底策略
		return this.requiredByDefault;
	}

	/**
	 * Options for customizing the behavior of the module.
	 */
	/**
	 * 【中文说明】模块行为的可选开关枚举，通过构造器的可变参数传入。
	 *
	 * <p>
	 * 目前仅一个常量，用于翻转“必填”的兜底默认值。
	 */
	public enum Option {

		/**
		 * Properties are only required if marked as such via one of the supported
		 * annotations.
		 */
		// 开启后：字段默认非必填，只有被注解显式标记时才必填（即反转兜底策略）
		PROPERTY_REQUIRED_FALSE_BY_DEFAULT

	}

}
