create or replace package wallet_pkg authid definer as
    procedure transfer(p_from in number, p_to in number, p_amount in number);
end wallet_pkg;
/

create or replace package body wallet_pkg as
    procedure transfer(p_from in ACCOUNT.ID%type, p_to in ACCOUNT.ID%type, p_amount in number) is
        from_balance WALLET_ACCOUNT.BALANCE%type;
        insufficient_balance EXCEPTION;
    begin
        select BALANCE into from_balance from WALLET_ACCOUNT where ID = p_from for update;
        if from_balance < p_amount then
            raise insufficient_balance;
        else
            update WALLET_ACCOUNT set BALANCE = BALANCE - p_amount where id = p_from;
            update WALLET_ACCOUNT set BALANCE = BALANCE + p_amount where id = p_to;
        end if;


    exception
        when insufficient_balance then
            DBMS_OUTPUT.PUT_LINE('Insufficient balance');
            raise;
        when others then
            DBMS_OUTPUT.PUT_LINE('Error : Please try again');
            raise;
    end;

end wallet_pkg;
/