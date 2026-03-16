package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        return null;
    }

    @Override
    public PageResponse<PermissionResponse> getPermissions(
            String resource,
            ActionType action,
            String search,
            Pageable pageable
    ) {

        Specification<Permission> spec = buildFilter(resource, action, search);

        Page<PermissionResponse> page = permissionRepository.findAll(spec, pageable).map(permissionMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    public PermissionResponse getPermissionById(String id) {
        return null;
    }

    @Override
    public PermissionResponse updatePermission(String id, UpdatePermissionRequest request) {
        return null;
    }

    @Override
    public void deletePermission(String id) {

    }

    /**
     * Builds a dynamic {@link Specification} used to filter {@link Permission} entities.
     * @param resource the resource identifier to filter by (exact match), e.g. INVENTORY, USER
     * @param action the action type to filter by (exact match), represented by {@link ActionType}
     * @param search the search keyword used for fuzzy matching against permission
     *               name and description
     * @return a {@link Specification} used by Spring Data JPA to build the query dynamically
     */
    private Specification<Permission> buildFilter(
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
