package com.subtrack.backend.category;

import com.subtrack.backend.shared.BaseEntity;
import com.subtrack.backend.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected Category() {
    }

    public Category(User user, String name, Integer sortOrder) {
        this.user = user;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}