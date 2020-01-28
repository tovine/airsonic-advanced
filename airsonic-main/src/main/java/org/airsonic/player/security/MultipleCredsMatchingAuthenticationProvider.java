package org.airsonic.player.security;

import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SecurityService.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class MultipleCredsMatchingAuthenticationProvider extends DaoAuthenticationProvider {
    public static final String SALT_TOKEN_MECHANISM_SPECIALIZATION = "salttoken";
    private SecurityService securityService;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        String presentedPassword = authentication.getCredentials().toString();

        String encoderSpecialization = (authentication.getCredentials() instanceof SaltToken)
                ? SALT_TOKEN_MECHANISM_SPECIALIZATION
                : "";

        if (!UserDetail.class.isAssignableFrom(userDetails.getClass())) {
            throw new InternalAuthenticationServiceException("Retrieved user does not match expected class");
        }

        UserDetail userDetail = (UserDetail) userDetails;

        Optional<UserCredential> matchedCred = userDetail.getCredentials().parallelStream().filter(
                c -> getPasswordEncoder().matches(presentedPassword,
                        "{" + c.getType() + encoderSpecialization + "}" + c.getCredential()))
                .findAny();

        if (matchedCred.isEmpty()) {
            logger.debug("Authentication failed: password does not match any stored values");

            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        Instant expiration = matchedCred.map(UserCredential::getExpiration).orElse(null);
        if (expiration != null && expiration.isBefore(Instant.now())) {
            logger.debug("User account credentials have expired");

            throw new CredentialsExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.credentialsExpired", "User credentials have expired"));
        }

        // perform upgrade if needed for password-based auth
        if ("".equals(encoderSpecialization) && getPasswordEncoder().upgradeEncoding("{" + matchedCred.get().getType() + "}" + matchedCred.get().getCredential())) {
            UserCredential upgraded = new UserCredential(matchedCred.get());
            upgraded.setCredential(authentication.getCredentials().toString());
            if (!securityService.updateCredentials(matchedCred.get(), upgraded, upgraded.getComment() + " | Automatically upgraded by system", true)) {
                logger.debug("Password needs to be upgraded, but failed");
            }
        }
    }

    @Autowired
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    @Autowired
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        super.setUserDetailsService(userDetailsService);
    }

    @Override
    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        super.setPasswordEncoder(passwordEncoder);
    }
}
