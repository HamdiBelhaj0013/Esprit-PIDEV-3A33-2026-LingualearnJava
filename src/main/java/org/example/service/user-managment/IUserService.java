package org.example.service;

import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {

    /**
     * Registers a new user account with a hashed password.
     * Throws {@link IllegalArgumentException} if validation fails or the
     * email is already taken.
     */
    void registerUser(String firstName, String lastName, String email, String password);

    /**
     * Authenticates by email + plain-text password.
     * Returns the user if credentials are valid and the account is active,
     * or empty if login should be denied.
     */
    Optional<User> login(String email, String password);

    /** Returns all users ordered by creation date descending. */
    List<User> getAllUsers();
}
