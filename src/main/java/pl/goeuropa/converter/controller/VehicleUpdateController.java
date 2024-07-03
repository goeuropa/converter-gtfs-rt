package pl.goeuropa.converter.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.goeuropa.converter.service.VehicleUpdateService;

import java.util.stream.Collectors;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kombus")
public class VehicleUpdateController {

    private final VehicleUpdateService service;

    @GetMapping("/vehicles.text")
    public String get() {
        var asText = service.getVehiclePositions();
        log.info("Get feed message include {} lines", asText.lines()
                .count());
        return asText;
    }
}
