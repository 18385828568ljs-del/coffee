package com.ruoyi.project.coffee.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.RuoYiConfig;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;

/**
 * 桌台小程序码管理接口
 *
 * 生成的是微信官方【无限量小程序码】(getwxacodeunlimit),
 * 扫码后打开 pages/scan/menu,携带 scene=shopId=X&tableNo=Y。
 */
@RestController
@RequestMapping("/api/scanTableQrcode")
public class ScanTableQrcodeApiController extends BaseController
{
    @Autowired
    private IScanTableQrcodeService scanTableQrcodeService;

    /** 生成单个桌号小程序码 */
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求参数不能为空");
        }
        Long shopId = toLong(body.get("shopId"));
        String shopName = toStr(body.get("shopName"));
        String tableNo = toStr(body.get("tableNo"));
        String scene = toStr(body.get("scene"));
        if (StringUtils.isEmpty(tableNo))
        {
            return AjaxResult.error("桌号不能为空");
        }
        try
        {
            ScanTableQrcode record = scanTableQrcodeService.generateOne(shopId, shopName, tableNo, scene);
            return AjaxResult.success("生成成功", record);
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 批量生成,传入 tableNos 数组。
     * 也兼容 prefix + start + end 的区间生成:
     *   { "shopId":1, "prefix":"A", "start":1, "end":10, "digits":2 }
     * -> A01, A02, ..., A10
     */
    @PostMapping("/batchGenerate")
    public AjaxResult batchGenerate(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求参数不能为空");
        }
        Long shopId = toLong(body.get("shopId"));
        String shopName = toStr(body.get("shopName"));
        String scene = toStr(body.get("scene"));

        List<String> tableNos = new ArrayList<String>();
        Object rawList = body.get("tableNos");
        if (rawList instanceof List)
        {
            for (Object o : (List<?>) rawList)
            {
                if (o != null && StringUtils.isNotEmpty(o.toString()))
                {
                    tableNos.add(o.toString().trim());
                }
            }
        }
        if (tableNos.isEmpty())
        {
            // 区间模式
            String prefix = toStr(body.get("prefix"));
            Integer start = toInt(body.get("start"));
            Integer end = toInt(body.get("end"));
            Integer digits = toInt(body.get("digits"));
            if (start != null && end != null && end >= start)
            {
                int width = digits == null ? 2 : digits;
                String p = prefix == null ? "" : prefix;
                for (int i = start; i <= end; i++)
                {
                    tableNos.add(p + String.format("%0" + width + "d", i));
                }
            }
        }
        if (tableNos.isEmpty())
        {
            return AjaxResult.error("请提供 tableNos 数组,或 prefix+start+end 区间");
        }

        try
        {
            List<ScanTableQrcode> list = scanTableQrcodeService.batchGenerate(shopId, shopName, tableNos, scene);
            return AjaxResult.success("批量生成完成", list);
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public AjaxResult list(
        @RequestParam(value = "shopId", required = false) Long shopId,
        @RequestParam(value = "tableNo", required = false) String tableNo,
        @RequestParam(value = "status", required = false) Integer status)
    {
        ScanTableQrcode query = new ScanTableQrcode();
        query.setShopId(shopId);
        query.setTableNo(StringUtils.isEmpty(tableNo) ? null : tableNo);
        query.setStatus(status);
        return AjaxResult.success(scanTableQrcodeService.selectList(query));
    }

    /**
     * 下载二维码图片,浏览器会触发保存。
     * URL 形如: /api/scanTableQrcode/download/{id}
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException
    {
        ScanTableQrcode record = scanTableQrcodeService.selectById(id);
        if (record == null || StringUtils.isEmpty(record.getQrUrl()))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1,\"msg\":\"二维码不存在\"}");
            return;
        }

        String qrUrl = record.getQrUrl();
        if (!qrUrl.startsWith(Constants.RESOURCE_PREFIX + "/"))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1,\"msg\":\"下载接口仅支持本地存储的二维码\"}");
            return;
        }
        String relative = qrUrl.substring(Constants.RESOURCE_PREFIX.length());
        File file = new File(RuoYiConfig.getProfile(), relative);
        if (!file.exists() || !file.isFile())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1,\"msg\":\"二维码文件已丢失\"}");
            return;
        }

        String downloadName = "qrcode_shop" + record.getShopId() + "_" + record.getTableNo() + ".jpg";
        response.setContentType("image/jpeg");
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + URLEncoder.encode(downloadName, "UTF-8") + "\"");
        try (InputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream())
        {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        ScanTableQrcode existing = scanTableQrcodeService.selectById(id);
        if (existing == null)
        {
            return AjaxResult.error("记录不存在");
        }
        return toAjax(scanTableQrcodeService.deleteById(id));
    }

    private String toStr(Object v)
    {
        return v == null ? null : v.toString();
    }

    private Long toLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
