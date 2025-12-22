create or replace package wallet_pkg authid definer as
    procedure transfer(p_from in number, p_to in number, p_amount in number);
end wallet_pkg;
/

create or replace package body wallet_pkg as
    procedure transfer(p_from in ACCOUNT.ID%type, p_to in ACCOUNT.ID%type, p_amount in number) is

        insufficient_balance EXCEPTION;

        v_min_id ACCOUNT.ID%type;
        v_max_id ACCOUNT.ID%type;
        v_locked pls_integer := 0;
    begin
        if p_from = p_to then
            raise_application_error(-20003, 'Source and destination must differ');
        end if;

        if p_amount is null or p_amount <= 0 then
            raise_application_error(-20004, 'Amount must be positive');
        end if;

        v_min_id := least(p_from, p_to);
        v_max_id := greatest(p_from, p_to);

        for r in (
            select id
            from WALLET_ACCOUNT
            where id in (v_min_id, v_max_id)
            order by id
                for update
            )
            loop
                v_locked := v_locked + 1;
            end loop;

        if v_locked != 2 then
            raise_application_error(-20005, 'Account not found');
        end if;

        update WALLET_ACCOUNT
        set BALANCE = BALANCE - p_amount
        where ID = p_from
          and BALANCE >= p_amount;

        if sql%rowcount = 0 then
            raise insufficient_balance;
        end if;

        update WALLET_ACCOUNT
        set BALANCE = BALANCE + p_amount
        where ID = p_to;

    exception
        when insufficient_balance then
            raise_application_error(-20006, 'Insufficient balance');
        when others then
            raise;
    end;

end wallet_pkg;
/