package com.example.kingdoms.election;

public enum ElectionPhase {
    IDLE, NOMINATION, CAMPAIGN, VOTING, RESOLVING, COMPLETE;

    public static ElectionPhase fromString(String s) {
        if (s == null) return IDLE;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return IDLE; }
    }
}
