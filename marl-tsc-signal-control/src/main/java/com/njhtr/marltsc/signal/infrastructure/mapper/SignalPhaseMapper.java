package com.njhtr.marltsc.signal.infrastructure.mapper;

import com.njhtr.marltsc.signal.domain.entity.SignalPhase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SignalPhaseMapper {

    List<SignalPhase> selectByPlanId(@Param("planId") String planId);
}
