package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.demo.whs.entity.Role;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RoleSpecification {

    public static Specification<Role> filter(
            Boolean isDefault,
            String search
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (isDefault != null) {
                predicates.add(
                        cb.equal(root.get("isDefault"), isDefault)
                );
            }

            if (StringUtils.hasText(search)) {
                String keyword = "%" + search.toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), keyword),
                                cb.like(cb.lower(root.get("description")), keyword)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}