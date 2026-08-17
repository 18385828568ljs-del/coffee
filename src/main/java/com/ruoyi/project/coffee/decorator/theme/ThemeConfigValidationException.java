package com.ruoyi.project.coffee.decorator.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThemeConfigValidationException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final List<String> errors;

    public ThemeConfigValidationException(List<String> errors)
    {
        super(errors == null || errors.isEmpty() ? "主题配置不合法" : errors.get(0));
        this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
