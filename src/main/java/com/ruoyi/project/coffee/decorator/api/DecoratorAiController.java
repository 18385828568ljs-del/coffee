package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.ai.DecoratorAiService;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;

@RestController
@RequestMapping("/coffee/decorator/ai")
public class DecoratorAiController
{
    @Autowired private DecoratorAiService aiService;

    @PostMapping("/background/tasks")
    public AjaxResult background(@RequestBody DecoratorAiTaskRequest request)
    { return AjaxResult.success(aiService.createBackground(TenantContextHolder.require(), request)); }

    @PostMapping("/art-text/tasks")
    public AjaxResult artText(@RequestBody DecoratorAiTaskRequest request)
    { return AjaxResult.success(aiService.createArtText(TenantContextHolder.require(), request)); }

    @PostMapping("/tasks")
    public AjaxResult createTask(@RequestBody DecoratorAiTaskRequest request)
    { return AjaxResult.success(aiService.createTask(TenantContextHolder.require(), request)); }

    @GetMapping("/tasks/{taskId}")
    public AjaxResult task(@PathVariable Long taskId)
    { return AjaxResult.success(aiService.task(TenantContextHolder.require(), taskId)); }

    @GetMapping("/tasks/{taskId}/results")
    public AjaxResult results(@PathVariable Long taskId)
    { return AjaxResult.success(aiService.results(TenantContextHolder.require(), taskId)); }

    @PostMapping("/results/{resultId}/accept")
    public AjaxResult accept(@PathVariable Long resultId)
    { return AjaxResult.success(aiService.accept(TenantContextHolder.require(), resultId)); }

    @PostMapping("/results/{resultId}/apply")
    public AjaxResult apply(@PathVariable Long resultId, @RequestBody DecoratorAiApplyRequest request)
    { return AjaxResult.success(aiService.apply(TenantContextHolder.require(), resultId, request)); }

    @PostMapping("/results/apply-batch")
    public AjaxResult applyBatch(@RequestBody DecoratorAiBatchApplyRequest request)
    { return AjaxResult.success(aiService.applyBatch(TenantContextHolder.require(), request)); }
}
