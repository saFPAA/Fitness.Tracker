package com.Author.Fitness.User;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserAccessTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A user can read their own profile.")
    void userCanReadOwnProfile() throws Exception{


        Tokens token = register("Alex");


        mockMvc.perform(get("/api/user/"+userId("Alex"))
                .header("Authorization",bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alex"))
                .andExpect(jsonPath("$.email").value("Alex@example.com"));
    }

    @Test
    @DisplayName("A user cannot read someone else’s profile and receives 403.")
    void userCannotReadOtherUsersProfile() throws Exception{

        Tokens firstPerson = register("Alex");

        Tokens secondPerson = register("Bob");

        mockMvc.perform(get("/api/user/"+userId("Bob"))
                .header("Authorization",bearer(firstPerson)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user can delete their own account.")
    void userCanDeleteOwnAccount() throws Exception{
        Tokens token = register("Alex6");

        mockMvc.perform(delete("/api/user/"+userId("Alex6"))
                .header("Authorization",bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A user cannot delete someone else’s account and receives 403.")
    void userCannotDeleteOtherUsersAccount() throws Exception{
        Tokens firstPerson = register("Alex7");

        Tokens secondPerson = register("Bob7");

        mockMvc.perform(delete("/api/user/"+userId("Bob7"))
                        .header("Authorization",bearer(firstPerson)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An administrator can read any profile.")
    void AdminCanReadAnyProfile() throws Exception{
        Tokens firstPerson = registerAdmin("Alex8");



        Tokens secondPerson = register("Bob8");

        mockMvc.perform(get("/api/user/"+userId("Bob8"))
                .header("Authorization",bearer(firstPerson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Bob8"))
                .andExpect(jsonPath("$.email").value("Bob8@example.com"));
    }




}
