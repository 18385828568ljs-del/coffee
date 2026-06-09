package com.ruoyi.project.abucoder.wxuser.service;

import java.util.List;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;

/**
 * 微信用户Service接口
 * 
 * @author ruoyi
 * @date 2022-05-27
 */
public interface IAbucoderWxuserService 
{
    /**
     * 查询微信用户
     * 
     * @param id 微信用户主键
     * @return 微信用户
     */
    public AbucoderWxuser selectAbucoderWxuserById(Long id);

    /**
     * 查询微信用户列表
     * 
     * @param abucoderWxuser 微信用户
     * @return 微信用户集合
     */
    public List<AbucoderWxuser> selectAbucoderWxuserList(AbucoderWxuser abucoderWxuser);

    /**
     * 新增微信用户
     * 
     * @param abucoderWxuser 微信用户
     * @return 结果
     */
    public int insertAbucoderWxuser(AbucoderWxuser abucoderWxuser);

    /**
     * 修改微信用户
     * 
     * @param abucoderWxuser 微信用户
     * @return 结果
     */
    public int updateAbucoderWxuser(AbucoderWxuser abucoderWxuser);

    /**
     * 批量删除微信用户
     * 
     * @param ids 需要删除的微信用户主键集合
     * @return 结果
     */
    public int deleteAbucoderWxuserByIds(String ids);

    /**
     * 删除微信用户信息
     * 
     * @param id 微信用户主键
     * @return 结果
     */
    public int deleteAbucoderWxuserById(Long id);

    /**
     * 通过openid查询相关信息
     * @param openid
     * @return
     */
    public AbucoderWxuser selectAbucoderWxuserOpenID(String openid);

}
