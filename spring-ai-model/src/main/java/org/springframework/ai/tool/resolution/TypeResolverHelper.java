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

package org.springframework.ai.tool.resolution;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A utility class that provides methods for resolving types and classes related to
 * functions.
 *
 * @author Christian Tzolov
 * @author Sebastien Dekeuze
 */
/**
 * 【中文说明】类型解析工具类：借助 Spring 的 {@link ResolvableType}，从函数式接口的实现类中 <b>反推出被泛型擦除掉的入参/出参类型</b>。
 *
 * <p>
 * 为什么需要它？Java 泛型在运行时会被擦除，但框架必须知道 {@code Function<WeatherRequest, WeatherResponse>}
 * 的输入类型是 {@code WeatherRequest}， 才能为工具生成正确的入参 JSON Schema，并把模型返回的 JSON 反序列化成该类型。
 *
 * <p>
 * 主要能力分三类：
 * <ul>
 * <li>解析 {@link Function}/{@link BiFunction}/{@link Consumer}/{@link Supplier}
 * 的泛型参数类型；</li>
 * <li>{@link #resolveBeanType} 解析 Spring 容器中 Bean 的完整泛型类型（含工厂方法场景）；</li>
 * <li>通过内部类 {@code KotlinDelegate} 兼容 Kotlin 的 {@code Function0/1/2}。</li>
 * </ul>
 *
 * <p>
 * 设计约定：所有解析方法在<b>无法解析时返回 {@code Object.class} 兜底</b>，而不是抛异常 （{@link #getFunctionArgumentType}
 * 除外，它会抛 IllegalArgumentException）。
 *
 * @author Christian Tzolov
 * @author Sebastien Dekeuze
 */
public final class TypeResolverHelper {

	// 私有构造器 + final 类：标准的纯静态工具类写法，禁止实例化与继承
	private TypeResolverHelper() {
		// Avoids instantiation
	}

	/**
	 * Returns the input class of a given Consumer class.
	 * @param consumerClass The consumer class.
	 * @return The input class of the consumer.
	 */
	/**
	 * 【中文说明】取出 {@code Consumer<T>} 的入参类型 T。
	 * <p>
	 * {@code .as(Consumer.class)} 的作用是「向上转型到指定接口视角」，这样即使
	 * consumerClass 是多层继承而来的实现类，也能定位到 Consumer 上的泛型位置。
	 * @param consumerClass Consumer 的实现类
	 * @return 入参类型；无法解析时兜底返回 {@code Object.class}
	 */
	public static Class<?> getConsumerInputClass(Class<? extends Consumer<?>> consumerClass) {
		ResolvableType resolvableType = ResolvableType.forClass(consumerClass).as(Consumer.class);
		// ResolvableType.NONE 表示未能解析出该接口视角，此时降级为 Object
		return (resolvableType == ResolvableType.NONE ? Object.class : resolvableType.getGeneric(0).toClass());
	}

	/**
	 * Returns the input class of a given function class.
	 * @param biFunctionClass The function class.
	 * @return The input class of the function.
	 */
	/**
	 * 【中文说明】取出 {@code BiFunction<T, U, R>} 的第一个入参类型 T（索引 0）。
	 * @param biFunctionClass BiFunction 的实现类
	 * @return 第一个入参的类型
	 */
	public static Class<?> getBiFunctionInputClass(Class<? extends BiFunction<?, ?, ?>> biFunctionClass) {
		// 索引 0 = 第一个泛型参数 T
		return getBiFunctionArgumentClass(biFunctionClass, 0);
	}

	/**
	 * Returns the input class of a given function class.
	 * @param functionClass The function class.
	 * @return The input class of the function.
	 */
	/**
	 * 【中文说明】取出 {@code Function<T, R>} 的入参类型 T（索引 0）。 这是工具调用中最常用的方法——入参类型决定了生成给模型的 JSON
	 * Schema。
	 * @param functionClass Function 的实现类
	 * @return 入参类型
	 */
	public static Class<?> getFunctionInputClass(Class<? extends Function<?, ?>> functionClass) {
		// 索引 0 = 输入类型 T
		return getFunctionArgumentClass(functionClass, 0);
	}

