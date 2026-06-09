package com.ruoyi.project.abucoder.wxuser.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.abucoder.wxuser.mapper.AbucoderWxuserMapper;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 微信用户Service业务层处理
 * 
 * @author ruoyi
 * @date 2022-05-27
 */
@Service
public class AbucoderWxuserServiceImpl implements IAbucoderWxuserService 
{
    @Autowired
    private AbucoderWxuserMapper abucoderWxuserMapper;

    /**
     * 查询微信用户
     * 
     * @param id 微信用户主键
     * @return 微信用户
     */
    @Override
    public AbucoderWxuser selectAbucoderWxuserById(Long id)
    {
        return abucoderWxuserMapper.selectAbucoderWxuserById(id);
    }

    /**
     * 查询微信用户列表
     * 
     * @param abucoderWxuser 微信用户
     * @return 微信用户
     */
    @Override
    public List<AbucoderWxuser> selectAbucoderWxuserList(AbucoderWxuser abucoderWxuser)
    {
        return abucoderWxuserMapper.selectAbucoderWxuserList(abucoderWxuser);
    }

    /**
     * 新增微信用户
     * 
     * @param abucoderWxuser 微信用户
     * @return 结果
     */
    @Override
    public int insertAbucoderWxuser(AbucoderWxuser abucoderWxuser)
    {
        abucoderWxuser.setCreateTime(DateUtils.getNowDate());
        return abucoderWxuserMapper.insertAbucoderWxuser(abucoderWxuser);
    }

    /**
     * 修改微信用户
     * 
     * @param abucoderWxuser 微信用户
     * @return 结果
     */
    @Override
    public int updateAbucoderWxuser(AbucoderWxuser abucoderWxuser)
    {
        abucoderWxuser.setUpdateTime(DateUtils.getNowDate());
        return abucoderWxuserMapper.updateAbucoderWxuser(abucoderWxuser);
    }

    /**
     * 批量删除微信用户
     * 
     * @param ids 需要删除的微信用户主键
     * @return 结果
     */
    @Override
    public int deleteAbucoderWxuserByIds(String ids)
    {
        return abucoderWxuserMapper.deleteAbucoderWxuserByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除微信用户信息
     * 
     * @param id 微信用户主键
     * @return 结果
     */
    @Override
    public int deleteAbucoderWxuserById(Long id)
    {
        return abucoderWxuserMapper.deleteAbucoderWxuserById(id);
    }

    /**
     * 通过openid查询相关信息
     * @param openid
     * @return
     */
    @Override
    public AbucoderWxuser selectAbucoderWxuserOpenID(String openid) {
        return abucoderWxuserMapper.selectAbucoderWxuserOpenID(openid);
    }

}
