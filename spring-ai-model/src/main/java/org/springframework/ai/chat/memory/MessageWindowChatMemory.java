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

package org.springframework.ai.chat.memory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

/**
 * A chat memory implementation that maintains a message window of a specified size,
 * ensuring that the total number of messages does not exceed the specified limit. When
 * the number of messages exceeds the maximum size, older messages are evicted.
 * <p>
 * Messages of type {@link SystemMessage} are treated specially: if a new
 * {@link SystemMessage} is added, all previous {@link SystemMessage} instances are
 * removed from the memory. Also, if the total number of messages exceeds the limit, the
 * {@link SystemMessage} messages are preserved while evicting other types of messages.
 *
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
/**
 * 【中文说明】{@link ChatMemory} 的"**滑动消息窗口**"实现：只保留最近的若干条消息，
 * 保证历史消息总数不超过上限，超出时淘汰更早的消息（避免 token 无限增长、超出模型上下文长度）。
 *
 * <p>
 * 关键字段：
 * <ul>
 * <li>{@code chatMemoryRepository}——实际负责存取的仓储，默认 {@link InMemoryChatMemoryRepository}；</li>
 * <li>{@code maxMessages}——窗口容量上限，默认 {@code DEFAULT_MAX_MESSAGES} = 20 条。</li>
 * </ul>
 *
 * <p>
 * 两条特殊的 {@link SystemMessage} 规则（英文 javadoc 已述，此处复述）：
 * <ol>
 * <li>新增一条 SystemMessage 时，会**删除记忆中所有旧的 SystemMessage**——因为系统提示词代表当前
 * 人设/指令，应当只有一份生效，否则多份矛盾的系统提示会互相干扰；</li>
 * <li>裁剪时 SystemMessage **永远被保留**，只淘汰其它类型的消息——系统提示词是对话的基础约束，
 * 一旦被裁掉模型行为就会跑偏。</li>
 * </ol>
 *
 * <p>
 * 典型用法（Builder 构造，构造器私有）：
 *
 * <pre>{@code
 * ChatMemory memory = MessageWindowChatMemory.builder()
 *         .maxMessages(10)
 *         .chatMemoryRepository(new InMemoryChatMemoryRepository())
 *         .build();
 * }</pre>
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Thomas Vitale、Ilayaperumal Gopinathan；@since 1.0.0。
 */
public final class MessageWindowChatMemory implements ChatMemory {

	// 【中文】默认窗口大小：不指定 maxMessages 时最多保留 20 条消息。
	private static final int DEFAULT_MAX_MESSAGES = 20;

	// 【中文】底层存储（策略与存储分离）：本类只负责"留哪些消息"，存取动作全部委托给它。
	private final ChatMemoryRepository chatMemoryRepository;

	// 【中文】窗口容量上限，构造时校验必须大于 0；final 保证实例创建后不可变、线程安全。
	private final int maxMessages;

	// 【中文】构造器为 private：强制通过 builder() 创建实例，便于统一做参数校验与默认值填充。
	private MessageWindowChatMemory(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
		Assert.notNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
		// 【中文】窗口大小必须为正数：等于 0 意味着不保留任何消息，记忆功能形同虚设；负数更无意义。
		Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
		this.chatMemoryRepository = chatMemoryRepository;
		this.maxMessages = maxMessages;
	}

	// 【中文】追加消息：整体流程为"读出旧历史 → 合并新消息并按窗口规则裁剪 → 整体写回"。
	// 注意最后调用的是 saveAll（覆盖语义），因此裁剪结果能真正生效，而不是不断追加。
	@Override
	public void add(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		// 【中文】第一步：取出该会话已有的历史消息。
		List<Message> memoryMessages = this.chatMemoryRepository.findByConversationId(conversationId);
		// 【中文】第二步：核心处理——合并新旧消息、去重系统消息、按窗口上限裁剪（见 process 方法）。
		List<Message> processedMessages = process(memoryMessages, messages);
		// 【中文】第三步：把处理后的"完整列表"整体覆盖写回存储。
		this.chatMemoryRepository.saveAll(conversationId, processedMessages);
	}

	// 【中文】读取历史消息：本实现不做额外加工，直接透传给仓储层。
	// 因为裁剪已在写入（add）时完成，所以读出来的天然就是窗口内的消息。
	@Override
	public List<Message> get(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		return this.chatMemoryRepository.findByConversationId(conversationId);
	}

	// 【中文】清空指定会话的历史，直接委托仓储删除。
	@Override
	public void clear(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.chatMemoryRepository.deleteByConversationId(conversationId);
	}

	/**
	 * 【中文说明】窗口裁剪的核心私有方法：把"已有历史 + 本次新增"合并成一份符合窗口约束的新列表。
	 *
	 * <p>
	 * 处理步骤：①判断本次是否带来了新的 SystemMessage；②若有，则丢弃历史中所有旧 SystemMessage；
	 * ③拼接新消息；④若总数未超限直接返回；⑤超限则只从非 SystemMessage 中淘汰最早的若干条，
	 * 并把切割点对齐到 USER 消息，保证保留下来的窗口从一轮完整对话开始。
	 * @param memoryMessages 存储中已有的历史消息
	 * @param newMessages 本次要追加的新消息
	 * @return 裁剪后应当被持久化的完整消息列表
	 */
	private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
		List<Message> processedMessages = new ArrayList<>();

		// 【中文】把历史消息放进 HashSet，目的是下一步能以 O(1) 复杂度判断"某条系统消息是不是新来的"。
		// 这依赖 Message 实现了 equals/hashCode（按内容比较），因此内容完全相同的系统消息会被视作同一条。
		Set<Message> memoryMessagesSet = new HashSet<>(memoryMessages);
		// 【中文】判断本次新增里是否包含"记忆中尚不存在"的 SystemMessage。
		// 注意必须排除内容重复的情况：若每轮都重复传同一条系统提示词，不应触发清理逻辑。
		boolean hasNewSystemMessage = newMessages.stream()
			.filter(SystemMessage.class::isInstance)
			.anyMatch(message -> !memoryMessagesSet.contains(message));

