package com.elecciones.election.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/elections")
public class ElectionController {

    @GetMapping
    public List<String> listElections() {
        return List.of("Endpoint protegido funcionando");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createElectionPlaceholder() {
        return "Solo ADMIN puede crear elecciones";
    }
}