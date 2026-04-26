package com.njhtr.marltsc.signal.infrastructure.mapper;

import com.njhtr.marltsc.signal.domain.entity.SignalPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Signal plan MyBatis mapper.
 */
@Mapper
public interface SignalPlanMapper {

    SignalPlan selectById(@Param("planId") String planId);

    List<SignalPlan> selectByIntersectionId(@Param("intersectionId") String intersectionId);

    int insert(SignalPlan plan);

    int update(SignalPlan plan);
}
