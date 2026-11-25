package com.passwordmanager.backend;

import com.passwordmanager.backend.entity.*;
import com.passwordmanager.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InfraVaultRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DatacenterRepository datacenterRepository;
    @Autowired private DatacenterUserRepository datacenterUserRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private CredentialRepository credentialRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final List<Long> createdDeviceIds = new ArrayList<>();
    private final List<Long> createdFolderIds = new ArrayList<>();
    private final List<Long> createdDcIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final java.time.Instant testStart = java.time.Instant.now();

    @AfterEach
    void cleanup() {
        List<AuditLog> newLogs = auditLogRepository.findAll().stream()
                .filter(a -> !a.getTimestamp().isBefore(testStart))
                .toList();
        for (AuditLog log : newLogs) {
            if (log.getMetadata() != null) {
                assertFalse(log.getMetadata().contains("FakeIdracSecret!99"), "Plaintext secret leaked to audit log");
                assertFalse(log.getMetadata().contains("IsolatedSecret!88"), "Plaintext secret leaked to audit log");
                assertFalse(log.getMetadata().contains("TestPass!2026"), "Plaintext user password leaked to audit log");
            }
        }
        auditLogRepository.deleteAll(newLogs);

        for (Long devId : createdDeviceIds) {
            credentialRepository.findByDeviceId(devId).forEach(credentialRepository::delete);
            deviceRepository.findById(devId).ifPresent(deviceRepository::delete);
        }
        for (Long fId : createdFolderIds) {
            folderRepository.findById(fId).ifPresent(folderRepository::delete);
        }
        for (Long dcId : createdDcIds) {
            datacenterUserRepository.deleteAll(datacenterUserRepository.findByDatacenterId(dcId));
            datacenterRepository.findById(dcId).ifPresent(datacenterRepository::delete);
        }
        for (Long uId : createdUserIds) {
            datacenterUserRepository.deleteAll(datacenterUserRepository.findByUserId(uId));
            userRepository.findById(uId).ifPresent(userRepository::delete);
        }
        createdDeviceIds.clear();
        createdFolderIds.clear();
        createdDcIds.clear();
        createdUserIds.clear();
    }

    private User createTestUser(String prefix, String role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User u = new User(
                prefix + "_" + suffix,
                prefix + "_" + suffix + "@test.infravault.local",
                passwordEncoder.encode("TestPass!2026"),
                role
        );
        User saved = userRepository.save(u);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password));
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void publicRegistrationCannotEscalateToSuperAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String uname = "reg_test_" + suffix;
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "username", uname,
                "email", uname + "@test.infravault.local",
                "password", "TestPass!2026",
                "role", "SUPER_ADMIN"
        ));

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertEquals("VIEWER", json.get("role").asText());
        createdUserIds.add(json.get("id").asLong());
    }

    @Test
    void enforcesRbacCrossDatacenterIsolationAndEncryption() throws Exception {
        User superAdmin = createTestUser("sa", "SUPER_ADMIN");
        User dcAdmin = createTestUser("dca", "DC_ADMIN");
        User operator = createTestUser("op", "OPERATOR");
        User viewer = createTestUser("vw", "VIEWER");

        String saToken = loginAndGetToken(superAdmin.getUsername(), "TestPass!2026");
        String dcaToken = loginAndGetToken(dcAdmin.getUsername(), "TestPass!2026");
        String opToken = loginAndGetToken(operator.getUsername(), "TestPass!2026");
        String vwToken = loginAndGetToken(viewer.getUsername(), "TestPass!2026");

        // Unauthenticated request gets 401
        mockMvc.perform(get("/api/datacenters")).andExpect(status().isUnauthorized());

        // VIEWER and OPERATOR cannot create datacenters (403)
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/datacenters")
                        .header("Authorization", "Bearer " + vwToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"vw-dc-" + suffix + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/datacenters")
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"op-dc-" + suffix + "\"}"))
                .andExpect(status().isForbidden());

        // DC_ADMIN creates DC-A and SUPER_ADMIN creates DC-B
        MvcResult dcARes = mockMvc.perform(post("/api/datacenters")
                        .header("Authorization", "Bearer " + dcaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"dc-a-" + suffix + "\",\"description\":\"Primary\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long dcAId = objectMapper.readTree(dcARes.getResponse().getContentAsString()).get("id").asLong();
        createdDcIds.add(dcAId);

        MvcResult dcBRes = mockMvc.perform(post("/api/datacenters")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"dc-b-" + suffix + "\",\"description\":\"Isolated\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long dcBId = objectMapper.readTree(dcBRes.getResponse().getContentAsString()).get("id").asLong();
        createdDcIds.add(dcBId);

        // Assign OPERATOR (WRITE) and VIEWER (READ) to DC-A only
        mockMvc.perform(post("/api/datacenters/" + dcAId + "/users")
                        .header("Authorization", "Bearer " + dcaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + operator.getId() + ",\"permissionLevel\":\"WRITE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/datacenters/" + dcAId + "/users")
                        .header("Authorization", "Bearer " + dcaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + viewer.getId() + ",\"permissionLevel\":\"READ\"}"))
                .andExpect(status().isOk());

        // OPERATOR creates folder & device with credential in DC-A; SUPER_ADMIN creates folder & device in DC-B
        MvcResult fARes = mockMvc.perform(post("/api/datacenters/" + dcAId + "/folders")
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"rack-a\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long fAId = objectMapper.readTree(fARes.getResponse().getContentAsString()).get("id").asLong();
        createdFolderIds.add(fAId);

        MvcResult fBRes = mockMvc.perform(post("/api/datacenters/" + dcBId + "/folders")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"rack-b\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long fBId = objectMapper.readTree(fBRes.getResponse().getContentAsString()).get("id").asLong();
        createdFolderIds.add(fBId);

        String devAPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "hostname", "host-a-" + suffix,
                "model", "PowerEdge R750",
                "idracPassword", "FakeIdracSecret!99"
        ));
        MvcResult devARes = mockMvc.perform(post("/api/folders/" + fAId + "/devices")
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devAPayload))
                .andExpect(status().isOk())
                .andReturn();
        long devAId = objectMapper.readTree(devARes.getResponse().getContentAsString()).get("id").asLong();
        createdDeviceIds.add(devAId);

        String devBPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "hostname", "host-b-" + suffix,
                "model", "PowerEdge R760",
                "idracPassword", "IsolatedSecret!88"
        ));
        MvcResult devBRes = mockMvc.perform(post("/api/folders/" + fBId + "/devices")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(devBPayload))
                .andExpect(status().isOk())
                .andReturn();
        long devBId = objectMapper.readTree(devBRes.getResponse().getContentAsString()).get("id").asLong();
        createdDeviceIds.add(devBId);

        // Verify encryption at rest in PostgreSQL
        List<Credential> credsA = credentialRepository.findByDeviceId(devAId);
        assertEquals(1, credsA.size());
        assertNotEquals("FakeIdracSecret!99", credsA.get(0).getEncryptedSecret());

        List<Credential> credsB = credentialRepository.findByDeviceId(devBId);
        assertEquals(1, credsB.size());
        long credBId = credsB.get(0).getId();

        // Cross-DC IDOR attempt: OPERATOR tries to access/reveal DC-B credential while passing DC-A's datacenterId
        mockMvc.perform(get("/api/devices/" + devBId + "/credentials?datacenterId=" + dcAId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/credentials/" + credBId + "/reveal?datacenterId=" + dcAId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());

        // VIEWER cannot create credential on devA (403)
        mockMvc.perform(post("/api/credentials/create")
                        .header("Authorization", "Bearer " + vwToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":" + devAId + ",\"type\":\"CUSTOM\",\"username\":\"root\",\"password\":\"Nope!1234\"}"))
                .andExpect(status().isForbidden());

        // OPERATOR can reveal DC-A credential
        mockMvc.perform(post("/api/credentials/" + credsA.get(0).getId() + "/reveal?datacenterId=" + dcAId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("FakeIdracSecret!99"));

        // Audit logs & Users restrictions
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + vwToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + vwToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit?datacenterId=" + dcBId).header("Authorization", "Bearer " + dcaToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit?datacenterId=" + dcAId).header("Authorization", "Bearer " + dcaToken))
                .andExpect(status().isOk());
    }
}
