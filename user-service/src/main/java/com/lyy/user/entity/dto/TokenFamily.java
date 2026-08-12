package com.lyy.user.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenFamily {

    private Long userId;
    private String deviceFingerprint;
    private String currentTokenHash;
    @Builder.Default
    private List<String> usedTokens = new ArrayList<>();
    private long createdAt;
    @Builder.Default
    private String status = "ACTIVE";

    public static final String ACTIVE = "ACTIVE";
    public static final String REVOKED = "REVOKED";
}
