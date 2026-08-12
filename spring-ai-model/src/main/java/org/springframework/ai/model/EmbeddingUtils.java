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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for embedding related operations.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */

/**
 * 【中文说明】EmbeddingUtils 是嵌入向量相关的<b>类型转换工具类</b>。
 *
 * <p>
 * 存在意义：不同厂商 SDK 对向量的表示各不相同——有的返回 {@code List<Double>}（JSON 解析默认），
 * 有的返回 {@code Float[]} 装箱数组，而 Spring AI 内部统一使用 {@code float[]} 原始类型数组
 * （内存占用小、无装箱开销、便于做相似度计算）。本类就负责在这些表示之间互相转换。
 *
 * <p>
 * 关键设计：
 * <ul>
 * <li>类声明为 {@code final} + 私有构造器，这是标准的<b>工具类</b>写法，
 * 禁止被继承和实例化，所有方法均为 static；</li>
 * <li>{@link #EMPTY_FLOAT_ARRAY} 常量：空数组是不可变的，可安全共享，
 * 避免每次遇到空输入都 new 一个对象，属于常见的性能优化技巧。</li>
 * </ul>
 *
 * <p>
 * 典型用法：{@code float[] vec = EmbeddingUtils.toPrimitive(response.getFloats());}
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public final class EmbeddingUtils {

	// 【中文】共享的空 float 数组常量。空数组本身不可变，多处复用同一实例是安全的，
	// 可减少无谓的对象分配。
	private static final float[] EMPTY_FLOAT_ARRAY = new float[0];

	// 【中文】私有构造器：阻止外部实例化本工具类（配合 final 类修饰符，构成标准工具类范式）。
	private EmbeddingUtils() {

	}

	// 【中文】把 List<Double> 转成 List<Float>。
	// 场景：JSON 反序列化时数字默认会被解析为 Double，而向量只需 float 精度，
	// 转换后可减半内存占用。注意 double -> float 是<b>窄化转换</b>，会损失精度，
	// 但对嵌入向量的相似度计算影响可忽略。
	public static List<Float> doubleToFloat(final List<Double> doubles) {
		return doubles.stream().map(f -> f.floatValue()).toList();
	}

	// 【中文】把 List<Float>（装箱）转成 float[]（原始类型数组）。
	public static float[] toPrimitive(List<Float> floats) {
		// 空值处理：null 或空列表统一返回共享的空数组，避免返回 null 给调用方带来判空负担
		if (floats == null || floats.isEmpty()) {
			return EMPTY_FLOAT_ARRAY;
		}
		final float[] result = new float[floats.size()];
		// 手写 for 循环而非 Stream：此处每个元素都会自动拆箱（Float -> float），
		// 循环写法性能更好，且避免了 Stream 的额外开销
		for (int i = 0; i < result.length; i++) {
			result[i] = floats.get(i);
		}
		return result;
	}

	// 【中文】把 Float[]（装箱数组）转成 float[]。与上面的方法构成<b>重载</b>，
	// 因为 List<Float> 和 Float[] 是两种不同的输入形态，需分别处理。
	public static float[] toPrimitive(final Float[] array) {
		// 空值处理：同样对 null 和空数组做兜底
		if (array == null || array.length == 0) {
			return EMPTY_FLOAT_ARRAY;
		}
		final float[] result = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			// 显式调用 floatValue() 拆箱，语义比依赖自动拆箱更清晰
			result[i] = array[i].floatValue();
		}
		return result;
	}

	// 【中文】反向转换：float[] -> Float[]（装箱）。
	// 场景：某些第三方 API 或泛型容器只接受对象类型而不接受原始类型数组。
	public static Float[] toFloatArray(final float[] array) {
		// 空值处理：注意这里每次 new Float[0] 而非复用常量，
		// 因为本类只为 float[] 定义了共享空数组常量
		if (array == null || array.length == 0) {
			return new Float[0];
		}
		final Float[] result = new Float[array.length];
		for (int i = 0; i < array.length; i++) {
			// 此处发生自动装箱（float -> Float）
			result[i] = array[i];
		}
		return result;
	}

	// 【中文】把 float[] 转成 List<Float>。
	// 注意：这里未做 null 判断，传入 null 会在 for-each 处抛 NullPointerException，
	// 与上面几个方法的宽松风格不同，调用时需自行保证入参非空。
	public static List<Float> toList(float[] floats) {
		List<Float> output = new ArrayList<>();
		// 增强 for 循环遍历，逐个自动装箱后加入列表
		for (float value : floats) {
			output.add(value);
		}
		return output;
	}

}
