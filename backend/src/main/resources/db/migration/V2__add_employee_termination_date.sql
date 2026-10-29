ALTER TABLE employee
    ADD COLUMN termination_date DATE,
    ADD CONSTRAINT ck_employee_termination_matches_status
        CHECK ((status = 'TERMINATED') = (termination_date IS NOT NULL)),
    ADD CONSTRAINT ck_employee_termination_after_hire
        CHECK (termination_date IS NULL OR termination_date >= hire_date);
