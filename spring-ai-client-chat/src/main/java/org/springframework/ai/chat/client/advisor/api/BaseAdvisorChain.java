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

package org.springframework.ai.chat.client.advisor.api;

import java.util.List;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain;

/**
 * A base interface for advisor chains that can be used to chain multiple advisors
 * together, both for call and stream advisors.
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 顾问链的基础接口，可将多个顾问串联执行，同时支持普通调用与流式调用顾问。
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
public interface BaseAdvisorChain extends CallAdvisorChain, StreamAdvisorChain {

	/**
	 * Returns a new {@link Builder} initialized with this chain's advisors and
	 * configuration, allowing it to be selectively modified before building a new chain.
	 *
	 * <p>
	 * Concrete {@link BaseAdvisorChain} classes must override this to return the most
	 * concrete builder implementation.
	 * @return a pre-populated {@link Builder}
	 */
	/**
	 * 返回一个使用当前调用链的顾问与配置初始化的全新 {@link Builder}，
	 * 支持在构建新调用链之前对其进行选择性修改。
	 *
	 * <p>
	 * {@link BaseAdvisorChain} 的具体实现类必须重写该方法，返回对应最具体的构建器实现。
	 * @return 已填充数据的 {@link Builder}
	 */
	// TODO: change from default to abstract once all implementations override mutate()
	// TODO: 待全部实现类重写 mutate() 方法后，将该方法从 default 修改为 abstract
	default Builder<?> mutate() {
		throw new UnsupportedOperationException("mutate() must be overridden to return the most concrete Builder");
	}

	/**
	 * Creates a new {@link Builder} for the default {@link BaseAdvisorChain}
	 * implementation.
	 * @param observationRegistry the observation registry to use
	 * @return a new {@link Builder}
	 */
	/**
	 * 为默认的 {@link BaseAdvisorChain} 实现创建全新的 {@link Builder}。
	 * @param observationRegistry 要使用的观测注册表
	 * @return 新的 {@link Builder}
	 */
	static Builder<?> builder(ObservationRegistry observationRegistry) {
		return new DefaultAroundAdvisorChain.Builder(observationRegistry);
	}

	/**
	 * Builder for creating a {@link BaseAdvisorChain} instance.
	 *
	 * @param <B> the concrete builder type, enabling fluent subtype chaining
	 */
	/**
	 * 用于构建 {@link BaseAdvisorChain} 实例的构建器。
	 *
	 * @param <B> 具体构建器类型，支持流式子类链式调用
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * Adds a single {@link Advisor} to the chain.
		 * @param advisor the advisor to add; must not be null
		 * @return this builder
		 */
		/**
		 * 向调用链添加单个 {@link Advisor}。
		 * @param advisor 待添加的顾问；不可为 null
		 * @return 当前构建器实例
		 */
		B push(Advisor advisor);

		/**
		 * Adds multiple {@link Advisor} instances to the chain.
		 * @param advisors the advisors to add; must not be null or contain null elements
		 * @return this builder
		 */
		/**
		 * 向调用链添加多个 {@link Advisor} 实例。
		 * @param advisors 待添加的顾问集合；不能为 null，也不能包含 null 元素
		 * @return 当前构建器实例
		 */
		B pushAll(List<? extends Advisor> advisors);

		/**
		 * Builds and returns the {@link BaseAdvisorChain}.
		 * @return the constructed chain
		 */
		/**
		 * 构建并返回 {@link BaseAdvisorChain}。
		 * @return 构建完成的顾问链
		 */
		BaseAdvisorChain build();

	}

}
