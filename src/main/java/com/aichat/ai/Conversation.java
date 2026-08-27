package com.aichat.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** 一个玩家的多轮对话历史（只保留 user 和最终 assistant 消息） */
public class Conversation {
    private final Deque<ChatMessage> history = new ArrayDeque<>();
    private final int maxSize;

    public Conversation(int maxSize) {
        this.maxSize = Math.max(2, maxSize);
    }

    public synchronized void add(ChatMessage msg) {
        history.addLast(msg);
        while (history.size() > maxSize) {
            history.removeFirst();
        }
    }

    public synchronized List<ChatMessage> snapshot() {
        return List.copyOf(history);
    }
}
