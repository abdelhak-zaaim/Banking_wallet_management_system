create or replace package wallet_pkg authid definer as
    procedure transfer(
        p_request_id in varchar2,
        p_from       in WALLET_ACCOUNT.ID%type,
        p_to         in WALLET_ACCOUNT.ID%type,
        p_currency   in WALLET_ACCOUNT.CURRENCY%type,
        p_amount     in number
    );
end wallet_pkg;
/

create or replace package body wallet_pkg as
    c_err_same_account       constant pls_integer := -20003;
    c_err_invalid_amount     constant pls_integer := -20004;
    c_err_acct_missing       constant pls_integer := -20005;
    c_err_insufficient       constant pls_integer := -20006;
    c_err_status_invalid     constant pls_integer := -20007;
    c_err_currency_mismatch  constant pls_integer := -20008;
    c_err_duplicate_request  constant pls_integer := -20009;

    procedure transfer(
        p_request_id in varchar2,
        p_from       in WALLET_ACCOUNT.ID%type,
        p_to         in WALLET_ACCOUNT.ID%type,
        p_currency   in WALLET_ACCOUNT.CURRENCY%type,
        p_amount     in number
    ) is
        v_min_id WALLET_ACCOUNT.ID%type;
        v_max_id WALLET_ACCOUNT.ID%type;
        v_locked pls_integer := 0;
        v_from_currency WALLET_ACCOUNT.CURRENCY%type;
        v_to_currency   WALLET_ACCOUNT.CURRENCY%type;
        v_from_status   WALLET_ACCOUNT.STATUS%type;
        v_to_status     WALLET_ACCOUNT.STATUS%type;
        v_from_balance  number;
    begin
        if p_request_id is null then
            raise_application_error(c_err_invalid_amount, 'Request id is required');
        end if;
        if p_from = p_to then
            raise_application_error(c_err_same_account, 'Source and destination must differ');
        end if;
        if p_amount is null or p_amount <= 0 then
            raise_application_error(c_err_invalid_amount, 'Amount must be positive');
        end if;

        declare
            v_cnt pls_integer;
        begin
            select count(*) into v_cnt
            from WALLET_JOURNAL
            where REQUEST_ID = p_request_id;
            if v_cnt = 2 then
                return;
            elsif v_cnt = 1 then
                raise_application_error(c_err_duplicate_request, 'Duplicate or partial request');
            end if;
        end;

        v_min_id := least(p_from, p_to);
        v_max_id := greatest(p_from, p_to);

        for r in (
            select id, currency, status, balance
            from WALLET_ACCOUNT
            where id in (v_min_id, v_max_id)
            order by id
                for update
            ) loop
                v_locked := v_locked + 1;
                if r.id = p_from then
                    v_from_currency := r.currency;
                    v_from_status   := r.status;
                    v_from_balance  := r.balance;
                else
                    v_to_currency := r.currency;
                    v_to_status   := r.status;
                end if;
            end loop;

        if v_locked != 2 then
            raise_application_error(c_err_acct_missing, 'Account not found');
        end if;

        if v_from_status != 'ACTIVE' or v_to_status != 'ACTIVE' then
            raise_application_error(c_err_status_invalid, 'Account status not active');
        end if;

        if v_from_currency != p_currency or v_to_currency != p_currency then
            raise_application_error(c_err_currency_mismatch, 'Currency mismatch');
        end if;

        update WALLET_ACCOUNT
        set BALANCE = BALANCE - p_amount
        where ID = p_from
          and BALANCE >= p_amount;

        if sql%rowcount = 0 then
            raise_application_error(c_err_insufficient, 'Insufficient balance');
        end if;

        update WALLET_ACCOUNT
        set BALANCE = BALANCE + p_amount
        where ID = p_to;

        insert into WALLET_JOURNAL (REQUEST_ID, POSTING_SEQ, ACCOUNT_ID, CURRENCY, AMOUNT, SIDE, DESCRIPTION)
        values (p_request_id, 1, p_from, p_currency, p_amount, 'DEBIT', 'Transfer out');

        insert into WALLET_JOURNAL (REQUEST_ID, POSTING_SEQ, ACCOUNT_ID, CURRENCY, AMOUNT, SIDE, DESCRIPTION)
        values (p_request_id, 2, p_to,   p_currency, p_amount, 'CREDIT', 'Transfer in');

    exception
        when others then
            raise;
    end transfer;
end wallet_pkg;
/
