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

package org.springframework.ai.audio.tts;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of the {@link TextToSpeechOptions} interface.
 *
 * @author Alexandros Pappas
 * @author Sebastien Deleuze
 */
/**
 * {@link TextToSpeechOptions} 的默认实现，是一个不可变的选项值对象。
 * <p>
 * 设计要点：
 * <ul>
 * <li>四个字段全部为 {@code final}，构造后不可变，天然线程安全，可安全地作为默认配置被多次复用。</li>
 * <li>构造方法为 {@code protected}，鼓励通过 {@link Builder} 创建；同时允许子类继承扩展厂商专有参数。</li>
 * <li>实现了 {@code equals}/{@code hashCode}，具备值相等语义，便于比较与作为缓存键。</li>
 * <li>字段允许为 null，语义为「不指定该参数」，交由服务端使用默认值。</li>
 * </ul>
 * 典型用法：{@code DefaultTextToSpeechOptions.builder().model("tts-1").voice("alloy").build()}
 */
public class DefaultTextToSpeechOptions implements TextToSpeechOptions {

	// 模型名称
	private final @Nullable String model;

	// 音色标识
	private final @Nullable String voice;

	// 音频输出格式
	private final @Nullable String format;

	// 语速倍率
	private final @Nullable Double speed;

	// protected 构造方法：供 Builder 与子类使用；四个参数均可为 null，表示不指定
	protected DefaultTextToSpeechOptions(@Nullable String model, @Nullable String voice, @Nullable String format,
			@Nullable Double speed) {
		this.model = model;
		this.voice = voice;
		this.format = format;
		this.speed = speed;
	}

	// 静态工厂：创建构建器（与接口的 TextToSpeechOptions.builder() 等价，但返回具体类型）
	public static Builder builder() {
		return new Builder();
	}

	// 返回模型名称，可能为 null
	@Override
	public @Nullable String getModel() {
		return this.model;
	}

	// 返回音色标识，可能为 null
	@Override
	public @Nullable String getVoice() {
		return this.voice;
	}

	// 返回音频输出格式，可能为 null
	@Override
	public @Nullable String getFormat() {
		return this.format;
	}

	// 返回语速倍率，可能为 null
	@Override
	public @Nullable Double getSpeed() {
		return this.speed;
	}

	// 值相等语义：四个字段全部相等才视为同一份配置
	@Override
	public boolean equals(@Nullable Object o) {
		// 同一引用，快速返回
		if (this == o) {
			return true;
		}
		// 注意此处判断的是具体类型 DefaultTextToSpeechOptions，
		// 因此与其它 TextToSpeechOptions 实现（即使字段相同）比较时不相等
		if (!(o instanceof DefaultTextToSpeechOptions that)) {
			return false;
		}
		// 字段可能为 null，统一用 Objects.equals 做空安全比较
		return Objects.equals(this.model, that.model) && Objects.equals(this.voice, that.voice)
				&& Objects.equals(this.format, that.format) && Objects.equals(this.speed, that.speed);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.voice, this.format, this.speed);
	}

	/**
	 * {@link DefaultTextToSpeechOptions} 的构建器，实现自 {@link TextToSpeechOptions.Builder}。
	 * <p>
	 * 各 setter 的返回类型被收窄为具体的 {@code Builder}（协变返回类型），
	 * 这样链式调用过程中不会退化成父接口类型，便于后续调用子类特有方法。
	 * <p>
	 * 未设置的字段保持 null，表示「不指定」。
	 */
	public static final class Builder implements TextToSpeechOptions.Builder {

		// 模型名称，未设置则为 null
		private @Nullable String model;

		// 音色标识，未设置则为 null
		private @Nullable String voice;

		// 音频输出格式，未设置则为 null
		private @Nullable String format;

		// 语速倍率，未设置则为 null
		private @Nullable Double speed;

		// 公开无参构造：从零开始构建一份全新配置
		public Builder() {
		}

		// 私有拷贝构造：以已有选项为模板预填充各字段，用于「基于默认配置做局部覆盖」的场景
		private Builder(DefaultTextToSpeechOptions options) {
			this.model = options.model;
			this.voice = options.voice;
			this.format = options.format;
			this.speed = options.speed;
		}

		// 设置模型名称；返回具体 Builder 类型以支持链式调用
		@Override
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		// 设置音色标识
		@Override
		public Builder voice(String voice) {
			this.voice = voice;
			return this;
		}

		// 设置音频输出格式
		@Override
		public Builder format(String format) {
			this.format = format;
			return this;
		}

		// 设置语速倍率
		@Override
		public Builder speed(Double speed) {
			this.speed = speed;
			return this;
		}

		// 构建不可变选项对象；每次调用都会创建新实例，因此同一构建器可安全地多次 build
		public DefaultTextToSpeechOptions build() {
			return new DefaultTextToSpeechOptions(this.model, this.voice, this.format, this.speed);
		}

	}

}
