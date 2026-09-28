package ru.quard0r.gitly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "role")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class Role implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String roleName;

    @Override
    public @Nullable String getAuthority() {
        return roleName;
    }
}
