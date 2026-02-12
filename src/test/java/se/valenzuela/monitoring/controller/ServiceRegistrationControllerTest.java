package se.valenzuela.monitoring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import se.valenzuela.monitoring.service.MonitoringService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceRegistrationController.class)
class ServiceRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @Test
    void register_validUrl_returns200() throws Exception {
        when(monitoringService.addService("http://localhost:8080")).thenReturn(true);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "http://localhost:8080"}
                                """))
                .andExpect(status().isOk());

        verify(monitoringService).addService("http://localhost:8080");
    }

    @Test
    void register_blankUrl_returns400() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": ""}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(monitoringService);
    }

    @Test
    void register_invalidUrl_returns400() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "not-a-url"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(monitoringService);
    }

    @Test
    void register_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(monitoringService);
    }
}
