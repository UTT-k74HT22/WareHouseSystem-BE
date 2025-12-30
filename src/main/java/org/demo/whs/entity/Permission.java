package org.demo.whs.entity;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission extends BaseEntity {
    private String code;
    private String name;
    private String description;
}
