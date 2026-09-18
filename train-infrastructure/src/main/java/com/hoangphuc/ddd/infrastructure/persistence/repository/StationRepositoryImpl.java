package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.Station;
import com.hoangphuc.ddd.domain.repository.StationRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.StationJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StationRepositoryImpl implements StationRepository {

    private final StationJPAMapper stationJPAMapper;

    @Override
    public List<Station> findAllOrdered() {
        return stationJPAMapper.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public long count() {
        return stationJPAMapper.count();
    }

    @Override
    public List<Station> saveAll(List<Station> stations) {
        return stationJPAMapper.saveAll(stations);
    }
}
