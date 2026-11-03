package com.Author.Fitness.User;

import com.Author.Fitness.Support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class UserRegistrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Registration creates a user with the USER role.")
    void registrationCreatesUserWithRole() throws Exception {
        register("Alex");

        String role = jdbc.queryForObject(
                "SELECT role FROM users WHERE username = ?", String.class,"Alex"
        );

        Assertions.assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("A client cannot assign themselves the ADMIN role during registration.")
    void clientCannotChooseRoleOnRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mallory","email":"mallory@example.com",
                                 "password":"password123","role":"ADMIN"}
                                """))
                .andExpect(status().isOk());

        String role = jdbc.queryForObject(
                "SELECT role FROM users WHERE username = ?", String.class, "mallory");

        Assertions.assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("Registration returns access and refresh tokens.")
    void registrationReturnTokens() throws Exception {

        Tokens tokens = register("Alex");

        Assertions.assertThat(tokens.access())
                .isNotBlank()
                .matches("[\\w-]+\\.[\\w-]+\\.[\\w-]+");

        Assertions.assertThat(tokens.refresh())
                .isNotBlank()
                .doesNotContain(".")
                .isNotEqualTo(tokens.access());

        //выданный access-токен реально открывает защищённый адрес
        mockMvc.perform(get("/api/exercise")
                .header("Authorization","Bearer " + tokens.access()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A registered user can log in.")
    void registeredUserCanLogin() throws Exception{

        register("Alex");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"Alex","password":"password123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("The password is stored as a hash, not in plain text.")
    void passwordIsStoredHashed() throws Exception{

        register("Alex");

        String password = jdbc.queryForObject("SELECT password FROM users WHERE username = ?", String.class,"Alex");


        Assertions.assertThat(password).isNotEqualTo("password123");

        Assertions.assertThat(passwordEncoder.matches("password123", password))
                .isTrue();
    }

    @Test
    @DisplayName("A duplicate username is rejected with 409.")
    void duplicateUsernameIsRejected() throws Exception{

        register("Alex");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"username":"Alex","email":"Alex2@example.com","password":"Alex2password123"}
                         """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));

        //проверка на то, что пользователь не создан после ошибки
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                "Alex2@example.com");

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("A duplicate email is rejected with 409.")
    void duplicateEmailIsRejected() throws Exception{
        register("Alex");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alex2","email":"Alex@example.com","password":"Alex2password123"}
                         """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));

        //проверка на то, что пользователь не создан после ошибки
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "Alex2");

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("A password shorter than eight characters is rejected with 400.")
    void shortPasswordIsRejected() throws Exception{

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"username":"Alex","email":"Alex@example.com","password":"passwo"}
                         """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password must be at least 8 characters"));

        //проверка на то, что пользователь не создан после ошибки
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "Alex");

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("An invalid email is rejected with 400.")
    void invalidEmailIsRejected() throws Exception{
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alex","email":"Alex-example.com","password":"password123"}
                         """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email should be valid"));

        //проверка на то, что пользователь не создан после ошибки
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "Alex");

        Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("A blank username is rejected with 400.")
    void blankUsernameIsRejected() throws Exception{
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"Alex@example.com","password":"password123"}
                         """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("username: Username is required"));

        //проверка на то, что пользователь не создан после ошибки
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users",
                Integer.class);

        Assertions.assertThat(count).isZero();
    }


}
