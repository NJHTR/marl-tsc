package com.njhtr.marltsc.route.infrastructure.repository;

import com.njhtr.marltsc.route.domain.entity.IntersectionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntersectionRepository extends Neo4jRepository<IntersectionNode, String> {
}
