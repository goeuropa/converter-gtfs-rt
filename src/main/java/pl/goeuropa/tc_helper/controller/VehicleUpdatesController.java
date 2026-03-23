package pl.goeuropa.tc_helper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.goeuropa.tc_helper.model.Assignment;
import pl.goeuropa.tc_helper.model.dto.ApiResponseDto;
import pl.goeuropa.tc_helper.model.dto.AssignmentDto;
import pl.goeuropa.tc_helper.service.VehicleUpdatesService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VehicleUpdatesController {

    private final VehicleUpdatesService service;

    @GetMapping("/vehicles/positions")
    public String getPositionsByAgency(@RequestParam("agency") String agency) {
        String asText = service.getVehiclePositions(agency);
        log.info("Get feed message include {} lines", asText.lines()
                .count());
        return asText;
    }

    @GetMapping("/vehicles/assignments")
    public List<Assignment> getAssignmentsByAgency(@RequestParam("agency") String agency) {
        log.info("Get assignments for agency {}", agency);
        return service.getAssignmentsByAgency(agency);
    }

    @PostMapping("/vehicles")
    public ApiResponseDto putAllAssignments(
            @RequestBody AssignmentDto assignments,
            @RequestParam("to") String to) {
        try {
            if (to.equals("blockAssignments")) {
                var result = service.addAllAssignments(assignments);
                log.info("Receive {} assignments with key {}", assignments.getAssignmentsList().size(), assignments.getKey());
                return new ApiResponseDto(true, result);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ApiResponseDto(false, e.getMessage());
        }
        throw new IllegalArgumentException("Check URI or JSON-body that you sent");
    }

    @PostMapping("/key/{key}/agency/{agency}/command/vehiclesToBlockAssignments")
    public ApiResponseDto putAllAssignmentsForVeritum(
            @RequestBody AssignmentDto assignments,
            @PathVariable String key, @PathVariable String agency) {
        try {
            var result = service.addAllAssignments(assignments);
            log.info("Receive {} assignments with key {}", assignments.getAssignmentsList().size(), assignments.getKey());
            return new ApiResponseDto(true, result);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ApiResponseDto(false, e.getMessage());
        }
    }

    @PostMapping("/vehicles/assignments")
    public String manualRetryToSendAssignments(@RequestBody String agency) {
        try {
            String regex = ":\\s*\"([^\"]+)";
            Matcher matcher = Pattern.compile(regex).matcher(agency);
            if (matcher.find()) {
                log.info("Resend assignments to agency: {}", matcher.group(1).toUpperCase());
                return service.sendAssignmentsToAgency(matcher.group(1));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
        throw new IllegalArgumentException("Check JSON-body that you set");
    }
}
