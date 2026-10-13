package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One audited row in the user_permissions junction. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PermissionGrant {
    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "granted_by")
    private Integer grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    public PermissionGrant(Permission permission, Integer grantedBy, Instant grantedAt) {
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }

    public String code() {
        return permission == null ? null : permission.getCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PermissionGrant grant && Objects.equals(code(), grant.code());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code());
    }
}
