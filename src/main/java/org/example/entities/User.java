package org.example.entities;

public class User {
    private int id;
    private String email;
    private String roles;
    private String password;
    private String subscriptionPlan;
    private boolean isPremium;
    private String lastPaymentStatus;
    private String firstName;
    private String lastName;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public String getLastPaymentStatus() { return lastPaymentStatus; }
    public void setLastPaymentStatus(String lastPaymentStatus) { this.lastPaymentStatus = lastPaymentStatus; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
