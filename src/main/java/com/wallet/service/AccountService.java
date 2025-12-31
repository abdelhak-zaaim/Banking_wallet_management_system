package com.wallet.service;

import com.google.inject.Inject;
import com.wallet.database.util.SqlTemplate;
import com.wallet.model.Account;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Optional;

public class AccountService {

    private final SqlTemplate sqlTemplate;

    @Inject
    public AccountService(SqlTemplate sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public List<Account> findAll() {

        List<Account> accounts = sqlTemplate.select("select * from account", rs -> new Account(
                rs.getInt("id"),
                rs.getString("fName"),
                rs.getString("lName"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("wallet_id")));

        return accounts;
    }

    public List<Account> findByName(String name) {

        List<Account> accounts = sqlTemplate.select("select * from account where fName like ? or lName like ?", rs -> new Account(
                rs.getInt("id"),
                rs.getString("fName"),
                rs.getString("lName"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("wallet_id")),
                name, name);

        return accounts;
    }

    public Optional<Account> findById(int id) {

        @Language("SQL")
        String sql = "select * from account where id = ?";

        Optional<Account> accounts = sqlTemplate.selectOne(sql, rs -> new Account(
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
    public Account addAccount(Account account) {

        @Language("SQL")
        String sql = "insert into account (fName, lName, email, password, wallet_id) values (?, ?, ?, ?, ?)";

        Optional<Long> id = sqlTemplate.insertAndGetKey(sql, account.getfName(), account.getlName(), account.getEmail(), account.getPassword(), account.getWalletId());

        return account.withId(id.get().intValue());
    }
}
