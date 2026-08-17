package com.ruoyi.project.coffee.decorator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ModelMap;

class DecoratorWorkbenchControllerTest
{
    private DecoratorWorkbenchController controller;

    @BeforeEach
    void setUp()
    {
        controller = new DecoratorWorkbenchController();
    }

    @Test
    void exposesConfiguredHttpOrRelativePreviewUrl()
    {
        ModelMap absoluteModel = render("https://preview.example.com/#/pages/decorator-preview/index");
        assertEquals("https://preview.example.com/#/pages/decorator-preview/index",
                absoluteModel.get("h5PreviewUrl"));

        ModelMap relativeModel = render("/h5/#/pages/decorator-preview/index");
        assertEquals("/h5/#/pages/decorator-preview/index", relativeModel.get("h5PreviewUrl"));
    }

    @Test
    void rejectsUnsafeOrMalformedPreviewUrl()
    {
        assertEquals("", render("javascript:alert(1)").get("h5PreviewUrl"));
        assertEquals("", render("//preview.example.com/page").get("h5PreviewUrl"));
        assertEquals("", render("not a url").get("h5PreviewUrl"));
    }

    private ModelMap render(String url)
    {
        ReflectionTestUtils.setField(controller, "h5PreviewUrl", url);
        ModelMap model = new ModelMap();
        assertEquals("coffee/decorator/workbench", controller.workbench(model));
        return model;
    }
}
