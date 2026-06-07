package com.subtrack.backend.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Returns categories owned by a specific user, ordered for UI display.
    List<Category> findByUserIdOrderBySortOrderAscNameAsc(Long userId);

    // Finds a category only if it belongs to the authenticated user.
    // This prevents users from using another user's categoryId.
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}