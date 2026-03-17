package org.demo.whs.repository.specification;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.enums.ActionType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PermissionSpecification {

    public static Specification<Permission> filter(
            String resource,
            ActionType action,
            String search
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (resource != null && !resource.isBlank()) {
                predicates.add(cb.equal(root.get("resource"), resource));
            }

            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("description")), pattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}