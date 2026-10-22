package com.campusconnect.security;

import com.campusconnect.entity.User;
import com.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CampusUserDetailsService userDetailsService;

    private User student;
    private User organizer;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        String hash = passwordEncoder.encode("password");

        student = new User("Anand Purohit", "anand.purohit@student.edu", User.Role.STUDENT);
        student.setPasswordHash(hash);

        organizer = new User("Dr. Priya Sharma", "priya.sharma@campus.edu", User.Role.ORGANIZER);
        organizer.setPasswordHash(hash);
    }

    @Test
    void loadUserByUsername_whenStudent_returnsUserDetailsWithRoleStudent() {
        when(userRepository.findByEmail("anand.purohit@student.edu")).thenReturn(Optional.of(student));

        UserDetails userDetails = userDetailsService.loadUserByUsername("anand.purohit@student.edu");

        assertThat(userDetails.getUsername()).isEqualTo("anand.purohit@student.edu");
        assertThat(passwordEncoder.matches("password", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_whenOrganizer_returnsUserDetailsWithRoleOrganizer() {
        when(userRepository.findByEmail("priya.sharma@campus.edu")).thenReturn(Optional.of(organizer));

        UserDetails userDetails = userDetailsService.loadUserByUsername("priya.sharma@campus.edu");

        assertThat(userDetails.getUsername()).isEqualTo("priya.sharma@campus.edu");
        assertThat(passwordEncoder.matches("password", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ORGANIZER");
    }

    @Test
    void loadUserByUsername_whenNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@campus.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@campus.edu"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("No user found with email");
    }
}
