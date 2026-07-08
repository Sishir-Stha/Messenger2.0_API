package com.messenger.api.messaging.repository;

import com.messenger.api.messaging.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("""
        select u from AppUser u
        where lower(u.name) like lower(concat('%', :query, '%'))
           or lower(u.username) like lower(concat('%', :query, '%'))
           or lower(u.email) like lower(concat('%', :query, '%'))
        order by u.name asc
        """)
    List<AppUser> search(@Param("query") String query);
}
