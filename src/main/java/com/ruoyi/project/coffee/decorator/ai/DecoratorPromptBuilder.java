package com.ruoyi.project.coffee.decorator.ai;

import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiTask;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfile;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfileService;

@Component
public class DecoratorPromptBuilder
{
    public static final String PROMPT_VERSION = "decorator-v3";

    @org.springframework.beans.factory.annotation.Autowired
    private ComponentAiProfileService profileService;

    public String background(DecoratorAiTask task, BackgroundSlotSpec slot)
    {
        ComponentAiProfile profile = profileService == null ? null : profileService.require(slot.getComponentKey());
        String forbidden = profile == null ? "QR codes, prices, buttons, navigation, or unrelated UI structure"
                : String.join(", ", profileService.values(profile.getForbiddenElementsJson()));
        if (task.getVisualIntentJson() != null && !task.getVisualIntentJson().trim().isEmpty())
        {
            return sketchBackground(task, slot, forbidden);
        }
        String composition = "aboutImage".equals(slot.getComponentKey())
                ? "For this aboutImage component, preserve the full-height story poster composition, the existing cafe scene and main subject(s), major silhouettes, framed text areas, and their relative positions. "
                : "Preserve the reference component's existing subjects, major shapes, text-block areas, relative scale, and spatial positions. ";
        return "Generate a premium coffee shop " + slot.getComponentKey() + " component background in " + task.getStylePreset() + " style. "
                + "Use the supplied reference image as the visual source: extract and preserve its material texture, paper grain, brush language, palette relationship, contrast, and decorative density, then reinterpret those textures in the requested style. "
                + "This is a style transfer for one UI component, not a full-page redesign. " + composition
                + "Change only the requested style language: palette, material, texture, linework, lighting, and decorative motifs. "
                + "Keep the reference's aspect ratio and composition unchanged: do not crop, reframe, stretch, rotate, move, remove, or redesign its occupied areas. "
                + "The reference composition takes precedence over any generic safe-area suggestion; keep an area quiet only when it is already quiet in the reference. Target " + slot.getOutputWidth() + "x"
                + slot.getOutputHeight() + " pixels exactly, with no output resizing that changes the component canvas or aspect ratio. Render mode: " + slot.getRenderMode()
                + ". Keep the component geometry and visual hierarchy compatible with the existing mini-program skin. Do not add new subjects, business UI, logos, QR codes, prices, buttons, navigation, or unrelated text. "
                + textInstruction(task) + " Do not generate " + forbidden + ". Text policy: "
                + (task.getTextMode() == null ? "NO_TEXT" : task.getTextMode())
                + ("EMBEDDED_TEXT".equals(task.getTextMode()) ? embeddedTextInstruction(task, slot) : "")
                + ". Real business UI outside the supplied reference image is rendered by the application. Merchant direction: " + task.getPromptText();
    }

    private String sketchBackground(DecoratorAiTask task, BackgroundSlotSpec slot, String forbidden)
    {
        return "Generate a premium coffee shop " + slot.getComponentKey() + " component background in "
                + task.getStylePreset() + " style. Use the supplied image only as a wireframe layout guide, not as finished artwork. "
                + "Follow its subject regions, relative scale, quiet text-safe areas, avoid areas, and spatial relationships. "
                + "Do not render guide boxes, labels, arrows, note pins, outlines, handles, or annotation marks in the final image. "
                + "Create a coherent commercial image at exactly " + slot.getOutputWidth() + "x" + slot.getOutputHeight()
                + " pixels with render mode " + slot.getRenderMode() + ". Keep every text-safe area visually quiet, low-detail, and free of subjects. "
                + "Do not generate business text, random text, logos, QR codes, prices, buttons, navigation, or unrelated UI. "
                + "Do not generate " + forbidden + ". Structured visual intent JSON: " + task.getVisualIntentJson()
                + ". Merchant direction: " + task.getPromptText();
    }

    public String productImage(DecoratorAiTask task, BackgroundSlotSpec slot)
    {
        return "Generate one polished product main image for a coffee shop menu. "
                + "Use the supplied original product photo as the strict visual reference: preserve the product identity, silhouette, proportions, recognizable ingredients, camera angle, and occupied area. "
                + "Apply only the merchant-requested style, palette, material, lighting, and texture language. "
                + "Keep the original product image aspect ratio and output exactly " + task.getTargetWidth() + "x" + task.getTargetHeight() + " pixels. "
                + "This is a single product photo, not a UI card or page redesign. Do not add text, price, button, logo, badge, QR code, border, menu layout, or unrelated objects. "
                + "The product must be large, sharp, centered, fully visible, and suitable for a mobile menu card. "
                + "Merchant direction: " + task.getPromptText();
    }

    private String textInstruction(DecoratorAiTask task)
    {
        if ("EMBEDDED_TEXT".equals(task.getTextMode()))
            return "The merchant-provided text is the only text allowed in the generated image. Render it exactly as provided, with no extra words, and make it large, crisp, high-contrast, foregrounded, and clearly legible on a mobile screen. The lettering may change its font, brushwork, ornament, and color to match the requested style, but it must never be tiny, blurred, smudged, distorted, or treated as abstract texture.";
        return "The reference image's text is authoritative component content: preserve every original word, character, number, line, text block, and reading order. Never omit, translate, replace, invent, blur, smudge, or turn the original text into abstract marks. The lettering may change its font, brushwork, ornament, and color to match the requested style, but it must remain large, crisp, high-contrast, foregrounded, and clearly legible on a mobile screen. NO_TEXT means do not add new text; it does not permit removing or degrading reference text.";
    }

    private String embeddedTextInstruction(DecoratorAiTask task, BackgroundSlotSpec slot)
    {
        String size = task.getSizePreset() == null ? "LARGE" : task.getSizePreset();
        String scale = "SMALL".equals(size) ? "about 20% of the canvas width"
                : "MEDIUM".equals(size) ? "about 30% of the canvas width"
                : "about 42% to 55% of the canvas width";
        if ("sectionBanner".equals(slot.getComponentKey())) scale = "about 65% to 85% of the canvas width and at least 40% of the canvas height";
        return ". Include only the exact text '" + task.getTextContent() + "' at " + task.getPlacementPreset()
                + ". Make the lettering prominent, large, crisp, and highly legible at mobile display size; it must occupy "
                + scale + ", with strong contrast against the background. Do not render the text as tiny decoration";
    }

    public String artText(DecoratorAiTask task, BackgroundSlotSpec slot)
    {
        return "Create only the exact decorative lettering '" + task.getTextContent() + "' in "
                + task.getStylePreset() + " coffee-brand style. Transparent background, no extra words, icons,"
                + " logos, QR codes, prices, buttons, or frame. Output a centered PNG suitable for overlay on a "
                + slot.getOutputWidth() + "x" + slot.getOutputHeight() + " component.";
    }
}
