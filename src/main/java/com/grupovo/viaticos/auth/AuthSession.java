package com.grupovo.viaticos.auth;

import com.grupovo.viaticos.domain.Token;

public record AuthSession(Token token, String refreshToken) {
}
