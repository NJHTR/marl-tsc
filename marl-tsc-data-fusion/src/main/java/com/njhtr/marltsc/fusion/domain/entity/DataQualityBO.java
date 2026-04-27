package com.njhtr.marltsc.fusion.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityBO {
    private Double completeness;
    private Double timeliness;
    private Double accuracy;
    private Double confidence;
}
