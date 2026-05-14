package org.bacon.vanishPlusPlusPlus.model;

public final class PlayerData {
    private volatile boolean vanished;
    private volatile VanishRules rules;
    private volatile String originalListName;
    private volatile String pendingChatMessage;
    private volatile boolean allowNextChat;

    public PlayerData(VanishRules rules) {
        this.rules = rules;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public VanishRules getRules() {
        return rules;
    }

    public void setRules(VanishRules rules) {
        this.rules = rules;
    }

    public String getOriginalListName() {
        return originalListName;
    }

    public void setOriginalListName(String originalListName) {
        this.originalListName = originalListName;
    }

    public String getPendingChatMessage() {
        return pendingChatMessage;
    }

    public void setPendingChatMessage(String pendingChatMessage) {
        this.pendingChatMessage = pendingChatMessage;
    }

    public boolean isAllowNextChat() {
        return allowNextChat;
    }

    public void setAllowNextChat(boolean allowNextChat) {
        this.allowNextChat = allowNextChat;
    }

    public void clearPendingChat() {
        pendingChatMessage = null;
        allowNextChat = false;
    }
}
