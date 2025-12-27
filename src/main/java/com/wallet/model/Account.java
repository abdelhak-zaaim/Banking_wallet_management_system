package com.wallet.model;

import java.util.Objects;

public class Account {
    private int id;
    private String fName;
    private String lName;
    private String email;
    private String password;
    private int walletId;

    public Account(int id, String fName, String lName, String email, String password, int walletId) {
        this.id = id;
        this.fName = fName;
        this.lName = lName;
        this.email = email;
        this.password = password;
        this.walletId = walletId;
    }

    public int getId() {
        return id;
    }

    public String getfName() {
        return fName;
    }

    public String getlName() {
        return lName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public int getWalletId() {
        return walletId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id == account.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
