package com.liming.livestage;

public final class GiftEvent {
    public final String user;
    public final String gift;
    public final int count;
    public final long createdAt;

    public GiftEvent(String user, String gift, int count) {
        this.user = user;
        this.gift = gift;
        this.count = count;
        this.createdAt = System.currentTimeMillis();
    }
}
