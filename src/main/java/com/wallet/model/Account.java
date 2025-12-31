package com.wallet.model;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public class Account {
    private final int id;
    private final String fName;
    private final String lName;
    private final String email;
    private final String password;
    private final int walletId;

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

    public Account withId(int id) {
        return new Account(id, fName, lName, email, password, walletId);
    }

    public Account withfName(String fName) {
        return new Account(id, fName, lName, email, password, walletId);
    }

    public Account withlName(String lName) {
        return new Account(id, fName, lName, email, password, walletId);
    }

    public Account withEmail(String email) {
        return new Account(id, fName, lName, email, password, walletId);
    }

    public Account withPassword(String password) {
        return new Account(id, fName, lName, email, password, walletId);
    }

    public Account withWalletId(int walletId) {
        return new Account(id, fName, lName, email, password, walletId);
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
