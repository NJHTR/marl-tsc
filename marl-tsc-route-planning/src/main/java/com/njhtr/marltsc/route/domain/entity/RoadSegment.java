package com.njhtr.marltsc.route.domain.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@RelationshipProperties
public class RoadSegment {
    @Id
    private String id;
    private Double length;
    private Integer lanes;
    private Double freeFlowSpeed;
    private Double currentSpeed;
    private Double currentTravelTime;

    @Property
    private String fromId;

    @Property
    private String toId;

    @TargetNode
    private IntersectionNode targetNode;
}
