package com.elecciones.user.repository;

import com.elecciones.common.enums.RoleName;
import com.elecciones.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select count(distinct u.id)
            from User u
            join u.roles r
            where r.roleName = :roleName
            """)
    long countByRole(RoleName roleName);
}