CREATE TABLE employee (
    id            UUID         PRIMARY KEY,
    employee_code VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    job_title     VARCHAR(255) NOT NULL,
    department    VARCHAR(255) NOT NULL,
    country       VARCHAR(2)   NOT NULL CHECK (country ~ '^[A-Z]{2}$'),
    work_email    VARCHAR(255) NOT NULL,
    phone         VARCHAR(255),
    hire_date     DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'TERMINATED')),
    version       BIGINT       NOT NULL,
    CONSTRAINT uk_employee_code UNIQUE (employee_code)
);
