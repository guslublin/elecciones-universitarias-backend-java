package com.elecciones.election.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "candidates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_candidate_list_position", columnNames = {"list_id", "position_id"})
        }
)
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private ElectionList electionList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(length = 180)
    private String career;

    @Column(columnDefinition = "TEXT")
    private String proposal;
}