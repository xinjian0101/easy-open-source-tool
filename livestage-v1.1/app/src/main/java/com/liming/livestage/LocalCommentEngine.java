package com.liming.livestage;

import java.util.Random;

public final class LocalCommentEngine {
    private final Random random = new Random();

    private static final String[] EN_NAMES = {
            "Emma_92", "MiaLive", "LeoNYC", "Jay_404", "Sophie", "Noah", "Ava", "Liam",
            "Olivia", "Ethan", "Chloe", "Ryan", "Nora", "Lucas", "Luna", "Alex", "Sam", "Ruby"
    };
    private static final String[] CN_NAMES = {
            "小雨", "阿杰", "晚风", "橘子汽水", "林间鹿", "北城", "小满", "大川",
            "柚子", "星河", "海盐", "阿宁", "小野", "南风", "七七", "木子"
    };
    private static final String[] GENERAL_CN = {
            "刚进来，这是在做什么？", "主播声音挺清楚", "今天状态不错", "这个角度可以",
            "有人知道这是哪里吗？", "继续聊，挺有意思", "刚刚那句话没听清", "现场看起来很热闹",
            "第一次刷到", "主播看一下评论", "这个氛围不错", "别停，继续说"
    };
    private static final String[] GENERAL_EN = {
            "Just joined, what is happening?", "The vibe is good tonight", "Where are you right now?",
            "First time seeing this live", "This angle actually looks nice", "Keep talking, this is fun",
            "Can you read the comments?", "The place looks busy", "Your audio is clear", "What time is it there?"
    };
    private static final String[] BAR_CN = {
            "这家酒吧人好多", "背景音乐叫什么？", "你喝的是什么？", "旁边那桌一直在看镜头",
            "灯光还挺好看", "今天周末吗这么热闹", "别喝太快", "现场音乐声音大不大？",
            "这家店在哪个城市？", "后面有人在跳舞", "这个位置拍得挺有感觉"
    };
    private static final String[] BAR_EN = {
            "That bar looks packed", "What are you drinking?", "The lighting is actually nice",
            "What song is playing?", "Bro is having a good night", "Is that place downtown?",
            "Someone behind you keeps looking at the camera", "The music must be loud there",
            "That table behind you is wild", "Show us the dance floor"
    };
    private static final String[] STREET_CN = {
            "外面风是不是很大？", "这条街晚上挺漂亮", "后面那家店还开着", "注意看路",
            "这是市中心吗？", "街上人不少", "镜头转一下看看周围", "夜景不错"
    };
    private static final String[] STREET_EN = {
            "That street looks nice at night", "Is this downtown?", "Watch where you are walking",
            "Show the buildings around you", "It looks busy there", "What city is this?"
    };
    private static final String[] RESTAURANT_CN = {
            "这道菜看着不错", "人均大概多少？", "味道怎么样？", "菜单能拍一下吗？",
            "这家店需要预约吗？", "旁边那杯是什么？", "看饿了", "上菜速度快吗？"
    };
    private static final String[] RESTAURANT_EN = {
            "That food looks good", "What did you order?", "How much is the meal?",
            "Would you recommend this place?", "Show the menu", "Now I am hungry"
    };
    private static final String[] GYM_CN = {
            "今天练什么部位？", "这个重量可以", "注意动作别借力", "练了多久了？",
            "组间休息多长？", "健身房人不多", "动作挺标准", "别忘了拉伸"
    };
    private static final String[] GYM_EN = {
            "What are you training today?", "How many sets?", "That form looks solid",
            "Do not skip the warm-up", "How long have you been training?", "Good set"
    };
    private static final String[] ENTER_CN = {"进入了直播间", "刚刚加入", "从推荐页进来了"};
    private static final String[] ENTER_EN = {"joined the live", "just joined", "came from recommendations"};
    private static final String[] FOLLOW_CN = {"关注了主播", "刚刚点了关注"};
    private static final String[] FOLLOW_EN = {"followed the host", "started following"};

    public ChatMessage nextComment(SimulationConfig config) {
        boolean english = config.bilingual && random.nextBoolean();
        String text;
        switch (config.scene) {
            case BAR: text = pick(english ? BAR_EN : BAR_CN); break;
            case STREET: text = pick(english ? STREET_EN : STREET_CN); break;
            case RESTAURANT: text = pick(english ? RESTAURANT_EN : RESTAURANT_CN); break;
            case GYM: text = pick(english ? GYM_EN : GYM_CN); break;
            default: text = pick(english ? GENERAL_EN : GENERAL_CN);
        }
        if (random.nextInt(4) == 0) {
            String suffix = pick(english ? GENERAL_EN : GENERAL_CN);
            if (!suffix.equals(text)) text = text + "  " + suffix;
        }
        return new ChatMessage(randomName(english), text, ChatMessage.Type.COMMENT);
    }

    public ChatMessage nextEnter(SimulationConfig config) {
        boolean english = config.bilingual && random.nextBoolean();
        return new ChatMessage(randomName(english), pick(english ? ENTER_EN : ENTER_CN), ChatMessage.Type.ENTER);
    }

    public ChatMessage nextFollow(SimulationConfig config) {
        boolean english = config.bilingual && random.nextBoolean();
        return new ChatMessage(randomName(english), pick(english ? FOLLOW_EN : FOLLOW_CN), ChatMessage.Type.FOLLOW);
    }

    public GiftEvent nextGift(SimulationConfig config) {
        boolean english = config.bilingual && random.nextBoolean();
        String[] gifts = {"Rose", "Heart", "Star", "Coffee", "Crown", "Fireworks"};
        int roll = random.nextInt(100);
        String gift = roll < 48 ? gifts[0] : roll < 72 ? gifts[1] : roll < 86 ? gifts[2]
                : roll < 94 ? gifts[3] : roll < 99 ? gifts[4] : gifts[5];
        int count = roll < 75 ? 1 : roll < 95 ? 3 : 10;
        return new GiftEvent(randomName(english), gift, count);
    }

    private String randomName(boolean english) { return pick(english ? EN_NAMES : CN_NAMES); }
    private String pick(String[] values) { return values[random.nextInt(values.length)]; }
}
