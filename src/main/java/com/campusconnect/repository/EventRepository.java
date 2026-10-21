package com.campusconnect.repository;

import com.campusconnect.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Full listing with optional keyword search and category filter.
     * Only UPCOMING events are returned unless status is explicitly included.
     */
    @Query("""
            SELECT e FROM Event e
            WHERE e.status = 'UPCOMING'
              AND (:category IS NULL OR LOWER(e.category) = LOWER(:category))
              AND (:keyword  IS NULL
                   OR LOWER(e.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY e.startTime ASC
            """)
    List<Event> search(@Param("keyword") String keyword,
                       @Param("category") String category);
}
