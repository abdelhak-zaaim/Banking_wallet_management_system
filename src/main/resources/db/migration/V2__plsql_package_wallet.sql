CREATE OR REPLACE PACKAGE wallet_pkg AS
  PROCEDURE transfer_funds(p_from IN NUMBER, p_to IN NUMBER, p_amount IN NUMBER);
END wallet_pkg;
/
CREATE OR REPLACE PACKAGE BODY wallet_pkg AS
  PROCEDURE transfer_funds(p_from IN NUMBER, p_to IN NUMBER, p_amount IN NUMBER) IS
  BEGIN
    --
    NULL;
  END;
END wallet_pkg;
/