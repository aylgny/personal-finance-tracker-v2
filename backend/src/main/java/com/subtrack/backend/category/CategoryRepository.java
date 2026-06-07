package com.subtrack.backend.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Returns the global category list for dropdowns.
    List<Category> findAllByOrderBySortOrderAscNameAsc();
}