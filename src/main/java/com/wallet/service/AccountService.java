package com.wallet.service;

import com.google.inject.Inject;
import com.wallet.model.Account;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountService {
    private final DataSource dataSource;

    @Inject
    public AccountService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Account> findAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet res = statement.executeQuery("select * from account")) {
            while (res.next()) {
                accounts.add(new Account(
                        res.getInt(1),
                        res.getString(2),
                        res.getString(3),
                        res.getString(4),
                        res.getString(5),
                        res.getInt(6)));
            }
        }
        return accounts;
    }

    public List<Account> findByName(String name) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select * from account where fName like ? or lName like ?")) {
            String pattern = "%" + name + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);

            try (ResultSet res = statement.executeQuery()) {
                while (res.next()) {
                    Account acc = new Account(
                            res.getInt(1),
                            res.getString(2),
                            res.getString(3),
                            res.getString(4),
                            res.getString(5),
                            res.getInt(6));
                    accounts.add(acc);
                }
            }
        }
        return accounts;
    }

    public Account findById(int id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select * from account where id = ?")) {
            statement.setInt(1, id);
            try (ResultSet res = statement.executeQuery()) {
                if (!res.next()) {
                    return null;
                }
                return new Account(
                        res.getInt(1),
                        res.getString(2),
                        res.getString(3),
                        res.getString(4),
                        res.getString(5),
                        res.getInt(6));
            }
        }
    }

}
