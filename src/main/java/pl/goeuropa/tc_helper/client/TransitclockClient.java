package pl.goeuropa.tc_helper.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.goeuropa.tc_helper.configs.ApiProperties;
import pl.goeuropa.tc_helper.model.dto.AssignmentDto;

import java.util.Objects;

@Slf4j
@Component
public class TransitclockClient {

    private final RestClient restClient;
    private final ApiProperties properties;

    private final static String URI = "/command/vehiclesToBlockAssignments";

    public TransitclockClient(@Qualifier("restClientTC") RestClient restClient, ApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Retryable
    public String sendAssignments(String agency, AssignmentDto assignments) {
        var response = restClient.post()
                .uri(properties.getTcBaseUrls().get(agency) + URI)
                .body(assignments)
                .retrieve().body(String.class);

        log.info("Post {} assignments to TC {} - {}; retry attempt(s): {}",
                assignments.getAssignmentsList().size(),
                agency.toUpperCase(),
                response,
                Objects.requireNonNull(RetrySynchronizationManager.getContext()).getRetryCount());
        return response;
    }

    @Recover
    public String recover(Exception ex, String agency, AssignmentDto assignments) {
        log.error("Recover called after all retries for agency '{}': {}", agency, ex.getMessage());
        return "Error Class :: " + ex.getClass().getName();
    }
}
