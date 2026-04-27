package com.njhtr.marltsc.coopt.domain.bo;

import lombok.Data;
import java.util.List;

@Data
public class RouteChangeBO {
    private String vehicleId;
    private List<String> newRoute;
}
