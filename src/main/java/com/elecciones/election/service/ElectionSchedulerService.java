package com.elecciones.election.service;

import com.elecciones.common.enums.ElectionStatus;
import com.elecciones.election.entity.Election;
import com.elecciones.election.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElectionSchedulerService {

    private final ElectionRepository electionRepository;
    private final ElectionService electionService;

    @Scheduled(fixedDelay = 60000)
    public void closeExpiredElections() {

        List<Election> expired =
                electionRepository.findByStatusAndEndDateBefore(
                        ElectionStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (Election election : expired) {

            try {
                electionService.closeAutomatically(election);

                log.info("Elección cerrada automáticamente: {}",
                        election.getTitle());

            } catch (Exception ex) {
                log.error("Error cerrando elección {}", election.getId());
            }
        }
    }
}