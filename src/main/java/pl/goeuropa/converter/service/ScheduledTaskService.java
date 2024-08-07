package pl.goeuropa.converter.service;

import com.google.transit.realtime.GtfsRealtime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.goeuropa.converter.gtfsrt.GtfsRealTimeVehicleFeed;
import pl.goeuropa.converter.repository.VehicleRepository;

import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScheduledTaskService {

    private final VehicleRepository vehicleRepository = VehicleRepository.getInstance();

    @Value("${api.out-path}")
    private String outputPath;

    @Value("${api.time-zone}")
    private String timeZone;


    @Scheduled(fixedRateString = "${api.refresh-interval}",
            timeUnit = TimeUnit.SECONDS)
    public void updateVehiclesPositionsProtoBufFile() {
        try (FileOutputStream toFile = new FileOutputStream(outputPath)) {

            GtfsRealtime.FeedMessage feed = new GtfsRealTimeVehicleFeed()
                    .create(vehicleRepository.getVehiclesList(), timeZone);
            log.info("Write to file: {} entities.", feed.getEntityList().size());

            //Writing to protobuf file
            feed.writeTo(toFile);

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}
