package com.ruoyi.project.coffee.decorator.ai.domain;

import java.util.Date;

public class DecoratorAiResult
{
    private Long id;
    private Long merchantId;
    private Long taskId;
    private Integer candidateNo;
    private String storageKey;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer sourceWidth;
    private Integer sourceHeight;
    private Integer finalWidth;
    private Integer finalHeight;
    private Long byteSize;
    private String checksumSha256;
    private Boolean hasAlpha;
    private Boolean postProcessed;
    private String textValidationStatus;
    private String textValidationDetail;
    private String status;
    private Long acceptedAssetId;
    private Date acceptedAt;
    private Date expiresAt;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getCandidateNo() { return candidateNo; }
    public void setCandidateNo(Integer candidateNo) { this.candidateNo = candidateNo; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getSourceWidth() { return sourceWidth; }
    public void setSourceWidth(Integer sourceWidth) { this.sourceWidth = sourceWidth; }
    public Integer getSourceHeight() { return sourceHeight; }
    public void setSourceHeight(Integer sourceHeight) { this.sourceHeight = sourceHeight; }
    public Integer getFinalWidth() { return finalWidth; }
    public void setFinalWidth(Integer finalWidth) { this.finalWidth = finalWidth; }
    public Integer getFinalHeight() { return finalHeight; }
    public void setFinalHeight(Integer finalHeight) { this.finalHeight = finalHeight; }
    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long byteSize) { this.byteSize = byteSize; }
    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
    public Boolean getHasAlpha() { return hasAlpha; }
    public void setHasAlpha(Boolean hasAlpha) { this.hasAlpha = hasAlpha; }
    public Boolean getPostProcessed() { return postProcessed; }
    public void setPostProcessed(Boolean postProcessed) { this.postProcessed = postProcessed; }
    public String getTextValidationStatus() { return textValidationStatus; }
    public void setTextValidationStatus(String textValidationStatus) { this.textValidationStatus = textValidationStatus; }
    public String getTextValidationDetail() { return textValidationDetail; }
    public void setTextValidationDetail(String textValidationDetail) { this.textValidationDetail = textValidationDetail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAcceptedAssetId() { return acceptedAssetId; }
    public void setAcceptedAssetId(Long acceptedAssetId) { this.acceptedAssetId = acceptedAssetId; }
    public Date getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Date acceptedAt) { this.acceptedAt = acceptedAt; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getUrl() { return storageKey; }
}
