package com.synergyhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateOrganizationRequest;
import com.synergyhub.dto.response.OrganizationResponse;
import com.synergyhub.security.JwtAuthenticationFilter;
import com.synergyhub.security.UserPrincipal;
import com.synergyhub.service.organization.OrganizationService;
import com.synergyhub.util.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity in this unit test
public class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private ClientIpResolver ipResolver;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;
    private UserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@example.com");
        
        mockPrincipal = UserPrincipal.create(mockUser);
    }

    @Test
    void createOrganization_Success() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("Test Org");
        request.setAddress("123 Test St");

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .name("Test Org")
                .build();

        when(ipResolver.resolveClientIp(any())).thenReturn("127.0.0.1");
        when(organizationService.createOrganization(any(CreateOrganizationRequest.class), any(User.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(SecurityMockMvcRequestPostProcessors.user(mockPrincipal))) // Inject mock principal
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Org"));
    }
}
