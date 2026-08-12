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

package org.springframework.ai.aot;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

/**
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of the {@link Tool}
 * annotation on methods and creates the required reflection hints.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
/**
 * 【中文说明】面向 Bean 的 AOT 处理器：自动为含 {@link Tool @Tool} 方法的 Bean 登记反射提示。
 *
 * <p>
 * 用途：与手写死清单的 {@code RuntimeHintsRegistrar} 不同，本类实现
 * {@link BeanRegistrationAotProcessor}，在 AOT 构建期<b>逐个遍历容器中的每个 Bean</b>，
 * 动态判断它是否定义了工具方法；只有命中的 Bean 才会被登记反射提示，从而精准控制原生镜像体积。
 *
 * <p>
 * 工作流程：
 * <ol>
 * <li>{@link #processAheadOfTime(RegisteredBean)}：检测 Bean 类上是否存在 {@code @Tool} 方法。</li>
 * <li>命中则返回一个 {@code AotContribution}；未命中返回 {@code null}（表示本处理器对该 Bean 无贡献）。</li>
 * <li>框架随后回调 {@code AotContribution#applyTo}，真正写入反射提示。</li>
 * </ol>
 *
 * <p>
 * 注意：本类是包级私有（package-private）的，仅供框架内部通过
 * {@code META-INF/spring/aot.factories} 装配使用。
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
class ToolBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	// 返回值可为 null：null 表示“此 Bean 与工具调用无关，无需生成任何 AOT 贡献”
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		// TYPE_HIERARCHY 策略：沿类型层次结构（父类与接口）搜索注解，
		// 这样定义在接口方法或父类方法上的 @Tool 也能被发现
		MergedAnnotations.Search search = MergedAnnotations
			.search(org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);

		// anyMatch 短路求值：只要发现一个 @Tool 方法即可停止扫描
		boolean hasAnyToolAnnotatedMethods = Stream.of(ReflectionUtils.getDeclaredMethods(beanClass))
			.anyMatch(method -> search.from(method).isPresent(Tool.class));

		// 命中才创建贡献对象，实现“按需登记”
		if (hasAnyToolAnnotatedMethods) {
			return new AotContribution(beanClass);
		}

		// 未命中：明确返回 null，框架会跳过本处理器
		return null;
	}

	// 私有静态内部类：承载单个工具类的反射登记逻辑，与外部处理器解耦
	private static class AotContribution implements BeanRegistrationAotContribution {

		// 仅开放方法调用相关的成员类别（声明方法 + public 方法），
		// 相比 MemberCategory.values() 的全量开放更克制，可减小原生镜像体积
		private final MemberCategory[] memberCategories = new MemberCategory[] { MemberCategory.INVOKE_DECLARED_METHODS,
				MemberCategory.INVOKE_PUBLIC_METHODS };

		// 待登记的工具类（即持有 @Tool 方法的 Bean 类）
		private final Class<?> toolClass;

		AotContribution(Class<?> toolClass) {
			this.toolClass = toolClass;
		}

		@Override
		// 由 AOT 引擎回调：把该工具类的方法反射提示写入生成上下文
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
			// 登记后，运行时才能通过反射调用 @Tool 标注的方法
			reflectionHints.registerType(this.toolClass, this.memberCategories);
		}

	}

}
