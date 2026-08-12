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

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Utility class for manipulating {@link ModelOptions} objects.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author chabinhwang
 * @author Sebastien Deleuze
 * @since 0.8.0
 */
/**
 * 【中文说明】ModelOptionsUtils 是操作 {@link ModelOptions} 的工具类，
 * 当前只提供一个核心能力：<b>选项合并</b>。
 *
 * <p>
 * 解决的问题：Spring AI 中模型选项存在两个层级——构造模型客户端时配置的"默认选项"
 * （defaultOptions）和单次请求携带的"运行时选项"（runtime options）。
 * 二者需要按"运行时优先、缺失则回退默认值"的规则逐项合并，本类就封装了这条规则。
 *
 * <p>
 * 关键设计：声明为 {@code abstract class} 而非 final class + 私有构造器，
 * 这是 Spring 生态惯用的工具类写法——抽象类同样无法被直接实例化，
 * 同时保留了子类扩展的可能性。
 *
 * <p>
 * 典型用法：各厂商的 Options 合并逻辑中逐字段调用，例如
 * {@code merged.setTemperature(ModelOptionsUtils.mergeOption(runtime.getTemperature(), defaults.getTemperature()));}
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author chabinhwang
 * @author Sebastien Deleuze
 * @since 0.8.0
 */
public abstract class ModelOptionsUtils {

	/**
	 * Return the runtime value if not null, or else the default value.
	 */
	// 【中文】合并单个选项值：运行时值非 null 就用它，否则回退到默认值。
	//
	// 关于 @Contract("_, !null -> !null")：这是 Spring 提供的空安全契约注解，
	// 含义是"第一个参数任意（_），若第二个参数非 null，则返回值一定非 null"。
	// 它让 IDE / 静态分析器能推断出：当默认值确定存在时，合并结果无需再判空，
	// 从而减少调用处冗余的空检查告警。
	//
	// 注意语义边界：判定依据是 null 而非"是否为空/是否为零"，
	// 因此运行时显式设置的 0、false、空串都会<b>覆盖</b>默认值，这是符合预期的。
	@Contract("_, !null -> !null")
	public static <T> @Nullable T mergeOption(@Nullable T runtimeValue, @Nullable T defaultValue) {
		return runtimeValue == null ? defaultValue : runtimeValue;
	}

}
