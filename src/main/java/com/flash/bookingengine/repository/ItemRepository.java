package com.flash.bookingengine.repository;

import com.flash.bookingengine.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE Item i SET i.availableStock = i.availableStock - 1 WHERE i.id = :id AND i.availableStock > 0")
    int decrementAvailableStock(@Param("id") Long id);
}
