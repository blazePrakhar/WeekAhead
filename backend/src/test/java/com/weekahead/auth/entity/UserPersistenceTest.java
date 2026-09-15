package com.weekahead.auth.entity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.weekahead.auth.repository.UserRepository;

@SpringBootTest
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndReadUser() {
        User user = new User(
                "persistence-test@example.com",
                "test-password-hash",
                Role.USER,
                UserStatus.ACTIVE
        );

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();

        Optional<User> foundUser =
                userRepository.findByEmail("persistence-test@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail())
                .isEqualTo("persistence-test@example.com");
        assertThat(foundUser.get().getRole())
                .isEqualTo(Role.USER);
        assertThat(foundUser.get().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
        assertThat(foundUser.get().getUpdatedAt()).isNotNull();
        
        userRepository.delete(savedUser);
    }
}