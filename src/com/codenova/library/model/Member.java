package com.codenova.library.model;

import java.time.LocalDate;

/** A registered library member with an issued library card. */
public class Member {
    private final int id;
    private final String cardNumber;
    private final String name;
    private final String email;
    private final String phone;
    private final LocalDate registeredOn;

    public Member(int id, String cardNumber, String name, String email, String phone, LocalDate registeredOn) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.registeredOn = registeredOn;
    }

    public int getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getRegisteredOn() { return registeredOn; }

    @Override
    public String toString() {
        return cardNumber + " - " + name;
    }
}
