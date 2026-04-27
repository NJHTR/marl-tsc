package com.njhtr.marltsc.fusion.domain.bo;

import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlignedDataBucket {
    private Long bucketTimestamp;
    private List<UnifiedTrafficData> dataList;
}
