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

package org.springframework.ai.chat.prompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ModelRequest;
import org.springframework.util.Assert;

/**
 * The Prompt class represents a prompt used in AI model requests. A prompt consists of
 * one or more messages and additional chat options.
 *
 * @author Mark Pollack
 * @author luocongqiu
 * @author Thomas Vitale
 * @author Sebastien Deleuze
 */
/**
 * 【中文说明】Prompt（提示词）是发送给大模型的**一次完整请求**的封装，是 Spring AI 中最核心的入参对象。
 *
 * <p>
 * 组成部分（两个 final 字段）：
 * <ul>
 * <li>{@code messages}——消息列表，可包含 SystemMessage（系统设定）、UserMessage（用户提问）、
 * AssistantMessage（历史回复）、ToolResponseMessage（工具返回）等；</li>
 * <li>{@code chatOptions}——本次请求的模型参数（温度、模型名、最大 token 等），可为 null 表示用默认值。</li>
 * </ul>
 *
 * <p>
 * 实现 {@code ModelRequest<List<Message>>} 接口，其 {@link #getInstructions()} 返回消息列表、
 * {@link #getOptions()} 返回参数，从而统一了框架内所有模型请求的形态。
 *
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * // 最简：一句话
 * new Prompt("你好");
 * // 带系统设定与参数
 * new Prompt(List.of(new SystemMessage("你是翻译助手"), new UserMessage("hello")), options);
 * }</pre>
 *
 * <p>
 * 便捷能力：提供 {@code getSystemMessage()}/{@code getUserMessage()} 等按类型检索消息的方法，
 * 以及 {@code augmentXxxMessage(...)}/{@code copy()}/{@code mutate()} 等"**返回新实例**"的
 * 修改方法（本类语义上不可变，任何修改都不会影响原对象，便于在 Advisor 链中安全传递）。
 *
 * <p>
 * 对应英文 javadoc 中的标签：@author Mark Pollack、luocongqiu、Thomas Vitale、Sebastien Deleuze。
 */
public class Prompt implements ModelRequest<List<Message>> {

	// 【中文】本次请求的消息列表，按顺序发送给模型。
	private final List<Message> messages;

	// 【中文】本次请求的模型参数；可为 null，此时由 ChatModel 使用其默认参数。
	private final @Nullable ChatOptions chatOptions;

	// 【中文】便捷构造器：直接传一段文本，内部自动包装成一条 UserMessage（最常用）。
	// 下面一系列重载构造器最终都通过 this(...) 委托到"消息列表 + 参数"的全参构造器。
	public Prompt(String contents) {
		this(new UserMessage(contents));
	}

	// 【中文】传入单条消息（可以是任意类型的 Message）。
	public Prompt(Message message) {
		this(Collections.singletonList(message));
	}

	// 【中文】传入消息列表，不指定模型参数（chatOptions 传 null）。
	public Prompt(List<Message> messages) {
		this(messages, null);
	}

	// 【中文】可变参数版本：Prompt(msg1, msg2, msg3) 这样写更方便。
	public Prompt(Message... messages) {
		this(Arrays.asList(messages), null);
	}

	// 【中文】文本 + 模型参数。
	public Prompt(String contents, @Nullable ChatOptions chatOptions) {
		this(new UserMessage(contents), chatOptions);
	}

	// 【中文】单条消息 + 模型参数。
	public Prompt(Message message, @Nullable ChatOptions chatOptions) {
		this(Collections.singletonList(message), chatOptions);
	}

	// 【中文】全参构造器：所有其它构造器最终都汇聚到这里，参数校验只需写一份。
	public Prompt(List<Message> messages, @Nullable ChatOptions chatOptions) {
		Assert.notNull(messages, "messages cannot be null");
		// 【中文】校验列表内不能有 null 元素，否则后续序列化请求体时才会抛 NPE，难以定位。
		Assert.noNullElements(messages, "messages cannot contain null elements");
		this.messages = messages;
		// 【中文】chatOptions 允许为 null，因此不做校验。
		this.chatOptions = chatOptions;
	}

	// 【中文】把所有消息的正文**直接拼接**成一个字符串。
	// 注意：中间不加任何分隔符，也不含角色标识，主要用于日志或简单场景，不适合还原完整对话结构。
	public String getContents() {
		StringBuilder sb = new StringBuilder();
		for (Message message : getInstructions()) {
			sb.append(message.getText());
		}
		return sb.toString();
	}

	// 【中文】实现 ModelRequest 接口：返回本次请求的模型参数（可能为 null）。
	@Override
	public @Nullable ChatOptions getOptions() {
		return this.chatOptions;
	}

