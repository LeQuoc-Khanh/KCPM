package app.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock private UserRepository userRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnPrincipal_whenUserIsActive() {
        User user = activeUser(UserRole.CANDIDATE);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(user.getEmail());

        assertEquals(user.getEmail(), result.getUsername());
        assertEquals("CANDIDATE", result.getAuthorities().iterator().next().getAuthority());
        verify(userRepository, never()).save(user);
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@test.com"));
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserIsBanned() {
        User user = activeUser(UserRole.CANDIDATE);
        user.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class,
                () -> userDetailsService.loadUserByUsername(user.getEmail()));
    }

    @Test
    void loadUserByUsername_shouldDowngradeAndSave_whenVipHasExpired() {
        User user = activeUser(UserRole.CANDIDATE_VIP);
        user.setVipExpirationDate(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(user.getEmail());

        assertEquals(UserRole.CANDIDATE, user.getUserRole());
        assertNull(user.getVipExpirationDate());
        assertEquals("CANDIDATE", result.getAuthorities().iterator().next().getAuthority());
        verify(userRepository).save(user);
    }

    @Test
    void loadUserByUsername_shouldKeepVipRole_whenVipHasNotExpired() {
        User user = activeUser(UserRole.RECRUITER_VIP);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        user.setVipExpirationDate(expiry);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(user.getEmail());

        assertSame(expiry, user.getVipExpirationDate());
        assertEquals("RECRUITER_VIP", result.getAuthorities().iterator().next().getAuthority());
        verify(userRepository, never()).save(user);
    }

    private User activeUser(UserRole role) {
        return User.builder()
                .id(1L).email("candidate@test.com").password("encoded-password")
                .userRole(role).status(UserStatus.ACTIVE).build();
    }
}
