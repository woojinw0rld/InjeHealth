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

    /** 부위별 기본 drawable 리소스 이름 */
    public static String imageRef(String key) {
        if (key == null) return "chest_image";
        switch (key) {
            case "chest":
            case "가슴":
                return "chest_image";
            case "back":
            case "등":
                return "back_image";
            case "legs":
            case "하체":
                return "leg_image";
            case "arms":
            case "팔":
                return "arm_image";
            case "cardio":
            case "유산소":
                return "cardio_image";
            case "shoulders":
            case "어깨":
                return "arm_image";
            default:
                return "chest_image";
        }
    }
}
