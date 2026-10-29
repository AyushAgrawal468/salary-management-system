CREATE TABLE salary_record (
    id             UUID           PRIMARY KEY,
    employee_id    UUID           NOT NULL REFERENCES employee (id),
    -- Annual base salary in the currency below; NUMERIC, never floating point, for money.
    amount         NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)     NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    effective_date DATE           NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_salary_record_employee_date UNIQUE (employee_id, effective_date)
);
