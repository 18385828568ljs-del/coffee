package com.ruoyi.project.coffee.profile.domain;

import java.util.Date;

/** A category or product preference from the profile snapshot. */
public class ProfilePreference
{
    private Long id;
    private String name;
    private Double score;
    private Integer evidenceCount;
    private Date lastEvidenceTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Integer getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(Integer evidenceCount) { this.evidenceCount = evidenceCount; }
    public Date getLastEvidenceTime() { return lastEvidenceTime; }
    public void setLastEvidenceTime(Date lastEvidenceTime) { this.lastEvidenceTime = lastEvidenceTime; }
}
