package com.agrinexus.repository;

import com.agrinexus.entity.VillageCooperativeMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VillageCooperativeMemberRepository
        extends JpaRepository<VillageCooperativeMember, UUID> {

    List<VillageCooperativeMember>
    findByCooperativeId(UUID cooperativeId);

    List<VillageCooperativeMember>
    findByCooperativeIdAndStatusIgnoreCase(
            UUID cooperativeId,
            String status
    );

    List<VillageCooperativeMember>
    findByFarmerId(UUID farmerId);

    boolean existsByCooperativeIdAndFarmerId(
            UUID cooperativeId,
            UUID farmerId
    );
}