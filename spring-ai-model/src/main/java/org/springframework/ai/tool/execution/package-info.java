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

/**
 * 【中文说明】工具「执行期」支撑包：负责工具调用前后的结果转换与异常处理。
 *
 * <p>
 * 两条主线：
 * <ul>
 * <li><b>结果转换</b>：{@link org.springframework.ai.tool.execution.ToolCallResultConverter}
 * 及其默认实现 {@link org.springframework.ai.tool.execution.DefaultToolCallResultConverter}，
 * 把工具返回的 Java 对象序列化为可回传给模型的字符串；</li>
 * <li><b>异常处理</b>：{@link org.springframework.ai.tool.execution.ToolExecutionException}
 * 承载「哪个工具出错了」，
 * {@link org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor} 决定是把错误
 * 转成文本喂给模型，还是向上抛出中断流程。</li>
 * </ul>
 *
 * <p>
 * 包级注解 {@code @NullMarked}（JSpecify）表示本包内类型默认不可为 null。
 */
@NullMarked
package org.springframework.ai.tool.execution;

import org.jspecify.annotations.NullMarked;
