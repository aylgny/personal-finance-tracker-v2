package com.subtrack.backend.category;

import com.subtrack.backend.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        // Categories are global reference data used by all users.
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        // Return only fields needed by the frontend dropdown.
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder()
        );
    }
}