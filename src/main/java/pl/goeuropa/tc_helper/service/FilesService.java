package pl.goeuropa.tc_helper.service;

import com.google.transit.realtime.GtfsRealtime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.goeuropa.tc_helper.configs.ApiProperties;
import pl.goeuropa.tc_helper.gtfsrt.GtfsRealTimeVehicleFeed;
import pl.goeuropa.tc_helper.model.Assignment;
import pl.goeuropa.tc_helper.repository.VehicleRepository;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilesService {

    private final static String FILE_PATH = "./.back.up";

    private final VehicleRepository vehicleRepository = VehicleRepository.getInstance();
    private final ApiProperties properties;

    @Scheduled(fixedRateString = "${api.refresh-interval:15}",
            timeUnit = TimeUnit.SECONDS)
    public void updateVehiclesPositionsProtoBufFile() {

        for (String key : properties.getTokens().keySet()) {
            try (FileOutputStream toFile = new FileOutputStream(
                    properties.getOutPath() + key + properties.getPostfix())) {

                GtfsRealtime.FeedMessage feed = new GtfsRealTimeVehicleFeed()
                        .create(vehicleRepository.getVehiclesList().get(key).getList(),
                                properties.getTimeZone());
                //Writing to protobuf file
                feed.writeTo(toFile);
                log.debug("Write to file: {} entities.", feed.getEntityList().size());

            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @PostConstruct
    public void getFromBackupFile() {
        try (FileInputStream fileInput = new FileInputStream(FILE_PATH);
             ObjectInputStream objectInput = new ObjectInputStream(fileInput)) {

            Object raw = objectInput.readObject();
            if (raw instanceof ConcurrentHashMap<?, ?> map) {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<String, List<Assignment>> restored =
                        (ConcurrentHashMap<String, List<Assignment>>) map;
                vehicleRepository.setSegregatedAssignments(restored);
                log.info("{} - assignments from file is added to repository.",
                        restored.values().stream().mapToInt(List::size).sum());
            } else {
                log.warn("Backup file {} did not contain a ConcurrentHashMap (was {}), ignoring.",
                        FILE_PATH, raw == null ? "null" : raw.getClass().getName());
            }
        } catch (FileNotFoundException e) {
            log.info("Backup file {} not found, starting fresh.", FILE_PATH);
        } catch (Exception e) {
            log.error("Failed to load backup file {}: {}", FILE_PATH, e.getMessage());
        }
    }

    @PreDestroy
    public void saveToBackFile() {
        var segregatedAssignments = vehicleRepository.getSegregatedAssignments();
        try {
            var toFile = new FileOutputStream(FILE_PATH);
            var objectToFile = new ObjectOutputStream(toFile);
            objectToFile.writeObject(segregatedAssignments);

            log.info(" Successfully save {} assignments to back-up file : {}",
                    segregatedAssignments.values().stream()
                            .mapToInt(List::size)
                            .sum(),
                    FILE_PATH);

            objectToFile.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
