-- Runs on every startup (spring.sql.init.mode=always) against ddl-auto=update,
-- so IDs are fixed and re-running just re-inserts the same rows. Fine for a
-- local dev/demo setup - a real project would gate this behind a profile.

INSERT INTO users (id, name, email, password, phone, created_at) VALUES
    (1, 'Usha', 'usha@example.com', 'password123', '9000000001', NOW()),
    (2, 'Ravi', 'ravi@example.com', 'password123', '9000000002', NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO insurance_plans (id, plan_name, description, coverage_amount, premium_amount, created_at) VALUES
    (1, 'Health Secure', 'Individual health cover with hospitalization benefits', 500000.00, 1200.00, NOW()),
    (2, 'Family Health Plus', 'Floater health cover for the whole family', 1000000.00, 2500.00, NOW())
ON DUPLICATE KEY UPDATE plan_name = VALUES(plan_name);

INSERT INTO policies (id, policy_number, user_id, insurance_plan_id, start_date, end_date, status) VALUES
    (1, 'POL1001', 1, 1, '2026-01-01', '2026-12-31', 'ACTIVE'),
    (2, 'POL1002', 2, 2, '2026-03-01', '2027-02-28', 'ACTIVE')
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO claims (id, claim_number, policy_id, claim_type, description, claim_amount, status, submitted_at) VALUES
    (1, 'CLM1001', 1, 'Hospitalization', '3-day hospitalization for fever and dehydration', 45000.00, 'UNDER_REVIEW', NOW()),
    (2, 'CLM1002', 2, 'Accident', 'Minor road accident, outpatient treatment', 12000.00, 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);
