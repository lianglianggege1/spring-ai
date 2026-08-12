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

package org.springframework.ai.moderation;

import org.springframework.ai.model.AbstractResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;

/**
 * Defines the metadata associated with a moderation response, extending a base response
 * interface. This interface is intended to provide additional context or data about the
 * moderation process result.
 *
 * @author Ahmed Yousri
 * @since 1.0.0
 */
/**
 * 【中文说明】整个审核响应的元数据。
 *
 * <p>
 * 继承 {@code AbstractResponseMetadata} 获得一份基于 Map 的键值对存储能力（可放入
 * 请求 ID、限流信息、用量统计等厂商自定义数据），同时实现 {@code ResponseMetadata} 接口以纳入
 * Spring AI 统一的元数据体系。
 *
 * <p>
 * 类体为空，说明公共层没有额外字段——所有能力均由父类提供，此类的价值在于给审核场景一个
 * 专属类型，便于各厂商继承扩展。
 */
public class ModerationResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {

}
