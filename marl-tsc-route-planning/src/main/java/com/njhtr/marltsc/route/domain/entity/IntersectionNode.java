package com.njhtr.marltsc.route.domain.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Data
@Node("Intersection")
public class IntersectionNode {

    @Id
    private String id;

    @Property
    private String name;

    @Property
    private Double longitude;

    @Property
    private Double latitude;
}