	/**
	 * Returns the output class of a given function class.
	 * @param functionClass The function class.
	 * @return The output class of the function.
	 */
	/**
	 * 【中文说明】取出 {@code Function<T, R>} 的返回类型 R（索引 1）。
	 * @param functionClass Function 的实现类
	 * @return 返回值类型
	 */
	public static Class<?> getFunctionOutputClass(Class<? extends Function<?, ?>> functionClass) {
		// 索引 1 = 输出类型 R
		return getFunctionArgumentClass(functionClass, 1);
	}

	/**
	 * Retrieves the class of a specific argument in a given function class.
	 * @param functionClass The function class.
	 * @param argumentIndex The index of the argument whose class should be retrieved.
	 * @return The class of the specified function argument.
	 */
	/**
	 * 【中文说明】{@link Function} 泛型参数解析的通用实现，上面两个方法都委托到这里。
	 * @param functionClass Function 的实现类
	 * @param argumentIndex 泛型位置索引：0=输入 T，1=输出 R
	 * @return 指定位置的类型；无法解析时兜底返回 {@code Object.class}
	 */
	public static Class<?> getFunctionArgumentClass(Class<? extends Function<?, ?>> functionClass, int argumentIndex) {
		ResolvableType resolvableType = ResolvableType.forClass(functionClass).as(Function.class);
		// 解析失败降级为 Object.class，保证调用方不必处理 null
		return (resolvableType == ResolvableType.NONE ? Object.class
				: resolvableType.getGeneric(argumentIndex).toClass());
	}

	/**
	 * Retrieves the class of a specific argument in a given function class.
	 * @param biFunctionClass The function class.
	 * @param argumentIndex The index of the argument whose class should be retrieved.
	 * @return The class of the specified function argument.
	 */
	/**
	 * 【中文说明】{@link BiFunction} 泛型参数解析的通用实现。
	 * <p>
	 * 在工具调用中，BiFunction 常用于「第二个参数接收 ToolContext」的场景。
	 * @param biFunctionClass BiFunction 的实现类
	 * @param argumentIndex 泛型位置索引：0=第一入参 T，1=第二入参 U，2=返回值 R
	 * @return 指定位置的类型；无法解析时兜底返回 {@code Object.class}
	 */
	public static Class<?> getBiFunctionArgumentClass(Class<? extends BiFunction<?, ?, ?>> biFunctionClass,
			int argumentIndex) {
		ResolvableType resolvableType = ResolvableType.forClass(biFunctionClass).as(BiFunction.class);
		return (resolvableType == ResolvableType.NONE ? Object.class
				: resolvableType.getGeneric(argumentIndex).toClass());
	}

	/**
	 * Resolve bean type, either directly with {@link BeanDefinition#getResolvableType()}
	 * or by resolving the factory method (duplicating
	 * {@code ConstructorResolver#resolveFactoryMethodIfPossible} logic as it is not
	 * public).
	 * @param applicationContext The application context.
	 * @param beanName The name of the bean to find a definition for.
	 * @return The resolved type.
	 * @throws IllegalArgumentException if the type of the bean definition is not
	 * resolvable.
	 */
	/**
	 * 【中文说明】解析 Spring 容器中指定 Bean 的完整泛型类型（如
	 * {@code Function<WeatherRequest, WeatherResponse>}）。
	 *
	 * <p>
	 * 关键在于：<b>不能直接用 {@code getBean()} 拿实例再取 class</b>，那样泛型信息已被擦除， 而且会提前触发 Bean
	 * 初始化。因此这里从 {@link BeanDefinition} 层面解析。
	 *
	 * <p>
	 * 三级降级策略：
	 * <ol>
	 * <li>直接从 BeanDefinition 解析（适用于类型信息完整的情况）；</li>
	 * <li>若是 {@link RootBeanDefinition}，尝试通过<b>工厂方法</b>的返回类型推断
	 * （对应 {@code @Bean} 方法定义的函数式 Bean）；</li>
	 * <li>最后按 {@code @Component} 方式，用 beanClassName 反射加载类。</li>
	 * </ol>
	 *
	 * <p>
	 * 英文注释已说明：第 2 步复刻了 Spring 内部
	 * {@code ConstructorResolver#resolveFactoryMethodIfPossible} 的逻辑，因为该方法不是 public 的。
	 * @param applicationContext 应用上下文
	 * @param beanName 目标 Bean 名称
	 * @return 解析出的可解析类型
	 * @throws IllegalArgumentException Bean 不存在或类型无法解析时抛出
	 */
	public static ResolvableType resolveBeanType(GenericApplicationContext applicationContext, String beanName) {
		BeanDefinition beanDefinition = getBeanDefinition(applicationContext, beanName);

		// Try to resolve directly
		// 【中文】第一级：直接解析。resolve() 返回非 null 说明类型信息完整可用
		ResolvableType functionType = beanDefinition.getResolvableType();
		if (functionType.resolve() != null) {
			return functionType;
		}

		// Handle root bean definitions with factory methods
		// 【中文】第二级：处理由 @Bean 工厂方法定义的 Bean，需先定位唯一的工厂方法
		if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
			return resolveRootBeanDefinitionType(applicationContext, rootBeanDefinition);
		}

