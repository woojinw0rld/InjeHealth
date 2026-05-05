package com.example.injehealth.util;

/** 부위 키 ↔ 한글명 / 이모지 변환 유틸 */
public class BodyPartLabels {

    public static final String[] KEYS = {"chest", "back", "legs", "shoulders", "arms", "cardio"};

    /** 영문 키 → 한글명 */
    public static String kr(String key) {
        if (key == null) return "";
        switch (key) {
            case "chest":     return "가슴";
            case "back":      return "등";
            case "legs":      return "하체";
            case "shoulders": return "어깨";
            case "arms":      return "팔";
            case "cardio":    return "유산소";
            default:          return key;
        }
    }

    /** 부위 대표 이모지 (시딩과 일치) */
    public static String emoji(String key) {
        if (key == null) return "💪";
        switch (key) {
            case "legs":   return "🦵";
            case "cardio": return "🏃";
            default:       return "💪";
        }
    }
}
