package com.backend.givu.model.repository;

import com.backend.givu.model.Enum.PaymentsStatus;
import com.backend.givu.model.entity.Payment;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Integer paymentId);


    List<Payment> findAllByStatusAndDateBefore(PaymentsStatus status, Instant date);

    @Query("""
    SELECT p
    FROM Payment p
    JOIN FETCH p.relatedFunding f
    JOIN FETCH f.product  pr
    WHERE p.id = :paymentId
    """)
    Optional<Payment> findByIdWithFAndRelatedFunding(@Param("paymentId") Integer paymentId);


    boolean existsByUserIdAndRelatedProductId(Long userId, Integer productId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.relatedProduct WHERE p.user.id = :userId")
    List<Payment> findByUserIdWithProduct(@Param("userId") Long userId);

    @Query("""
    SELECT p
    FROM Payment p
    LEFT JOIN FETCH p.relatedFunding
    LEFT JOIN FETCH p.relatedProduct
    WHERE p.user.id = :userId
    """)
    List<Payment> findByUserIdWithFundingAndProduct(@Param("userId") Long userId);


}
