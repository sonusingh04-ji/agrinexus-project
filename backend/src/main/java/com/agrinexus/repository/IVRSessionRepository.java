package com.agrinexus.repository;

import com.agrinexus.entity.IVRSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IVRSessionRepository
        extends JpaRepository<IVRSession, UUID> {

    List<IVRSession> findByFarmIdOrderByStartedAtDesc(
            UUID farmId
    );

    List<IVRSession> findByCallerReferenceOrderByStartedAtDesc(
            String callerReference
    );

    List<IVRSession> findByStatusOrderByLastInteractionAtDesc(
            String status
    );
}