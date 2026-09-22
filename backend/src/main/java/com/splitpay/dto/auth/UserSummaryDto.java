package com.splitpay.dto.auth;

import com.splitpay.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private String businessName;
    private String defaultUpiId;
}