		// Handle @Component beans
		// 【中文】第三级：兜底，按类名反射加载（适用于 @Component 声明的函数式 Bean）
		return resolveComponentBeanType(applicationContext, beanDefinition, beanName);
	}

	/**
	 * 【中文说明】获取 BeanDefinition，并把 Spring 的 {@link NoSuchBeanDefinitionException}
	 * 转换成语义更明确的 {@link IllegalArgumentException}。
	 */
	private static BeanDefinition getBeanDefinition(GenericApplicationContext applicationContext, String beanName) {
		try {
			return applicationContext.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 异常转换：对调用方屏蔽 Spring 内部异常类型，给出更贴合业务语义的提示
			throw new IllegalArgumentException(
					"Functional bean with name " + beanName + " does not exist in the context.");
		}
	}

	/**
	 * 【中文说明】通过定位工厂方法来推断 Bean 的泛型类型。
	 * <p>
	 * 一旦调用 {@code setResolvedFactoryMethod} 设置了工厂方法， {@code getResolvableType()}
	 * 就能依据该方法的返回类型给出带泛型的完整类型。
	 */
	private static ResolvableType resolveRootBeanDefinitionType(GenericApplicationContext applicationContext,
			RootBeanDefinition rootBeanDefinition) {

		Class<?> factoryClass;
		boolean isStatic;

		// 区分两种工厂方法形态：
		// 1) 有 factoryBeanName —— 实例工厂方法（如配置类中的 @Bean 方法），需从容器取其类型
		if (rootBeanDefinition.getFactoryBeanName() != null) {
			factoryClass = applicationContext.getBeanFactory().getType(rootBeanDefinition.getFactoryBeanName());
			isStatic = false;
		}
		else {
			// 2) 无 factoryBeanName —— 静态工厂方法，工厂类就是 Bean 自身的类
			factoryClass = rootBeanDefinition.getBeanClass();
			isStatic = true;
		}

		Assert.state(factoryClass != null, "Unresolvable factory class");
		// getUserClass：剥离 CGLIB 代理类（名字含 $$），拿到原始用户类，
		// 否则在代理类上找不到真正的工厂方法
		factoryClass = ClassUtils.getUserClass(factoryClass);

		Method uniqueCandidate = findUniqueFactoryMethod(factoryClass, isStatic, rootBeanDefinition);
		// 把找到的工厂方法回填到 BeanDefinition，从而让下一行能解析出带泛型的类型
		rootBeanDefinition.setResolvedFactoryMethod(uniqueCandidate);
		return rootBeanDefinition.getResolvableType();
	}

	/**
	 * 【中文说明】在工厂类中查找<b>唯一匹配</b>的工厂方法。
	 * <p>
	 * 「唯一」是关键约束：如果存在多个同名但参数签名不同的重载方法（方法重载导致歧义）， 则无法确定用哪个来推断类型，此时返回 null 表示放弃解析。
	 * @return 唯一的工厂方法；存在歧义或未找到时返回 null
	 */
	private static @Nullable Method findUniqueFactoryMethod(Class<?> factoryClass, boolean isStatic,
			RootBeanDefinition rootBeanDefinition) {
		Method[] candidates = getCandidateMethods(factoryClass, rootBeanDefinition);
		Method uniqueCandidate = null;

		for (Method candidate : candidates) {
			// 双重过滤：静态工厂场景要求方法必须是 static，且方法名需与定义的工厂方法名匹配
			if ((!isStatic || isStaticCandidate(candidate, factoryClass))
					&& rootBeanDefinition.isFactoryMethod(candidate)) {
				if (uniqueCandidate == null) {
					// 首个命中，先暂存
					uniqueCandidate = candidate;
				}
				else if (isParamMismatch(uniqueCandidate, candidate)) {
					// 发现第二个签名不同的同名方法 —— 存在重载歧义，直接置空并终止
					uniqueCandidate = null;
					break;
				}
			}
		}

		return uniqueCandidate;
	}

	/**
	 * 【中文说明】兜底解析：针对 {@code @Component} 之类「直接由类定义」的 Bean， 通过类名反射加载出 Class 再包装为
	 * ResolvableType。
	 * <p>
	 * 前置条件是「没有工厂方法名」且「有类名」；两者不满足则无从推断，直接抛异常。
	 */
	private static ResolvableType resolveComponentBeanType(GenericApplicationContext applicationContext,
			BeanDefinition beanDefinition, String beanName) {
		// 仅当不存在工厂方法、且类名可用时，才能按类名直接解析
		if (beanDefinition.getFactoryMethodName() == null && beanDefinition.getBeanClassName() != null) {
			try {
				// 使用容器的 ClassLoader 加载，兼容热部署、模块化等自定义类加载场景
				return ResolvableType.forClass(
						ClassUtils.forName(beanDefinition.getBeanClassName(), applicationContext.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalArgumentException("Impossible to resolve the type of bean " + beanName, ex);
			}
		}
		// 三级策略全部失败，明确报错而非返回一个不可用的类型
		throw new IllegalArgumentException("Impossible to resolve the type of bean " + beanName);
	}

	/**
	 * 【中文说明】获取候选方法集合。 若 BeanDefinition 允许访问非 public 成员，则连同私有/包级方法一起取出； 否则只取 public
	 * 方法。
	 */
	static private Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
		return (mbd.isNonPublicAccessAllowed() ? ReflectionUtils.getUniqueDeclaredMethods(factoryClass)
				: factoryClass.getMethods());
	}

	/**
	 * 【中文说明】判断是否为合格的静态工厂方法。 除了 static 修饰符，还要求方法<b>就声明在该工厂类上</b>——
	 * 排除从父类继承来的静态方法，避免误匹配。
	 */
	static private boolean isStaticCandidate(Method method, Class<?> factoryClass) {
		return (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == factoryClass);
	}

	/**
	 * 【中文说明】判断两个方法的参数签名是否不一致（用于检测方法重载歧义）。 先比参数个数（快速失败），再逐一比较参数类型。
	 */
	static private boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
		int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
		int candidateParameterCount = candidate.getParameterCount();
		// 参数个数不同 或 参数类型序列不同，均视为签名不匹配
		return (uniqueCandidateParameterCount != candidateParameterCount
				|| !Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes()));
	}

	/**
	 * Retrieves the type of a specific argument in a given function class.
	 * @param functionType The function type.
	 * @param argumentIndex The index of the argument whose type should be retrieved.
	 * @return The type of the specified function argument.
	 * @throws IllegalArgumentException if functionType is not a supported type
	 */
	/**
	 * 【中文说明】通用入口：从任意受支持的函数式类型中取出指定位置的泛型类型。
	 *
	 * <p>
	 * 与前面几个 {@code getXxxArgumentClass} 的区别：
	 * <ul>
	 * <li>入参是 {@link ResolvableType} 而非 Class，因此能接收已带泛型信息的类型；</li>
	 * <li>返回 {@link ResolvableType} 而非 Class，<b>保留嵌套泛型</b>（如
	 * {@code List<Foo>}）；</li>
	 * <li>自动识别 Function/BiFunction/Supplier/Consumer 以及 Kotlin 的
	 * Function0/1/2；</li>
	 * <li>无法识别时<b>抛异常</b>而非返回 Object.class 兜底。</li>
	 * </ul>
	 *
	 * <p>
	 * Kotlin 支持通过 {@code KotlinDetector.isKotlinPresent()} 做<b>运行时探测</b>—— 只有 classpath
	 * 上存在 Kotlin 时才走该分支，从而使 kotlin-stdlib 成为可选依赖。
	 * @param functionType 函数式类型
	 * @param argumentIndex 泛型位置索引
	 * @return 指定位置的泛型类型（保留嵌套泛型信息）
	 * @throws IllegalArgumentException 当类型不是受支持的函数式类型时抛出
	 */
	public static ResolvableType getFunctionArgumentType(ResolvableType functionType, int argumentIndex) {

		Class<?> resolvableClass = functionType.toClass();
		// 以 NONE 作为「尚未识别」的初始标记
		ResolvableType functionArgumentResolvableType = ResolvableType.NONE;

		// 依次尝试匹配四种 Java 标准函数式接口
		if (Function.class.isAssignableFrom(resolvableClass)) {
			functionArgumentResolvableType = functionType.as(Function.class);
		}
		else if (BiFunction.class.isAssignableFrom(resolvableClass)) {
			functionArgumentResolvableType = functionType.as(BiFunction.class);
		}
		else if (Supplier.class.isAssignableFrom(resolvableClass)) {
			functionArgumentResolvableType = functionType.as(Supplier.class);
		}
		else if (Consumer.class.isAssignableFrom(resolvableClass)) {
			functionArgumentResolvableType = functionType.as(Consumer.class);
		}
		// 都不匹配时，若运行环境存在 Kotlin，再尝试 Kotlin 的函数类型
		// 把 Kotlin 相关代码隔离在内部类中，可避免 Kotlin 缺失时触发 NoClassDefFoundError
		else if (KotlinDetector.isKotlinPresent()) {
			if (KotlinDelegate.isKotlinFunction(resolvableClass)) {
				functionArgumentResolvableType = KotlinDelegate.adaptToKotlinFunctionType(functionType);
			}
			else if (KotlinDelegate.isKotlinBiFunction(resolvableClass)) {
				functionArgumentResolvableType = KotlinDelegate.adaptToKotlinBiFunctionType(functionType);
			}
			else if (KotlinDelegate.isKotlinSupplier(resolvableClass)) {
				functionArgumentResolvableType = KotlinDelegate.adaptToKotlinSupplierType(functionType);
			}
		}

		// 仍为 NONE 说明所有分支都未命中，属于不受支持的类型
		if (functionArgumentResolvableType == ResolvableType.NONE) {
			throw new IllegalArgumentException(
					"Type must be a Function, BiFunction, Function1 or Function2. Found: " + functionType);
		}

		return functionArgumentResolvableType.getGeneric(argumentIndex);
	}

	/**
	 * 【中文说明】Kotlin 适配内部类。
	 *
	 * <p>
	 * 这是 Spring 中常见的<b>可选依赖隔离</b>写法：把对 Kotlin 类（{@code Function0/1/2}）的引用 集中在一个独立的私有静态内部类里。JVM
	 * 只有在真正加载该内部类时才会解析这些引用， 因此当 classpath 上没有 kotlin-stdlib 时，只要不触碰本类就不会报错。
	 *
	 * <p>
	 * Kotlin 函数类型与 Java 的对应关系：
	 * <ul>
	 * <li>{@code Function0<R>} ≈ {@link Supplier}（无参有返回）；</li>
	 * <li>{@code Function1<P1, R>} ≈ {@link Function}（一参一返回）；</li>
	 * <li>{@code Function2<P1, P2, R>} ≈ {@link BiFunction}（两参一返回）。</li>
	 * </ul>
	 */
	private static final class KotlinDelegate {

		// 对应 Java 的 Supplier：() -> R
		public static boolean isKotlinSupplier(Class<?> clazz) {
			return Function0.class.isAssignableFrom(clazz);
		}

		public static ResolvableType adaptToKotlinSupplierType(ResolvableType resolvableType) {
			return resolvableType.as(Function0.class);
		}

		// 对应 Java 的 Function：(P1) -> R
		public static boolean isKotlinFunction(Class<?> clazz) {
			return Function1.class.isAssignableFrom(clazz);
		}

		public static ResolvableType adaptToKotlinFunctionType(ResolvableType resolvableType) {
			return resolvableType.as(Function1.class);
		}

		// 对应 Java 的 BiFunction：(P1, P2) -> R
		public static boolean isKotlinBiFunction(Class<?> clazz) {
			return Function2.class.isAssignableFrom(clazz);
		}

		public static ResolvableType adaptToKotlinBiFunctionType(ResolvableType resolvableType) {
			return resolvableType.as(Function2.class);
		}

	}

}
