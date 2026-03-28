package org.demo.whs.entity.dto.response.Permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPermissionsResponse {

    @Builder.Default
    private List<String> permissions = List.of();
}
