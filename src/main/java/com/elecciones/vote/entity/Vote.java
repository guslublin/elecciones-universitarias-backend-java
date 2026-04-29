package com.elecciones.vote.entity;

import com.elecciones.election.entity.Election;
import com.elecciones.election.entity.ElectionList;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vote_voter_election", columnNames = {"voter_hash", "election_id"})
        }
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private ElectionList electionList;

    @Column(name = "voter_hash", nullable = false)
    private String voterHash;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    void prePersist() {
        if (votedAt == null) {
            votedAt = LocalDateTime.now();
        }
    }
}