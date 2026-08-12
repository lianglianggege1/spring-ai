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

/**
 * Marker interface, to be used to store info on the model such as the current context
 * length.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
/**
 * 【中文说明】ChatModelDescription 是<b>聊天模型</b>专用的描述标记接口，
 * 直接继承 {@link ModelDescription} 且不新增任何方法。
 *
 * <p>
 * 为什么需要这样一个"空的子接口"：它起到<b>类型细分</b>的作用——在泛型签名或参数类型中
 * 可以写明"这里只接受聊天模型的描述"，从而在编译期就排除掉嵌入模型等其他类型，
 * 比统一用父接口更安全。英文注释中也提到，未来可用于承载上下文长度（context length）
 * 等聊天模型特有的信息。
 *
 * <p>
 * 典型用法：各厂商的聊天模型枚举实现本接口，例如
 * {@code public enum ChatModel implements ChatModelDescription}，
 * 只需实现继承来的 getName() 即可。
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public interface ChatModelDescription extends ModelDescription {

}
