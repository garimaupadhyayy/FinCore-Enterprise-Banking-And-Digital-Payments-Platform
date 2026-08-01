package com.fincore.service;

import com.fincore.config.JwtUtil;
import com.fincore.dto.AuthResponse;
import com.fincore.dto.LoginRequest;
import com.fincore.dto.RegisterRequest;
import com.fincore.exception.InvalidCredentialsException;
import com.fincore.model.Account;
import com.fincore.model.Customer;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(CustomerRepository customerRepository, AccountRepository accountRepository,
                        PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (customerRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        Customer customer = new Customer(
                req.getUsername(),
                passwordEncoder.encode(req.getPassword()),
                req.getFullName(),
                req.getEmail(),
                Customer.Role.CUSTOMER
        );
        customer = customerRepository.save(customer);

        // auto-create a linked account, pending admin approval
        Account.AccountType type = "CURRENT".equalsIgnoreCase(req.getAccountType())
                ? Account.AccountType.CURRENT : Account.AccountType.SAVINGS;

        Account account = new Account(generateAccountNumber(), customer, type, BigDecimal.ZERO);
        accountRepository.save(account);

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole().name());
        return new AuthResponse(token, customer.getUsername(), customer.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        Customer customer = customerRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), customer.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole().name());
        return new AuthResponse(token, customer.getUsername(), customer.getRole().name());
    }

    private String generateAccountNumber() {
        return "FC" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10).toUpperCase();
    }
}
