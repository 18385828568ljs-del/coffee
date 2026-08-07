package com.ruoyi.project.abucoder.wxuser.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.abucoder.wxuser.mapper.AbucoderWxuserMapper;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
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

    @Autowired
    private UserProfileMapper userProfileMapper;

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
    @Transactional(rollbackFor = Exception.class)
    public int deleteAbucoderWxuserByIds(String ids)
    {
        String[] userIds = Convert.toStrArray(ids);
        int deleted = abucoderWxuserMapper.deleteAbucoderWxuserByIds(userIds);
        userProfileMapper.deleteBehaviorByUserIds(userIds);
        userProfileMapper.deleteProfilesByUserIds(userIds);
        return deleted;
    }

    /**
     * 删除微信用户信息
     * 
     * @param id 微信用户主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAbucoderWxuserById(Long id)
    {
        String[] userIds = { String.valueOf(id) };
        int deleted = abucoderWxuserMapper.deleteAbucoderWxuserById(id);
        userProfileMapper.deleteBehaviorByUserIds(userIds);
        userProfileMapper.deleteProfilesByUserIds(userIds);
        return deleted;
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
