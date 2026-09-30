package com.agrinexus.repository;

import com.agrinexus.entity.AgriculturalReference;
import com.agrinexus.enums.AgriculturalReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgriculturalReferenceRepository
        extends JpaRepository<AgriculturalReference, UUID> {

    List<AgriculturalReference> findByCropId(UUID cropId);

    List<AgriculturalReference> findByCropIdAndReferenceType(
            UUID cropId,
            AgriculturalReferenceType referenceType
    );

    List<AgriculturalReference> findByReferenceType(
            AgriculturalReferenceType referenceType
    );

    long countByCropId(UUID cropId);
}
