package com.ruoyi.project.coffee.scanOrder.wx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.RuoYiConfig;

/**
 * 微信小程序码(getwxacodeunlimit)生成服务
 *
 * 调用官方接口:
 *   POST https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=ACCESS_TOKEN
 *
 * 成功: 直接返回 image/jpeg 二进制流
 * 失败: 返回 application/json,如 {"errcode":41030,"errmsg":"invalid page"}
 *
 * 图片保存策略:
 *   本地文件,路径为 ${ruoyi.profile}/scanQrcode/{shopId}_{tableNo}_{ts}.jpg
 *   对外访问走若依 ResourceHandler 的 /profile/** 映射,
 *   最终 URL 形如 http(s)://<server>/profile/scanQrcode/xxx.jpg
 *
 * 如需改为腾讯云 COS,参考 CosFileStorageService,只需新增一个
 * 接收 byte[] 的 upload 重载即可;当前保持最小实现,本地存储更利于开发测试。
 */
@Service
public class WxaCodeService
{
    private static final Logger log = LoggerFactory.getLogger(WxaCodeService.class);

    private static final String WXA_CODE_URL =
        "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=";

    private static final String SUB_DIR = "scanQrcode";

    @Autowired
    private WxAccessTokenService wxAccessTokenService;

    /**
     * 生成小程序码并落到本地磁盘。
     *
     * @param page     小程序页面,例如 pages/scan/menu
     * @param scene    scene 参数,最长 32 字节,例如 shopId=1&tableNo=A01
     * @param fileName 保存文件名(不含扩展名),调用方负责保证唯一
     * @return 可直接存库 / 回前端的访问 URL(以 /profile 开头的相对路径)
     */
    public String generateWxaCode(String page, String scene, String fileName)
    {
        if (StringUtils.isEmpty(page))
        {
            throw new ServiceException("page 不能为空");
        }
        if (StringUtils.isEmpty(scene))
        {
            throw new ServiceException("scene 不能为空");
        }
        if (scene.getBytes(StandardCharsets.UTF_8).length > 32)
        {
            throw new ServiceException("scene 长度超过 32 字节限制,当前: " + scene);
        }
        if (StringUtils.isEmpty(fileName))
        {
            throw new ServiceException("fileName 不能为空");
        }

        String accessToken = wxAccessTokenService.getAccessToken();
        byte[] bytes = requestWxaCode(accessToken, page, scene);

        File dir = new File(RuoYiConfig.getProfile(), SUB_DIR);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new ServiceException("小程序码保存目录创建失败: " + dir.getAbsolutePath());
        }
        String safeName = sanitizeFileName(fileName) + ".jpg";
        File dest = new File(dir, safeName);
        try (FileOutputStream fos = new FileOutputStream(dest))
        {
            fos.write(bytes);
        }
        catch (IOException e)
        {
            throw new ServiceException("小程序码写入磁盘失败: " + e.getMessage());
        }
        log.info("小程序码已保存: {}", dest.getAbsolutePath());

        // 返回 /profile/scanQrcode/xxx.jpg
        return Constants.RESOURCE_PREFIX + "/" + SUB_DIR + "/" + safeName;
    }

    /**
     * 调用微信官方接口,拿到二进制字节或抛错。
     * JDK 原生 HttpURLConnection,不引入新依赖。
     */
    private byte[] requestWxaCode(String accessToken, String page, String scene)
    {
        HttpURLConnection conn = null;
        try
        {
            URL url = new URL(WXA_CODE_URL + URLEncoder.encode(accessToken, "UTF-8"));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(20_000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "image/jpeg, application/json");

            JSONObject body = new JSONObject();
            body.put("scene", scene);
            body.put("page", page);
            // check_path: 线上版本不存在或未发布时设为 false 避免 41030
            body.put("check_path", false);
            body.put("env_version", "trial");
            body.put("width", 430);

            byte[] payload = body.toJSONString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(payload);
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] raw = readAllBytes(stream);

            // 微信返回失败时 Content-Type 是 application/json 或 text/plain
            String contentType = conn.getContentType();
            if (contentType != null && contentType.contains("json"))
            {
                String text = new String(raw, StandardCharsets.UTF_8);
                JSONObject err = JSON.parseObject(text);
                throw new ServiceException(
                    "微信小程序码接口返回错误: errcode=" + err.getString("errcode")
                    + ", errmsg=" + err.getString("errmsg"));
            }
            if (raw.length == 0)
            {
                throw new ServiceException("微信小程序码接口返回空内容");
            }
            return raw;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("调用微信小程序码接口失败: " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException
    {
        if (in == null) return new byte[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1)
        {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private String sanitizeFileName(String name)
    {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
