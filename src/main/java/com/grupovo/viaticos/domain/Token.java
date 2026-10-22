package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.TokenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "token")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idToken;
    @Column(unique = true, columnDefinition = "TEXT")
    private String token;
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    @Column(unique = true, length = 64)
    private String refreshTokenHash;
    private Boolean isRevoked;
    private Boolean isExpired;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TokenType type = TokenType.BEARER;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona")
    private Persona persona;

}
