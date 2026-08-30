package com.ruoyi.project.coffee.profile.domain;

/** A stable tag used by both profile calculation and recommendation ranking. */
public class ProfileTag
{
    private final String key;
    private final String dimension;
    private final String name;

    public ProfileTag(String key, String dimension, String name)
    {
        this.key = key;
        this.dimension = dimension;
        this.name = name;
    }

    public String getKey() { return key; }
    public String getDimension() { return dimension; }
    public String getName() { return name; }
}
