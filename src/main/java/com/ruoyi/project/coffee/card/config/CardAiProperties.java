package com.ruoyi.project.coffee.card.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.card")
public class CardAiProperties
{
    private boolean enabled = false;
    private int pollSeconds = 5;
    private int concurrency = 2;
    private int staleMinutes = 15;
    private int maxAttempts = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getPollSeconds() { return pollSeconds; }
    public void setPollSeconds(int pollSeconds) { this.pollSeconds = pollSeconds; }
    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
    public int getStaleMinutes() { return staleMinutes; }
    public void setStaleMinutes(int staleMinutes) { this.staleMinutes = staleMinutes; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