	// 【中文】实现 ModelRequest 接口：返回消息列表（"instructions"即发给模型的指令内容）。
	// 注意此处直接返回内部引用、未做防御性拷贝，调用方不应修改返回的列表。
	@Override
	public List<Message> getInstructions() {
		return this.messages;
	}

	/**
	 * Get the first system message in the prompt. If no system message is found, an empty
	 * SystemMessage is returned.
	 */
	// 【中文】取**第一条**系统消息（从前往后找）：系统提示词通常放在最前面。
	public SystemMessage getSystemMessage() {
		for (int i = 0; i <= this.messages.size() - 1; i++) {
			Message message = this.messages.get(i);
			// 【中文】instanceof 模式匹配：类型判断 + 变量绑定一步完成。
			if (message instanceof SystemMessage systemMessage) {
				return systemMessage;
			}
		}
		// 【中文】空值处理：找不到时返回内容为空串的 SystemMessage 而非 null，调用方无需判空。
		return new SystemMessage("");
	}

	/**
	 * Get the last user message in the prompt. If no user message is found, an empty
	 * UserMessage is returned.
	 */
	// 【中文】取**最后一条**用户消息（从后往前找）。
	// 与上面取系统消息的方向相反：用户的"当前问题"总是在列表末尾，前面的都是历史。
	public UserMessage getUserMessage() {
		for (int i = this.messages.size() - 1; i >= 0; i--) {
			Message message = this.messages.get(i);
			if (message instanceof UserMessage userMessage) {
				return userMessage;
			}
		}
		// 【中文】找不到时返回空 UserMessage，避免返回 null。
		return new UserMessage("");
	}

	/**
	 * Get the last user or tool response message in the prompt. If no user or tool
	 * response message is found, an empty UserMessage is returned.
	 */
	// 【中文】取最后一条"用户消息或工具返回消息"。
	// 用途：在工具调用（Function Calling）场景下，触发模型继续生成的可能是用户提问，
	// 也可能是上一轮工具的执行结果，这两者都算"本轮的输入"，因此需要一并查找。
	public Message getLastUserOrToolResponseMessage() {
		for (int i = this.messages.size() - 1; i >= 0; i--) {
			Message message = this.messages.get(i);
			if (message instanceof UserMessage || message instanceof ToolResponseMessage) {
				return message;
			}
		}
		return new UserMessage("");
	}

	/**
	 * Get all system messages in the prompt.
	 * @return a list of all system messages in the prompt
	 */
	// 【中文】取出**全部**系统消息（区别于只取第一条的 getSystemMessage）。
	public List<SystemMessage> getSystemMessages() {
		List<SystemMessage> systemMessages = new ArrayList<>();
		for (Message message : this.messages) {
			if (message instanceof SystemMessage systemMessage) {
				systemMessages.add(systemMessage);
			}
		}
		return systemMessages;
	}

	/**
	 * Get all user messages in the prompt.
	 * @return a list of all user messages in the prompt
	 */
	// 【中文】取出**全部**用户消息，按原有顺序返回；找不到时返回空列表。
	public List<UserMessage> getUserMessages() {
		List<UserMessage> userMessages = new ArrayList<>();
		for (Message message : this.messages) {
			if (message instanceof UserMessage userMessage) {
				userMessages.add(userMessage);
			}
		}
		return userMessages;
	}

	// 【中文】调试输出。注意字段名打印为 modelOptions（历史命名），与字段 chatOptions 略有出入。
	@Override
	public String toString() {
		return "Prompt{" + "messages=" + this.messages + ", modelOptions=" + this.chatOptions + '}';
	}

