package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Station;

import java.util.List;

public interface StationRepository {

    /** All stations, sorted in North–South route order. */
    List<Station> findAllOrdered();

    long count();

    List<Station> saveAll(List<Station> stations);
}
