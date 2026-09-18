package com.manuelperez.pipelinesentinel.api.health;

public record HealthResponse(String service, String status) {
}
