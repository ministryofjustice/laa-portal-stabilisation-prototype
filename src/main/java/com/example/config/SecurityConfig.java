package com.example.config;

import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Javadoc comment.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
    {
      this.clientRegistrationRepository = clientRegistrationRepository;
    }
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests((authorize) -> authorize
            .requestMatchers("/", "/migrate", "/css/**", "/js/**", "/assets/**")
            .permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .permitAll()
            .defaultSuccessUrl("/home", true)
        )
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
            .logoutSuccessUrl("/")
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID") // clear the session cookie
            .invalidateHttpSession(true)
            .permitAll());


    return http.build();
  }

  /**
   * Javadoc comment.
   */
  @Bean
  public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
    UserDetails user = User.withUsername("testUser")
        .password(passwordEncoder.encode("password123"))
        .roles("USER")
        .build();

    return new InMemoryUserDetailsManager(user);
  }
}
