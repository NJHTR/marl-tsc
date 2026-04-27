package com.njhtr.marltsc.coopt.infrastructure.mapper;

import com.njhtr.marltsc.coopt.domain.entity.OptimizationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OptimizationLogMapper {
    
    int insert(OptimizationLog log);
    
    List<OptimizationLog> selectRecent(@Param("limit") int limit);
}
