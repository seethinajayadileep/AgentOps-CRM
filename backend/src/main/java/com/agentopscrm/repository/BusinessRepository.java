package com.agentopscrm.repository;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.CrawlStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Business entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByWebsiteUrl(String websiteUrl);

    List<Business> findByCrawlStatus(CrawlStatus crawlStatus);

    Page<Business> findByCrawlStatus(CrawlStatus crawlStatus, Pageable pageable);

    @Query("SELECT b FROM Business b WHERE b.name LIKE %:search% OR b.industry LIKE %:search%")
    List<Business> search(@Param("search") String search);

    @Query("SELECT b FROM Business b WHERE b.name LIKE %:search% OR b.industry LIKE %:search%")
    Page<Business> search(@Param("search") String search, Pageable pageable);

    boolean existsByWebsiteUrl(String websiteUrl);
}