package pl.goeuropa.tc_helper.service;

import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.goeuropa.tc_helper.client.TransitclockClient;
import pl.goeuropa.tc_helper.configs.ApiProperties;
import pl.goeuropa.tc_helper.gtfsrt.GtfsRealTimeVehicleFeed;
import pl.goeuropa.tc_helper.model.Assignment;
import pl.goeuropa.tc_helper.model.dto.AssignmentDto;
import pl.goeuropa.tc_helper.repository.VehicleRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleUpdatesServiceImpl implements VehicleUpdatesService {

    private static String KEY;
    private final VehicleRepository vehicleRepository = VehicleRepository.getInstance();
    private final TransitclockClient tcClient;
    private final ApiProperties properties;

    @Override
    public String getVehiclePositions(String department) {
        try {
            GtfsRealtime.FeedMessage feed = new GtfsRealTimeVehicleFeed()
                    .create(vehicleRepository.getVehiclesList()
                                    .get(department)
                                    .getList(),
                            properties.getTimeZone());
            log.info("Get : {} entities.", feed.getEntityList().size());

            return feed.toString();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return ex.getMessage();
        }
    }

    @Override
    public List<Assignment> getAssignmentsByAgency(String agency) {
        var list = vehicleRepository.getSegregatedAssignments().get(agency);
        if (list == null) return Collections.emptyList();
        log.info("{} assignments for agency '{}' to show", list.size(), agency.toUpperCase());
        return list;
    }

    @Override
    public String sendAssignmentsToAgency(String agency) {
        var list = vehicleRepository.getSegregatedAssignments().get(agency);
        var key = "";
            String regex = "key\\s*/([^/]+)";
            var url = properties.getTcBaseUrls().get(agency);
            Matcher matcher = Pattern.compile(regex).matcher(url);
            if (matcher.find()) {
                key = matcher.group(1);
        }
        log.info("{} assignments send to agency '{}'", list.size(), agency.toUpperCase());
        return tcClient.sendAssignments(agency, new AssignmentDto(key, list));
    }

    @Override
    public String addAllAssignments(AssignmentDto assignments) {
        KEY = assignments.getKey();
        int amount = 0;
        try {
            vehicleRepository.getAssignments().put(KEY, assignments);

            List<Assignment> assignmentsList = vehicleRepository.getAssignments().get(KEY).getAssignmentsList();
            amount = assignmentsList.size();

            log.info("Added {} assignments to repository.", amount);

            new Thread(new Runnable() {
                @Override
                public void run() {

                    for (String agency : properties.getTcBaseUrls().keySet()) {

                        var list = assignmentsList.stream()
                                .filter(assignment -> {
                                    return vehicleRepository.getVehiclesList()
                                            .get(agency)
                                            .getList()
                                            .stream()
                                            .anyMatch(vehicle -> vehicle.get("number")
                                                    .equals(assignment.getVehicleId()
                                                    ));
                                }).toList();
                        if (list.isEmpty()) continue;
                        vehicleRepository.getSegregatedAssignments().put(agency, list);

                        log.info("Segregated {} assignments for agency: {}",
                                list.size(),
                                agency
                        );
                        tcClient.sendAssignments(agency, new AssignmentDto(KEY, list));
                    }
                }
            }).start();
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        Map<String, Boolean> result = new HashMap<>();
        vehicleRepository.getAssignments().get(KEY).getAssignmentsList()
                .forEach(assignment -> result.put(assignment.getVehicleId(), true));
        return result.toString();
    }
}
