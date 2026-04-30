package com.elecciones.election.service;

import com.elecciones.election.entity.Election;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostCloseAsyncService {

    @Async
    public void process(Election election) {

        try {
            Thread.sleep(2000);

            log.info("Procesamiento async finalizado para elección {}",
                    election.getTitle());

        } catch (Exception ex) {
            log.error("Error async", ex);
        }
    }
}