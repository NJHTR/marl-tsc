package com.njhtr.marltsc.route.infrastructure.repository;

import com.njhtr.marltsc.route.domain.entity.RoadSegment;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadSegmentRepository extends Neo4jRepository<RoadSegment, Long> {

    @Query("MATCH (from:Intersection {id: $fromId})-[r:ROAD_SEGMENT]->(to:Intersection {id: $toId}) RETURN r, to")
    List<RoadSegment> findByFromIdAndToId(String fromId, String toId);
}
