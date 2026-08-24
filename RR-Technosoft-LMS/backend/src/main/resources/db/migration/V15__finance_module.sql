-- =====================================================================
-- RR TECHNOSOFT LMS — Finance Module (V15)
-- Fee Structures, Student Fees, Installments, Discounts, Fines,
-- Payments (gateway + manual), Refunds, Receipts.
-- PostgreSQL 15+
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM TYPES
-- ---------------------------------------------------------------------
CREATE TYPE fee_status AS ENUM ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED', 'CANCELLED');
CREATE TYPE installment_status AS ENUM ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED');
CREATE TYPE payment_status AS ENUM ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED');
CREATE TYPE payment_method AS ENUM ('CARD', 'UPI', 'NETBANKING', 'WALLET', 'CASH', 'BANK_TRANSFER', 'CHEQUE');
CREATE TYPE payment_gateway_provider AS ENUM ('RAZORPAY', 'MANUAL');
CREATE TYPE discount_type AS ENUM ('PERCENTAGE', 'FLAT');
CREATE TYPE fine_status AS ENUM ('PENDING', 'WAIVED', 'PAID');
CREATE TYPE refund_status AS ENUM ('REQUESTED', 'APPROVED', 'REJECTED', 'PROCESSED', 'FAILED');

-- ---------------------------------------------------------------------
-- FEE STRUCTURES (reusable templates, optionally scoped to a course)
-- ---------------------------------------------------------------------
CREATE TABLE fee_structures (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id           UUID REFERENCES courses(id) ON DELETE SET NULL,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    total_amount        NUMERIC(12,2) NOT NULL CHECK (total_amount > 0),
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    installment_count   INT NOT NULL DEFAULT 1 CHECK (installment_count >= 1),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_structures_course ON fee_structures(course_id);
CREATE INDEX idx_fee_structures_active ON fee_structures(is_active);

CREATE TABLE fee_structure_installments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fee_structure_id    UUID NOT NULL REFERENCES fee_structures(id) ON DELETE CASCADE,
    installment_number  INT NOT NULL CHECK (installment_number >= 1),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    due_after_days      INT NOT NULL DEFAULT 0 CHECK (due_after_days >= 0),
    UNIQUE (fee_structure_id, installment_number)
);

-- ---------------------------------------------------------------------
-- STUDENT FEES (a fee structure assigned/instantiated for one student)
-- ---------------------------------------------------------------------
CREATE TABLE student_fees (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id           UUID REFERENCES courses(id) ON DELETE SET NULL,
    fee_structure_id    UUID REFERENCES fee_structures(id) ON DELETE SET NULL,
    total_amount        NUMERIC(12,2) NOT NULL CHECK (total_amount > 0),
    discount_amount     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    fine_amount         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (fine_amount >= 0),
    net_payable         NUMERIC(12,2) NOT NULL CHECK (net_payable >= 0),
    amount_paid         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              fee_status NOT NULL DEFAULT 'PENDING',
    assigned_by         UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_student_fees_student ON student_fees(student_id);
CREATE INDEX idx_student_fees_course ON student_fees(course_id);
CREATE INDEX idx_student_fees_status ON student_fees(status);
-- one active fee record per student per course (course-scoped fees only)
CREATE UNIQUE INDEX uq_student_fee_student_course ON student_fees(student_id, course_id) WHERE course_id IS NOT NULL;

CREATE TABLE student_fee_installments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_fee_id      UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    installment_number  INT NOT NULL CHECK (installment_number >= 1),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    due_date            DATE NOT NULL,
    paid_amount         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    status              installment_status NOT NULL DEFAULT 'PENDING',
    paid_at             TIMESTAMPTZ,
    UNIQUE (student_fee_id, installment_number)
);
CREATE INDEX idx_sfi_student_fee ON student_fee_installments(student_fee_id);
CREATE INDEX idx_sfi_due_date ON student_fee_installments(due_date);
CREATE INDEX idx_sfi_status ON student_fee_installments(status);

-- ---------------------------------------------------------------------
-- DISCOUNTS & FINES (adjustments against a student's fee record)
-- ---------------------------------------------------------------------
CREATE TABLE fee_discounts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_fee_id      UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    type                discount_type NOT NULL,
    value               NUMERIC(12,2) NOT NULL CHECK (value > 0),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    reason              TEXT NOT NULL,
    approved_by         UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_discounts_student_fee ON fee_discounts(student_fee_id);

CREATE TABLE fee_fines (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_fee_id      UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    installment_id      UUID REFERENCES student_fee_installments(id) ON DELETE SET NULL,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    reason              TEXT NOT NULL,
    status              fine_status NOT NULL DEFAULT 'PENDING',
    imposed_by          UUID NOT NULL REFERENCES users(id),
    waived_by           UUID REFERENCES users(id),
    waived_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_fines_student_fee ON fee_fines(student_fee_id);

-- ---------------------------------------------------------------------
-- PAYMENTS (gateway-backed and manual/offline)
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_fee_id      UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    installment_id      UUID REFERENCES student_fee_installments(id) ON DELETE SET NULL,
    student_id          UUID NOT NULL REFERENCES users(id),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    method              payment_method,
    gateway_provider    payment_gateway_provider NOT NULL DEFAULT 'RAZORPAY',
    gateway_order_id    VARCHAR(100),
    gateway_payment_id  VARCHAR(100),
    gateway_signature   VARCHAR(255),
    status              payment_status NOT NULL DEFAULT 'INITIATED',
    failure_reason      TEXT,
    refunded_amount     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    recorded_by         UUID REFERENCES users(id),
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_student_fee ON payments(student_fee_id);
CREATE INDEX idx_payments_student ON payments(student_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE UNIQUE INDEX uq_payments_gateway_order ON payments(gateway_order_id) WHERE gateway_order_id IS NOT NULL;
CREATE UNIQUE INDEX uq_payments_gateway_payment ON payments(gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;

CREATE TABLE payment_refunds (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id          UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    reason              TEXT NOT NULL,
    status              refund_status NOT NULL DEFAULT 'REQUESTED',
    gateway_refund_id   VARCHAR(100),
    requested_by        UUID NOT NULL REFERENCES users(id),
    processed_by        UUID REFERENCES users(id),
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ
);
CREATE INDEX idx_payment_refunds_payment ON payment_refunds(payment_id);

-- ---------------------------------------------------------------------
-- RECEIPTS (immutable proof of a successful payment; PDF stored inline)
-- ---------------------------------------------------------------------
CREATE SEQUENCE receipt_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE receipts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id          UUID NOT NULL UNIQUE REFERENCES payments(id) ON DELETE CASCADE,
    student_fee_id      UUID NOT NULL REFERENCES student_fees(id),
    receipt_number      VARCHAR(50) NOT NULL UNIQUE,
    amount              NUMERIC(12,2) NOT NULL,
    pdf_data            BYTEA,
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    generated_by        UUID REFERENCES users(id)
);
CREATE INDEX idx_receipts_student_fee ON receipts(student_fee_id);

-- ---------------------------------------------------------------------
-- Extend audit log usage note: finance actions reuse the existing
-- audit_logs table (action values: ASSIGN_FEE, RECORD_PAYMENT,
-- VERIFY_PAYMENT, ISSUE_REFUND, WAIVE_FINE, APPLY_DISCOUNT, etc).
-- No schema change needed there.
-- ---------------------------------------------------------------------
