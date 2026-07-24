package com.ruoyi.project.abucoder.banners.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.abucoder.banners.mapper.AbucoderBannersMapper;
import com.ruoyi.project.abucoder.banners.domain.AbucoderBanners;
import com.ruoyi.project.abucoder.banners.service.IAbucoderBannersService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 轮播图Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2022-05-27
 */
@Service
public class AbucoderBannersServiceImpl implements IAbucoderBannersService 
{
    @Autowired
    private AbucoderBannersMapper abucoderBannersMapper;

    /**
     * 查询轮播图
     * 
     * @param id 轮播图主键
     * @return 轮播图
     */
    @Override
    public AbucoderBanners selectAbucoderBannersById(Long id)
    {
        return abucoderBannersMapper.selectAbucoderBannersById(id);
    }

    /**
     * 查询轮播图列表
     * 
     * @param abucoderBanners 轮播图
     * @return 轮播图
     */
    @Override
    public List<AbucoderBanners> selectAbucoderBannersList(AbucoderBanners abucoderBanners)
    {
        return abucoderBannersMapper.selectAbucoderBannersList(abucoderBanners);
    }

    /**
     * 新增轮播图
     * 
     * @param abucoderBanners 轮播图
     * @return 结果
     */
    @Override
    public int insertAbucoderBanners(AbucoderBanners abucoderBanners)
    {
        abucoderBanners.setCreateTime(DateUtils.getNowDate());
        return abucoderBannersMapper.insertAbucoderBanners(abucoderBanners);
    }

    /**
     * 修改轮播图
     * 
     * @param abucoderBanners 轮播图
     * @return 结果
     */
    @Override
    public int updateAbucoderBanners(AbucoderBanners abucoderBanners)
    {
        abucoderBanners.setUpdateTime(DateUtils.getNowDate());
        return abucoderBannersMapper.updateAbucoderBanners(abucoderBanners);
    }

    /**
     * 批量删除轮播图
     * 
     * @param ids 需要删除的轮播图主键
     * @return 结果
     */
    @Override
    public int deleteAbucoderBannersByIds(String ids)
    {
        return abucoderBannersMapper.deleteAbucoderBannersByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除轮播图信息
     * 
     * @param id 轮播图主键
     * @return 结果
     */
    @Override
    public int deleteAbucoderBannersById(Long id)
    {
        return abucoderBannersMapper.deleteAbucoderBannersById(id);
    }
}
