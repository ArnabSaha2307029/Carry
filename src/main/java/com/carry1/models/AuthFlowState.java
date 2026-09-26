package com.carry1.models;

public class AuthFlowState {
    public static boolean isSignUpMode = false;
    public static Role selectedRole = null;

    // সেশন মেমোরিতে রাখার জন্য গ্লোবাল ভেরিয়েবল
    public static User currentUser = null;

    // পেজ ব্যাক করলে বা প্রসেস শেষ হলে ডেটা ক্লিয়ার করার জন্য
    public static void clear() {
        isSignUpMode = false;
        selectedRole = null;
    }
}