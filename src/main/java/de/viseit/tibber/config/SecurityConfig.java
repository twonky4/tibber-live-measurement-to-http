package de.viseit.tibber.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
				.httpBasic(withDefaults());
		return http.build();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService(@Value("${app.login.user-name}") String user,
			@Value("${app.login.password}") String password) {
		return new InMemoryUserDetailsManager(User
				.withUsername(user)
				.password("{noop}" + password)
				.roles("USER_ROLE")
				.build());
	}
}