	// 【中文】值相等性判断：消息列表与模型参数都相等才算相等。
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Prompt prompt)) {
			return false;
		}
		return Objects.equals(this.messages, prompt.messages) && Objects.equals(this.chatOptions, prompt.chatOptions);
	}

	// 【中文】与 equals 使用相同字段计算哈希。
	@Override
	public int hashCode() {
		return Objects.hash(this.messages, this.chatOptions);
	}

	// 【中文】深拷贝出一个新的 Prompt：消息逐条复制，chatOptions 则沿用同一引用（浅拷贝）。
	// 常用于 Advisor 链中在不影响原请求的前提下修改消息。
	public Prompt copy() {
		return new Prompt(instructionsCopy(), this.chatOptions);
	}

	/**
	 * 【中文说明】逐条复制消息列表的私有辅助方法。
	 *
	 * <p>
	 * 由于 {@code Message} 各子类的复制方式不同（部分有 {@code copy()}，部分只能靠 Builder 重建），
	 * 这里用一串 {@code instanceof} 分支分别处理，属于典型的"按类型分派"写法。
	 */
	private List<Message> instructionsCopy() {
		List<Message> messagesCopy = new ArrayList<>();
		this.messages.forEach(message -> {
			// 【中文】UserMessage / SystemMessage 自带 copy() 方法，直接调用即可。
			if (message instanceof UserMessage userMessage) {
				messagesCopy.add(userMessage.copy());
			}
			else if (message instanceof SystemMessage systemMessage) {
				messagesCopy.add(systemMessage.copy());
			}
			// 【中文】AssistantMessage 没有 copy()，需用 Builder 逐字段重建。
			else if (message instanceof AssistantMessage assistantMessage) {
				messagesCopy.add(AssistantMessage.builder()
					// 【中文】空值处理：getText() 可能为 null（例如只含工具调用、没有文本内容的回复），
					// requireNonNullElse 把 null 替换为空串，避免 Builder 因 null 报错。
					.content(Objects.requireNonNullElse(assistantMessage.getText(), ""))
					.properties(assistantMessage.getMetadata())
					.toolCalls(assistantMessage.getToolCalls())
					.build());
			}
			// 【中文】ToolResponseMessage 同样用 Builder 重建；
			// 这里对内部的 responses 和 metadata 都新建了容器，属于较彻底的深拷贝。
			else if (message instanceof ToolResponseMessage toolResponseMessage) {
				messagesCopy.add(ToolResponseMessage.builder()
					.responses(new ArrayList<>(toolResponseMessage.getResponses()))
					.metadata(new HashMap<>(toolResponseMessage.getMetadata()))
					.build());
			}
			else {
				// 【中文】兜底分支：遇到框架未知的自定义 Message 类型时直接抛异常（快速失败），
				// 而不是悄悄跳过——静默丢消息会造成极难排查的问题。
				throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
			}
		});

		return messagesCopy;
	}

	/**
	 * Augments the first system message in the prompt with the provided function. If no
	 * system message is found, a new one is created with the provided text.
	 * @return a new {@link Prompt} instance with the augmented system message.
	 */
	// 【中文】增强（改写）第一条系统消息，返回**新的 Prompt**，原对象不受影响。
	// 参数是一个 Function：入参为旧的 SystemMessage、返回新的 SystemMessage，由调用方决定如何改写，
	// 例如 RAG 场景下把检索到的知识拼接进系统提示词。
	public Prompt augmentSystemMessage(Function<SystemMessage, SystemMessage> systemMessageAugmenter) {
		// 【中文】先拷贝一份列表再修改，保证本对象的不可变性。
		var messagesCopy = new ArrayList<>(this.messages);
		// 【中文】found 标记用于区分"改写了已有系统消息"和"压根没有系统消息"两种情况。
		boolean found = false;
		for (int i = 0; i < messagesCopy.size(); i++) {
			Message message = messagesCopy.get(i);
			if (message instanceof SystemMessage systemMessage) {
				// 【中文】原地替换（set）第一条系统消息，然后 break——只处理第一条。
				messagesCopy.set(i, systemMessageAugmenter.apply(systemMessage));
				found = true;
				break;
			}
		}
		if (!found) {
			// If no system message is found, create a new one with the provided text
			// and add it as the first item in the list.
			// 【中文】没有系统消息时：用空的 SystemMessage("") 作为入参调用改写函数得到新消息，
			// 并插入到列表**最前面**（下标 0）——系统提示词按惯例必须排在最前。
			messagesCopy.add(0, systemMessageAugmenter.apply(new SystemMessage("")));
		}
		// 【中文】返回新 Prompt，沿用原有的 chatOptions。
		return new Prompt(messagesCopy, this.chatOptions);
	}

	/**
	 * Augments the last system message in the prompt with the provided text. If no system
	 * message is found, a new one is created with the provided text.
	 * @return a new {@link Prompt} instance with the augmented system message.
	 */
	// 【中文】重载的便捷版本：直接用新文本**整体替换**系统消息的内容。
	// 内部构造一个 Lambda 委托给上面的函数版本；mutate() 表示"基于原消息复制后修改"，
	// 因此除 text 外的其它属性（如 metadata）都会保留。
	public Prompt augmentSystemMessage(String newSystemText) {
		return augmentSystemMessage(systemMessage -> systemMessage.mutate().text(newSystemText).build());
	}

	/**
	 * Augments the last user message in the prompt with the provided function. If no user
	 * message is found, a new one is created with the provided text.
	 * @return a new {@link Prompt} instance with the augmented user message.
	 */
	// 【中文】增强（改写）**最后一条**用户消息，返回新的 Prompt。
	// RAG 场景中把检索到的上下文拼到用户提问里，就是通过它实现的。
	public Prompt augmentUserMessage(Function<UserMessage, UserMessage> userMessageAugmenter) {
		var messagesCopy = new ArrayList<>(this.messages);
		// 【中文】从后往前遍历，找到的第一条 UserMessage 即"最后一条"。
		for (int i = messagesCopy.size() - 1; i >= 0; i--) {
			Message message = messagesCopy.get(i);
			if (message instanceof UserMessage userMessage) {
				messagesCopy.set(i, userMessageAugmenter.apply(userMessage));
				break;
			}
			// 【中文】兜底逻辑：遍历到下标 0 仍未找到用户消息，说明列表中根本没有 UserMessage，
			// 此时用空 UserMessage("") 生成一条并追加到**末尾**（与系统消息插到最前恰好相反）。
			// 注意这里的写法依赖"列表非空"——若 messages 为空列表，循环体一次都不会执行，
			// 也就不会补出用户消息（与上面 augmentSystemMessage 用 found 标记的写法不同）。
			if (i == 0) {
				messagesCopy.add(userMessageAugmenter.apply(new UserMessage("")));
			}
		}

		return new Prompt(messagesCopy, this.chatOptions);
	}

	/**
	 * Augments the last user message in the prompt with the provided text. If no user
	 * message is found, a new one is created with the provided text.
	 * @return a new {@link Prompt} instance with the augmented user message.
	 */
	// 【中文】重载的便捷版本：用新文本替换最后一条用户消息的内容。
	public Prompt augmentUserMessage(String newUserText) {
		return augmentUserMessage(userMessage -> userMessage.mutate().text(newUserText).build());
	}

	// 【中文】mutate()：以当前 Prompt 为模板生成一个**已预填充**的 Builder，
	// 便于"复制并局部修改"（如只想换掉 chatOptions）。
	public Builder mutate() {
		// 【中文】消息使用深拷贝（instructionsCopy），避免新旧 Prompt 共享同一批消息对象。
		Builder builder = new Builder().messages(instructionsCopy());
		// 【中文】空值处理：chatOptions 可能为 null，而 Builder#chatOptions 参数不接受 null，
		// 故先判空再设置。
		if (this.chatOptions != null) {
			builder.chatOptions(this.chatOptions);
		}
		return builder;
	}

	// 【中文】获取空白 Builder 的静态工厂方法。
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 【中文说明】{@link Prompt} 的建造者，适合需要分步组装消息与参数的场景。
	 *
	 * <p>
	 * 互斥/覆盖约束需留意：{@code content(...)}、{@code messages(...)} 都是**覆盖**语义
	 * （直接给 {@code messages} 字段重新赋值，而非追加），因此同时调用时后者生效；
	 * 且二者至少要调用其一，否则 {@link #build()} 会抛异常。
	 */
	public static final class Builder {

		// 【中文】待设置的消息列表；初始为 null，用于在 build() 时判断"用户是否设置过"。
		private @Nullable List<Message> messages;

		// 【中文】待设置的模型参数，可选。
		private @Nullable ChatOptions chatOptions;

		// 【中文】便捷方法：直接给一段文本，内部包装成单条 UserMessage（注意是覆盖而非追加）。
		public Builder content(@Nullable String content) {
			this.messages = List.of(new UserMessage(content));
			return this;
		}

		// 【中文】可变参数版本设置消息列表。
		// 参数上的 `Message @Nullable ...` 是数组类型注解写法：表示"数组本身"可为 null，
		// 而非数组元素可为 null。传 null 时保持原值不变（不清空）。
		public Builder messages(Message @Nullable ... messages) {
			if (messages != null) {
				this.messages = Arrays.asList(messages);
			}
			return this;
		}

		// 【中文】List 版本设置消息列表（覆盖语义）。
		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		// 【中文】设置模型参数。
		public Builder chatOptions(ChatOptions chatOptions) {
			this.chatOptions = chatOptions;
			return this;
		}

		// 【中文】完成构建。
		public Prompt build() {
			// 【中文】约束校验：messages 与 content 必须至少设置一个，否则 Prompt 没有任何内容。
			// 这里用 Assert.state（校验对象状态，抛 IllegalStateException）而非 Assert.notNull
			// （校验方法入参，抛 IllegalArgumentException），语义更贴切。
			Assert.state(this.messages != null, "either messages or content needs to be set");
			return new Prompt(this.messages, this.chatOptions);
		}

	}

}
