package io.github.ringotangs.springcommons.sample;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建用户请求。 / Request for creating a user. */
public record CreateUserRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(1) @Max(150) Integer age) {}
