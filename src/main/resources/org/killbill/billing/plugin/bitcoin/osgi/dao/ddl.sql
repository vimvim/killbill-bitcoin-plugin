/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS btc_pending_payments;
CREATE TABLE btc_pending_payments (
    record_id integer NOT NULL AUTO_INCREMENT,
    payment_id char(36) NOT NULL,
    account_id char(36) NOT NULL,
    tenant_id char(36),
    btc_tx varchar(128) DEFAULT NULL,
    btc_contract_id char(36) DEFAULT NULL,
    PRIMARY KEY(record_id)
) CHARACTER SET utf8 COLLATE utf8_bin;
CREATE UNIQUE INDEX pending_payments_payment_id ON btc_pending_payments(payment_id);
CREATE UNIQUE INDEX pending_payments_btc_tx ON btc_pending_payments(btc_tx);

DROP TABLE IF EXISTS transaction_logs;
CREATE TABLE transaction_logs (
    record_id integer NOT NULL AUTO_INCREMENT,
    account_id char(36) NOT NULL,
    subscription_id char(36),
    contract_id char(36) NOT NULL,
    api_call varchar(255) NOT NULL,
    created_date datetime DEFAULT NULL,
    PRIMARY KEY(record_id)
) CHARACTER SET utf8 COLLATE utf8_bin;

