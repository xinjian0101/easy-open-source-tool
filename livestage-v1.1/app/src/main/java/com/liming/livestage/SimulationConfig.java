package com.liming.livestage;

public final class SimulationConfig {
    public String hostName = "HOST";
    public String title = "直播训练演示";
    public int initialViewers = 128;
    public Scene scene = Scene.BAR;
    public Speed speed = Speed.NORMAL;
    public boolean bilingual = true;

    public enum Scene {
        BAR("酒吧"), STREET("街头"), RESTAURANT("餐厅"), GYM("健身房"), GENERAL("通用");
        public final String label;
        Scene(String label) { this.label = label; }
    }

    public enum Speed {
        SLOW("慢", 4500L), NORMAL("正常", 2800L), FAST("快", 1500L);
        public final String label;
        public final long commentIntervalMs;
        Speed(String label, long commentIntervalMs) {
            this.label = label;
            this.commentIntervalMs = commentIntervalMs;
        }
    }
}
