package com.hoangphuc.ddd.domain.service.impl;

import com.hoangphuc.ddd.domain.model.entity.Station;
import com.hoangphuc.ddd.domain.repository.StationRepository;
import com.hoangphuc.ddd.domain.service.StationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationDomainServiceImpl implements StationDomainService {

    private final StationRepository stationRepository;

    @Override
    public List<Station> listStations() {
        return stationRepository.findAllOrdered();
    }
}
