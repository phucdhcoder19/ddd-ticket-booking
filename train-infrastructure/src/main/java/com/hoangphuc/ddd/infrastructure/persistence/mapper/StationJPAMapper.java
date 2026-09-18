package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationJPAMapper extends JpaRepository<Station, Long> {

    /** Spring Data tự sinh câu ORDER BY display_order ASC từ chính tên hàm này. */
    List<Station> findAllByOrderByDisplayOrderAsc();
}
