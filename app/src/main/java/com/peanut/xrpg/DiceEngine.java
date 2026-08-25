package com.peanut.xrpg;

import java.util.Random;

/** Pure dice math kept in Java so the game core is easy to reuse/test. */
public final class DiceEngine {
    private static final Random RANDOM = new Random();

    private DiceEngine() { }

    public static int roll(int sides, int luckPoints, int cursePoints,
                           boolean antiJinx, boolean percentageMode, int luckPercent, int cursePercent) {
        if (sides < 2) throw new IllegalArgumentException("sides must be >= 2");
        int raw = RANDOM.nextInt(sides) + 1;
        int result = raw;

        if (percentageMode) {
            int delta = (int) Math.round(sides * ((luckPercent - cursePercent) / 100.0));
            result += delta;
        } else {
            result += luckPoints - cursePoints;
        }

        if (antiJinx) {
            // Anti-zikamento is intentionally conservative: it prevents a roll
            // from landing at the absolute minimum, but never guarantees a high roll.
            if (result <= 1 && sides > 2) result = 2;
        }
        return clamp(result, 1, sides);
    }

    public static int rawRoll(int sides) {
        if (sides < 2) throw new IllegalArgumentException("sides must be >= 2");
        return RANDOM.nextInt(sides) + 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