		// 【中文】规则一：若确实有新的系统消息，则过滤掉历史中所有旧的 SystemMessage（保证系统提示只有一份生效）；
		// 若没有新系统消息，hasNewSystemMessage 为 false，条件恒不成立，历史消息全部原样保留。
		memoryMessages.stream()
			.filter(message -> !(hasNewSystemMessage && message instanceof SystemMessage))
			.forEach(processedMessages::add);

		// 【中文】再把本次的新消息追加到末尾，此时 processedMessages 就是"合并后的完整对话"。
		processedMessages.addAll(newMessages);

		// 【中文】快速返回：未超出窗口上限就无需裁剪，直接返回。
		if (processedMessages.size() <= this.maxMessages) {
			return processedMessages;
		}

		// Collect the indices of non-system messages; SystemMessages are always
		// preserved.
		// 【中文】规则二：先收集所有"非系统消息"的下标——只有它们才是可被淘汰的候选，
		// SystemMessage 不进入这个列表，因此永远不会被裁掉。
		List<Integer> nonSystemIndices = new ArrayList<>();
		for (int i = 0; i < processedMessages.size(); i++) {
			if (!(processedMessages.get(i) instanceof SystemMessage)) {
				nonSystemIndices.add(i);
			}
		}

		// Raw cut: the number of non-system messages that must be removed to fit within
		// maxMessages. This index into nonSystemIndices is where the kept window would
		// start based on count alone.
		// 【中文】初步计算切割点：为了把总数压到 maxMessages 以内，需要淘汰的消息条数。
		// 该值同时也是"保留窗口在 nonSystemIndices 中的起始下标"（仅按数量计算，尚未考虑对话完整性）。
		int cutIndex = processedMessages.size() - this.maxMessages;

		// Snap the cut forward to the nearest USER message so the kept window always
		// starts at a complete turn. This prevents keeping an assistant reply or tool
		// result without the user message that originated its turn.
		// 【中文】关键细节：把切割点向后"吸附"到最近的一条 USER 消息上，
		// 使保留下来的窗口一定从"完整的一轮对话"开始。
		// 否则可能出现只剩助手回复或工具调用结果、却丢了引发它的用户提问，
		// 这种残缺上下文会让模型困惑，某些厂商 API 也会因消息角色顺序非法而直接报错。
		// 代价是实际保留条数可能略少于 maxMessages（宁可少留，也要保证语义完整）。
		while (cutIndex < nonSystemIndices.size()
				&& processedMessages.get(nonSystemIndices.get(cutIndex)).getMessageType() != MessageType.USER) {
			cutIndex++;
		}
		// 【中文】边界保护：若一路后移都没找到 USER 消息，cutIndex 可能越界，
		// 用 min 夹住上界，防止后续 subList 抛 IndexOutOfBoundsException（极端情况下会淘汰全部非系统消息）。
		cutIndex = Math.min(cutIndex, nonSystemIndices.size());

		// 【中文】收集需要删除的原始下标集合（nonSystemIndices 的前 cutIndex 个，即最早的那批非系统消息）。
		// 用 Set 是为了在下面的遍历中做 O(1) 的 contains 判断。
		Set<Integer> removeIndices = new HashSet<>(nonSystemIndices.subList(0, cutIndex));
		// 【中文】按原始顺序遍历重建列表，跳过待删除下标；
		// 这样既保证了消息的时间先后顺序不乱，也保证了穿插其中的 SystemMessage 原位保留。
		List<Message> trimmedMessages = new ArrayList<>();
		for (int i = 0; i < processedMessages.size(); i++) {
			if (!removeIndices.contains(i)) {
				trimmedMessages.add(processedMessages.get(i));
			}
		}
		return trimmedMessages;
	}

	// 【中文】获取 Builder 的静态工厂方法，是创建本类实例的唯一入口（构造器为 private）。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@link MessageWindowChatMemory} 的建造者（Builder 模式）。
	 *
	 * <p>
	 * 两个可选配置项均有默认值：仓储默认使用 {@link InMemoryChatMemoryRepository}，
	 * 窗口大小默认 {@code DEFAULT_MAX_MESSAGES}（20），因此
	 * {@code MessageWindowChatMemory.builder().build()} 即可得到一个可用实例。
	 *
	 * <p>
	 * 每个配置方法都返回 {@code this} 以支持链式调用；真正的参数合法性校验统一放在
	 * {@code build()} 调用的私有构造器中完成。
	 */
	public static final class Builder {

		// 【中文】默认存储实现：内存版，开箱即用（生产环境建议显式替换为持久化实现）。
		private ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

		// 【中文】默认窗口大小 20 条。
		private int maxMessages = DEFAULT_MAX_MESSAGES;

		// 【中文】构造器私有：只能通过外部类的 builder() 方法获得实例。
		private Builder() {
		}

		// 【中文】自定义底层存储实现（如 JDBC、Redis 版本的 ChatMemoryRepository）。
		public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
			this.chatMemoryRepository = chatMemoryRepository;
			return this;
		}

		// 【中文】自定义窗口大小；此处不校验，非法值（<= 0）会在 build() 时由构造器抛异常。
		public Builder maxMessages(int maxMessages) {
			this.maxMessages = maxMessages;
			return this;
		}

		// 【中文】完成构建：调用私有构造器，参数校验在其中集中进行。
		public MessageWindowChatMemory build() {
			return new MessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
		}

	}

}
