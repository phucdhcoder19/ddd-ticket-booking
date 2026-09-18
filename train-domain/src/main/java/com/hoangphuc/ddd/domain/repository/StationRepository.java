package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Station;

import java.util.List;

public interface StationRepository {

    /** Toàn bộ ga, đã sắp theo thứ tự trên tuyến Bắc – Nam. */
    List<Station> findAllOrdered();

    long count();

    List<Station> saveAll(List<Station> stations);
}
