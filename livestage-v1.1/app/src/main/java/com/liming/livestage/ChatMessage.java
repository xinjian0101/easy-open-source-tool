package com.liming.livestage;

public final class ChatMessage {
    public enum Type { COMMENT, ENTER, FOLLOW, SYSTEM }

    public final String user;
    public final String text;
    public final Type type;
    public final long createdAt;

    public ChatMessage(String user, String text, Type type) {
        this.user = user;
        this.text = text;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }
}
