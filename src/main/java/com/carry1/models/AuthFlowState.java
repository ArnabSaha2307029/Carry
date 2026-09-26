package com.carry1.models;

public class AuthFlowState {
    public static boolean isSignUpMode = false;
    public static Role selectedRole = null;

    
    public static User currentUser = null;

    
    public static void clear() {
        isSignUpMode = false;
        selectedRole = null;
    }
}