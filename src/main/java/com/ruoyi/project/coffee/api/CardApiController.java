package com.ruoyi.project.coffee.api;

import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.card.domain.CardCampaign;
import com.ruoyi.project.coffee.card.domain.CardDrawRecord;
import com.ruoyi.project.coffee.card.service.CardDrawService;

@RestController
@RequestMapping("/api/card")
public class CardApiController
{
    private final CardDrawService service;
    private final WxUserTokenService tokenService;
    public CardApiController(CardDrawService service,WxUserTokenService tokenService) { this.service=service; this.tokenService=tokenService; }

    @GetMapping("/campaign/active")
    public AjaxResult active(HttpServletRequest request)
    {
        CardCampaign campaign=service.active(tokenService.resolveUserId(request));
        return AjaxResult.success(campaign);
    }

    @PostMapping("/draw")
    public AjaxResult draw(@RequestBody Map<String,Object> body)
    {
        Long campaignId=longValue(body,"campaignId");
        Object requestValue=body==null?null:body.get("requestNo");
        String requestNo=requestValue==null?null:String.valueOf(requestValue);
        return AjaxResult.success(service.draw(campaignId,WxUserAuthContext.getCurrentUserId(),requestNo));
    }

    @GetMapping("/result/{drawId}")
    public AjaxResult result(@PathVariable Long drawId)
    {
        CardDrawRecord record=service.result(drawId,WxUserAuthContext.getCurrentUserId());
        return record==null?AjaxResult.error("抽卡记录不存在"):AjaxResult.success(record);
    }

    @GetMapping("/my")
    public AjaxResult my() { return AjaxResult.success(service.my(WxUserAuthContext.getCurrentUserId())); }

    private Long longValue(Map<String,Object> body,String key)
    { Object value=body==null?null:body.get(key); return value==null?null:Long.valueOf(String.valueOf(value)); }
}
