package vn.gov.bacninh.ninhxareport.repository;

import vn.gov.bacninh.ninhxareport.entity.ZaloAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZaloAccessTokenRepository extends JpaRepository<ZaloAccessToken, Long> {
    Optional<ZaloAccessToken> findFirstByOrderByIdAsc();
}

