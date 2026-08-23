package com.ruoyi.project.coffee.decorator.asset.domain;

public class BackgroundSlotSpec
{
    private Long id;
    private String slotKey;
    private String componentKey;
    private Integer specVersion;
    private Integer logicalWidth;
    private Integer logicalHeight;
    private Integer outputWidth;
    private Integer outputHeight;
    private String renderMode;
    private Integer uploadMinWidth;
    private Integer uploadMinHeight;
    private Long maxFileSize;
    private Boolean aiEnabled;
    private String safeAreaJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public String getComponentKey() { return componentKey; }
    public void setComponentKey(String componentKey) { this.componentKey = componentKey; }
    public Integer getSpecVersion() { return specVersion; }
    public void setSpecVersion(Integer specVersion) { this.specVersion = specVersion; }
    public Integer getLogicalWidth() { return logicalWidth; }
    public void setLogicalWidth(Integer logicalWidth) { this.logicalWidth = logicalWidth; }
    public Integer getLogicalHeight() { return logicalHeight; }
    public void setLogicalHeight(Integer logicalHeight) { this.logicalHeight = logicalHeight; }
    public Integer getOutputWidth() { return outputWidth; }
    public void setOutputWidth(Integer outputWidth) { this.outputWidth = outputWidth; }
    public Integer getOutputHeight() { return outputHeight; }
    public void setOutputHeight(Integer outputHeight) { this.outputHeight = outputHeight; }
    public String getRenderMode() { return renderMode; }
    public void setRenderMode(String renderMode) { this.renderMode = renderMode; }
    public Integer getUploadMinWidth() { return uploadMinWidth; }
    public void setUploadMinWidth(Integer uploadMinWidth) { this.uploadMinWidth = uploadMinWidth; }
    public Integer getUploadMinHeight() { return uploadMinHeight; }
    public void setUploadMinHeight(Integer uploadMinHeight) { this.uploadMinHeight = uploadMinHeight; }
    public Long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(Long maxFileSize) { this.maxFileSize = maxFileSize; }
    public Boolean getAiEnabled() { return aiEnabled; }
    public void setAiEnabled(Boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public String getSafeAreaJson() { return safeAreaJson; }
    public void setSafeAreaJson(String safeAreaJson) { this.safeAreaJson = safeAreaJson; }
}
