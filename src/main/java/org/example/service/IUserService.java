package org.example.service;

import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {

    void registerUser(String firstName, String lastName, String email, String password);

    Optional<User> login(String email, String password);

    List<User> getAllUsers();
}
