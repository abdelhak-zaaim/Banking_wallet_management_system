package com.wallet.service;

import com.google.inject.Inject;
import com.wallet.database.util.SQLUtils;
import com.wallet.model.Account;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountService {
    private final DataSource dataSource;
    private final SQLUtils sqlUtil;

    @Inject
    public AccountService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.sqlUtil = new SQLUtils(dataSource);
    }

    public List<Account> findAll() throws SQLException {

        List<Account> accounts = sqlUtil.select("select * from account", rs -> new Account(
                rs.getInt("id"),
                rs.getString("fName"),
                rs.getString("lName"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("wallet_id")));

        return accounts;
    }

    public List<Account> findByName(String name) throws SQLException {

        List<Account> accounts = sqlUtil.select("select * from account where fName like ? or lName like ?", rs -> new Account(
                rs.getInt("id"),
                rs.getString("fName"),
                rs.getString("lName"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("wallet_id")),
                name, name);

        return accounts;
    }

    public Optional<Account> findById(int id) throws SQLException {

        @Language("SQL")
        String sql = "select * from account where id = ?";

        Optional<Account> accounts = sqlUtil.selectOne(sql, rs -> new Account(
                        rs.getInt("id"),
                        rs.getString("fName"),
                        rs.getString("lName"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getInt("wallet_id")),
                id);

        return accounts;
    }
    // make sure a hash the password before call this method
    public Account addAccount(Account account) throws SQLException {

        @Language("SQL")
        String sql = "insert into account (fName, lName, email, password, wallet_id) values (?, ?, ?, ?, ?)";

        int id = sqlUtil.insert(sql, account.getfName(), account.getlName(), account.getEmail(), account.getPassword(), account.getWalletId());

        return account.withId(id);
    }
}
