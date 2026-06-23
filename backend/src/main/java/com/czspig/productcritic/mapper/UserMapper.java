package com.czspig.productcritic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czspig.productcritic.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
