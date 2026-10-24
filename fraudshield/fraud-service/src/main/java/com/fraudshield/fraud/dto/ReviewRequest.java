package com.fraudshield.fraud.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(@Size(max = 255) String note) {
}
