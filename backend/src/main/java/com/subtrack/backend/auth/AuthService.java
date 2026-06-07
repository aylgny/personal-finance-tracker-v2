package com.subtrack.backend.auth;

import com.subtrack.backend.auth.dto.AuthResponse;
import com.subtrack.backend.auth.dto.LoginRequest;
import com.subtrack.backend.auth.dto.RegisterRequest;
import com.subtrack.backend.category.Category;
import com.subtrack.backend.category.CategoryRepository;
import com.subtrack.backend.currency.Currency;
import com.subtrack.backend.currency.CurrencyRepository;
import com.subtrack.backend.paymentmethod.PaymentMethod;
import com.subtrack.backend.paymentmethod.PaymentMethodRepository;
import com.subtrack.backend.shared.exception.DuplicateResourceException;
import com.subtrack.backend.shared.exception.UnauthorizedException;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrencyRepository currencyRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrencyRepository currencyRepository,
            CategoryRepository categoryRepository,
            PaymentMethodRepository paymentMethodRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currencyRepository = currencyRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        // Prevent duplicate accounts with the same email address.
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        // Store only the hashed password, never the raw password.
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.name(),
                request.email(),
                passwordHash
        );

        // Save the new user before creating default records and generating the JWT.
        User savedUser = userRepository.save(user);

        // Create default records so a new user can immediately create subscriptions.
        createDefaultUserData(savedUser);

        // Generate a JWT containing the saved user's ID and email.
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {
        // Use a generic error message so attackers cannot learn whether the email exists.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Compare the raw password from the request with the stored password hash.
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate a JWT after the credentials are verified.
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private void createDefaultUserData(User user) {
        // Default currency belongs only to the newly registered user.
        currencyRepository.save(new Currency(
                user,
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        ));

        // Default category gives the user an immediate option when creating a subscription.
        categoryRepository.save(new Category(
                user,
                "General",
                1
        ));

        // Default payment method is enabled so it can be selected immediately.
        paymentMethodRepository.save(new PaymentMethod(
                user,
                "Default Payment Method",
                true,
                1
        ));
    }
}