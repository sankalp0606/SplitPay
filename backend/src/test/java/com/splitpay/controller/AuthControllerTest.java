package com.splitpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitpay.dto.auth.LoginRequest;
import com.splitpay.dto.auth.RegisterRequest;
import com.splitpay.entity.Role;
import com.splitpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register should create user and return JWT token")
    void registerUser_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("merchant@splitpay.in")
                .password("StrongPassword123!")
                .fullName("Rajesh Sharma")
                .role(Role.MERCHANT)
                .businessName("Sharma Retail")
                .defaultUpiId("sharma@upi")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("merchant@splitpay.in")))
                .andExpect(jsonPath("$.user.fullName", is("Rajesh Sharma")))
                .andExpect(jsonPath("$.user.role", is("MERCHANT")))
                .andExpect(jsonPath("$.user.businessName", is("Sharma Retail")));
    }

    @Test
    @DisplayName("POST /api/auth/register should fail on duplicate email with 409")
    void registerUser_DuplicateEmail_Fails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@splitpay.in")
                .password("Password123!")
                .fullName("First User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("POST /api/auth/login should authenticate and return token")
    void loginUser_Success() throws Exception {
        // First register
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("login@splitpay.in")
                .password("MySecretPass123")
                .fullName("Anita Verma")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginReq = LoginRequest.builder()
                .email("login@splitpay.in")
                .password("MySecretPass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("login@splitpay.in")));
    }

    @Test
    @DisplayName("POST /api/auth/login should fail on incorrect password with 401")
    void loginUser_BadPassword_Fails() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("wrongpass@splitpay.in")
                .password("CorrectPassword123")
                .fullName("Anita Verma")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = LoginRequest.builder()
                .email("wrongpass@splitpay.in")
                .password("WrongPassword999")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/auth/me should reject unauthenticated request with 401")
    void getMe_Unauthenticated_Fails() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/auth/me should succeed with valid JWT token")
    void getMe_Authenticated_Success() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("authme@splitpay.in")
                .password("AuthMeSecret123")
                .fullName("Vikram Seth")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("authme@splitpay.in")))
                .andExpect(jsonPath("$.fullName", is("Vikram Seth")));
    }
}
