package pl.goeuropa.tc_helper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.goeuropa.tc_helper.model.Assignment;
import pl.goeuropa.tc_helper.model.dto.AssignmentDto;
import pl.goeuropa.tc_helper.service.VehicleUpdatesService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleUpdatesController.class)
class VehicleUpdatesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    VehicleUpdatesService service;

    @Test
    void getPositions_returnsServiceResponse() throws Exception {
        when(service.getVehiclePositions("agency1")).thenReturn("feed data");

        mockMvc.perform(get("/api/v1/vehicles/positions").param("agency", "agency1"))
                .andExpect(status().isOk())
                .andExpect(content().string("feed data"));
    }

    @Test
    void getPositions_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/positions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssignments_returnsListFromService() throws Exception {
        Assignment arg = new Assignment();
        arg.setVehicleId("V1");
        arg.setValidFrom("2024-01-01T00:00:00");
        when(service.getAssignmentsByAgency("agency1")).thenReturn(List.of(arg));

        mockMvc.perform(get("/api/v1/vehicles/assignments").param("agency", "agency1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V1"));
    }

    @Test
    void getAssignments_emptyList() throws Exception {
        when(service.getAssignmentsByAgency("unknown")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/vehicles/assignments").param("agency", "unknown"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void putAllAssignments_blockAssignments_success() throws Exception {
        Assignment arg = new Assignment();
        arg.setVehicleId("V1");
        arg.setValidFrom("2024-01-01T00:00:00");
        AssignmentDto dto = new AssignmentDto("key123", List.of(arg));

        when(service.addAllAssignments(any(AssignmentDto.class))).thenReturn("ok");

        mockMvc.perform(post("/api/v1/vehicles")
                        .param("to", "blockAssignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void putAllAssignments_unknownToParam_throws() throws Exception {
        Assignment arg = new Assignment();
        arg.setVehicleId("V1");
        arg.setValidFrom("2024-01-01T00:00:00");
        AssignmentDto dto = new AssignmentDto("key123", List.of(arg));

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/vehicles")
                        .param("to", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
        );
    }

    @Test
    void putAllAssignments_serviceThrows_returnsFailure() throws Exception {
        Assignment arg = new Assignment();
        arg.setVehicleId("V1");
        arg.setValidFrom("2024-01-01T00:00:00");
        AssignmentDto dto = new AssignmentDto("key123", List.of(arg));

        when(service.addAllAssignments(any(AssignmentDto.class))).thenThrow(new RuntimeException("service error"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .param("to", "blockAssignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void manualRetry_validBody_callsService() throws Exception {
        when(service.sendAssignmentsToAgency("agency1")).thenReturn("sent");

        mockMvc.perform(post("/api/v1/vehicles/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agency\": \"agency1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("sent"));
    }

    @Test
    void manualRetry_invalidBody_throws() {
        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/vehicles/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(" "))
        );
    }
}
