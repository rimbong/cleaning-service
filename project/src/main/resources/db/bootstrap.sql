-- =====================================================================
-- bootstrap.sql — 빈 DB 하나로 완전한 CleanHub DB 를 만드는 독립 실행 파일
--
--   [목적] 임시 배포·데모·ERD 확인처럼 "이 SQL 하나로 바로 쓰는 DB" 가 필요할 때.
--          스키마 + Flyway 이력 + 초기 데이터(admin 계정, 단가 정책)를 모두 담았다.
--          이 파일로 만든 DB 에는 손대지 않은 WAR 를 그대로 붙여 실행할 수 있다
--          (Flyway 가 이력을 보고 "이미 최신" 으로 판단해 마이그레이션을 다시 돌리지 않는다).
--
--   [실행] 빈 DB 를 만든 뒤 이 파일을 돌린다.
--          createdb 후:  psql -U <user> -d <db> -f bootstrap.sql
--
--   [주의] 이 프로젝트의 실제 배포는 이 파일을 쓰지 않는다. 스키마의 소스는
--          여전히 Flyway(db/migration/V*.sql)이고, 정식 배포는 빈 DB + WAR 로 한다.
--          이 파일은 그 결과를 한 방에 재현하는 스냅샷일 뿐이다.
--          스키마를 바꾸면(새 V*.sql 추가) 이 파일도 다시 뽑아 갱신한다.
--
--   기준: 마이그레이션 V28 까지 적용된 상태 (2026-07-21)
--   ERD 도구로 볼 때 아래 \restrict / \unrestrict 는 psql 전용이라 문제되면 지워도 된다.
-- =====================================================================


--
-- PostgreSQL database dump
--

\restrict fjeGHAXaTe6zPFraZFgeb4GaJkNCCLHKU0HdOgAqzDdtsQLzG3oMTxPuACyo8SI

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_user (
    id bigint NOT NULL,
    username character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255)
);


--
-- Name: auth_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.auth_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auth_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.auth_user_id_seq OWNED BY public.auth_user.id;


--
-- Name: billing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.billing (
    id bigint NOT NULL,
    contract_id bigint,
    quote_id bigint,
    bill_year integer NOT NULL,
    bill_month integer NOT NULL,
    amount bigint NOT NULL,
    memo text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: billing_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.billing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: billing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.billing_id_seq OWNED BY public.billing.id;


--
-- Name: client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    address character varying(255),
    manager_name character varying(50),
    manager_phone character varying(30),
    cleaning_type character varying(20),
    contract_start_date date,
    memo text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    business_number character varying(20),
    representative_name character varying(50),
    business_type character varying(50),
    business_item character varying(50),
    tax_invoice_type character varying(20),
    floors integer,
    household_count integer,
    shared_toilets integer,
    extra_floors integer,
    has_elevator boolean
);


--
-- Name: client_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.client_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: client_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.client_id_seq OWNED BY public.client.id;


--
-- Name: company; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company (
    id bigint NOT NULL,
    business_number character varying(20),
    company_name character varying(100),
    owner_name character varying(50),
    address character varying(255),
    business_type character varying(50),
    business_item character varying(50),
    phone character varying(30),
    updated_at timestamp without time zone NOT NULL,
    stamp_image_path character varying(255)
);


--
-- Name: company_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_id_seq OWNED BY public.company.id;


--
-- Name: contract; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    title character varying(100) NOT NULL,
    monthly_fee bigint NOT NULL,
    billing_day integer,
    start_date date NOT NULL,
    end_date date,
    status character varying(20) NOT NULL,
    memo text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    document_location character varying(255),
    payment_method character varying(30),
    door_code character varying(50),
    cleaning_weekdays character varying(30),
    cleaning_cycle character varying(20),
    vat_type character varying(20),
    initial_fee bigint,
    cleaning_scope character varying(255),
    service_items character varying(255),
    extra_services character varying(255),
    extra_notes character varying(255),
    visits_per_month integer
);


--
-- Name: contract_attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contract_attachment (
    id bigint NOT NULL,
    contract_id bigint NOT NULL,
    original_filename character varying(255) NOT NULL,
    content_type character varying(100),
    file_size bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    stored_path character varying(500) NOT NULL
);


--
-- Name: contract_attachment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_attachment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contract_attachment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_attachment_id_seq OWNED BY public.contract_attachment.id;


--
-- Name: contract_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contract_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contract_id_seq OWNED BY public.contract.id;


--
-- Name: expense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense (
    id bigint NOT NULL,
    category character varying(20) NOT NULL,
    vendor_name character varying(100),
    business_number character varying(20),
    amount bigint NOT NULL,
    expense_date date NOT NULL,
    memo character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: expense_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.expense_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: expense_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.expense_id_seq OWNED BY public.expense.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: jwt_refresh_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jwt_refresh_token (
    id bigint NOT NULL,
    username character varying(255) NOT NULL,
    token character varying(512) NOT NULL
);


--
-- Name: jwt_refresh_token_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jwt_refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jwt_refresh_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.jwt_refresh_token_id_seq OWNED BY public.jwt_refresh_token.id;


--
-- Name: payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment (
    id bigint NOT NULL,
    billing_id bigint NOT NULL,
    amount bigint NOT NULL,
    paid_date date NOT NULL,
    method character varying(30),
    memo character varying(255),
    created_at timestamp without time zone NOT NULL
);


--
-- Name: payment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payment_id_seq OWNED BY public.payment.id;


--
-- Name: pricing_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pricing_policy (
    id bigint NOT NULL,
    base_fee bigint NOT NULL,
    per_floor bigint NOT NULL,
    per_household bigint NOT NULL,
    per_toilet bigint NOT NULL,
    elevator_fee bigint NOT NULL,
    rounding_unit bigint NOT NULL,
    memo character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    coef_base numeric(6,4) NOT NULL,
    coef_exponent numeric(6,4) NOT NULL
);


--
-- Name: pricing_policy_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pricing_policy_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pricing_policy_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pricing_policy_id_seq OWNED BY public.pricing_policy.id;


--
-- Name: quote; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quote (
    id bigint NOT NULL,
    client_id bigint,
    customer_name character varying(50),
    customer_phone character varying(30),
    address character varying(255),
    title character varying(100) NOT NULL,
    amount bigint NOT NULL,
    quote_date date NOT NULL,
    valid_until date,
    status character varying(20) NOT NULL,
    memo text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: quote_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quote_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quote_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quote_id_seq OWNED BY public.quote.id;


--
-- Name: supply_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supply_item (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    spec character varying(50),
    unit character varying(20) NOT NULL,
    unit_price bigint,
    safety_qty integer NOT NULL,
    memo character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    ph_type character varying(20)
);


--
-- Name: COLUMN supply_item.ph_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.supply_item.ph_type IS 'ACID(산성) / NEUTRAL(중성) / ALKALI(알칼리성) / OXIDIZER(표백·산화계) / ENZYME(효소계) / ETC(기타). NULL 은 미분류';


--
-- Name: supply_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.supply_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: supply_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.supply_item_id_seq OWNED BY public.supply_item.id;


--
-- Name: supply_transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supply_transaction (
    id bigint NOT NULL,
    item_id bigint NOT NULL,
    tx_type character varying(20) NOT NULL,
    quantity integer NOT NULL,
    tx_date date NOT NULL,
    memo character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: supply_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.supply_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: supply_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.supply_transaction_id_seq OWNED BY public.supply_transaction.id;


--
-- Name: tax_invoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_invoice (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    from_year integer CONSTRAINT tax_invoice_period_year_not_null NOT NULL,
    from_month integer NOT NULL,
    to_month integer NOT NULL,
    supply_amount bigint NOT NULL,
    tax_amount bigint NOT NULL,
    basis character varying(10) NOT NULL,
    issue_date date NOT NULL,
    created_at timestamp without time zone NOT NULL,
    to_year integer NOT NULL
);


--
-- Name: tax_invoice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tax_invoice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tax_invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tax_invoice_id_seq OWNED BY public.tax_invoice.id;


--
-- Name: auth_user id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user ALTER COLUMN id SET DEFAULT nextval('public.auth_user_id_seq'::regclass);


--
-- Name: billing id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing ALTER COLUMN id SET DEFAULT nextval('public.billing_id_seq'::regclass);


--
-- Name: client id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client ALTER COLUMN id SET DEFAULT nextval('public.client_id_seq'::regclass);


--
-- Name: company id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company ALTER COLUMN id SET DEFAULT nextval('public.company_id_seq'::regclass);


--
-- Name: contract id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract ALTER COLUMN id SET DEFAULT nextval('public.contract_id_seq'::regclass);


--
-- Name: contract_attachment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_attachment ALTER COLUMN id SET DEFAULT nextval('public.contract_attachment_id_seq'::regclass);


--
-- Name: expense id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense ALTER COLUMN id SET DEFAULT nextval('public.expense_id_seq'::regclass);


--
-- Name: jwt_refresh_token id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_refresh_token ALTER COLUMN id SET DEFAULT nextval('public.jwt_refresh_token_id_seq'::regclass);


--
-- Name: payment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment ALTER COLUMN id SET DEFAULT nextval('public.payment_id_seq'::regclass);


--
-- Name: pricing_policy id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing_policy ALTER COLUMN id SET DEFAULT nextval('public.pricing_policy_id_seq'::regclass);


--
-- Name: quote id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quote ALTER COLUMN id SET DEFAULT nextval('public.quote_id_seq'::regclass);


--
-- Name: supply_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supply_item ALTER COLUMN id SET DEFAULT nextval('public.supply_item_id_seq'::regclass);


--
-- Name: supply_transaction id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supply_transaction ALTER COLUMN id SET DEFAULT nextval('public.supply_transaction_id_seq'::regclass);


--
-- Name: tax_invoice id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice ALTER COLUMN id SET DEFAULT nextval('public.tax_invoice_id_seq'::regclass);


--
-- Name: auth_user auth_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user
    ADD CONSTRAINT auth_user_pkey PRIMARY KEY (id);


--
-- Name: auth_user auth_user_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user
    ADD CONSTRAINT auth_user_username_key UNIQUE (username);


--
-- Name: billing billing_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing
    ADD CONSTRAINT billing_pkey PRIMARY KEY (id);


--
-- Name: client client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (id);


--
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- Name: contract_attachment contract_attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_attachment
    ADD CONSTRAINT contract_attachment_pkey PRIMARY KEY (id);


--
-- Name: contract contract_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract
    ADD CONSTRAINT contract_pkey PRIMARY KEY (id);


--
-- Name: expense expense_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: jwt_refresh_token jwt_refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_refresh_token
    ADD CONSTRAINT jwt_refresh_token_pkey PRIMARY KEY (id);


--
-- Name: jwt_refresh_token jwt_refresh_token_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_refresh_token
    ADD CONSTRAINT jwt_refresh_token_username_key UNIQUE (username);


--
-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);


--
-- Name: pricing_policy pricing_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing_policy
    ADD CONSTRAINT pricing_policy_pkey PRIMARY KEY (id);


--
-- Name: quote quote_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quote
    ADD CONSTRAINT quote_pkey PRIMARY KEY (id);


--
-- Name: supply_item supply_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supply_item
    ADD CONSTRAINT supply_item_pkey PRIMARY KEY (id);


--
-- Name: supply_transaction supply_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supply_transaction
    ADD CONSTRAINT supply_transaction_pkey PRIMARY KEY (id);


--
-- Name: tax_invoice tax_invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice
    ADD CONSTRAINT tax_invoice_pkey PRIMARY KEY (id);


--
-- Name: billing uq_billing_contract_month; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing
    ADD CONSTRAINT uq_billing_contract_month UNIQUE (contract_id, bill_year, bill_month);


--
-- Name: tax_invoice uq_tax_invoice_client_period_basis; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice
    ADD CONSTRAINT uq_tax_invoice_client_period_basis UNIQUE (client_id, from_year, from_month, to_year, to_month, basis);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_billing_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_contract_id ON public.billing USING btree (contract_id);


--
-- Name: idx_billing_quote_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_quote_id ON public.billing USING btree (quote_id);


--
-- Name: idx_billing_year_month; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_year_month ON public.billing USING btree (bill_year, bill_month);


--
-- Name: idx_client_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_name ON public.client USING btree (name);


--
-- Name: idx_contract_attachment_contract_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_attachment_contract_id ON public.contract_attachment USING btree (contract_id);


--
-- Name: idx_contract_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_client_id ON public.contract USING btree (client_id);


--
-- Name: idx_contract_title; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_title ON public.contract USING btree (title);


--
-- Name: idx_expense_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_date ON public.expense USING btree (expense_date);


--
-- Name: idx_payment_billing_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_billing_id ON public.payment USING btree (billing_id);


--
-- Name: idx_quote_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_client_id ON public.quote USING btree (client_id);


--
-- Name: idx_quote_title; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_title ON public.quote USING btree (title);


--
-- Name: idx_supply_tx_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_supply_tx_date ON public.supply_transaction USING btree (tx_date);


--
-- Name: idx_supply_tx_item; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_supply_tx_item ON public.supply_transaction USING btree (item_id);


--
-- Name: idx_tax_invoice_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tax_invoice_client_id ON public.tax_invoice USING btree (client_id);


--
-- Name: uk_pricing_policy_single_row; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_pricing_policy_single_row ON public.pricing_policy USING btree ((true));


--
-- Name: uk_supply_item_name_spec; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_supply_item_name_spec ON public.supply_item USING btree (name, COALESCE(spec, ''::character varying));


--
-- Name: uq_billing_quote_month; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_billing_quote_month ON public.billing USING btree (quote_id, bill_year, bill_month) WHERE (quote_id IS NOT NULL);


--
-- Name: billing billing_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing
    ADD CONSTRAINT billing_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.contract(id) ON DELETE CASCADE;


--
-- Name: billing billing_quote_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing
    ADD CONSTRAINT billing_quote_id_fkey FOREIGN KEY (quote_id) REFERENCES public.quote(id) ON DELETE CASCADE;


--
-- Name: contract_attachment contract_attachment_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract_attachment
    ADD CONSTRAINT contract_attachment_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.contract(id) ON DELETE CASCADE;


--
-- Name: contract contract_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contract
    ADD CONSTRAINT contract_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: payment payment_billing_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_billing_id_fkey FOREIGN KEY (billing_id) REFERENCES public.billing(id) ON DELETE CASCADE;


--
-- Name: quote quote_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quote
    ADD CONSTRAINT quote_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.client(id) ON DELETE SET NULL;


--
-- Name: supply_transaction supply_transaction_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supply_transaction
    ADD CONSTRAINT supply_transaction_item_id_fkey FOREIGN KEY (item_id) REFERENCES public.supply_item(id);


--
-- Name: tax_invoice tax_invoice_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice
    ADD CONSTRAINT tax_invoice_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- PostgreSQL database dump complete
--

\unrestrict fjeGHAXaTe6zPFraZFgeb4GaJkNCCLHKU0HdOgAqzDdtsQLzG3oMTxPuACyo8SI


--
-- PostgreSQL database dump
--

\restrict H4TfHFIdCAGiJX8pdCS4MEfJltpHuW7hTXheegakZ6oqoyzBIHfhByqjk4HcCtc

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init	SQL	V1__init.sql	92248226	cleanhub_user	2026-07-07 17:00:19.123015	7	t
2	2	seed admin	SQL	V2__seed_admin.sql	1166292529	cleanhub_user	2026-07-07 17:00:19.136957	48	t
3	3	create client	SQL	V3__create_client.sql	-416269328	cleanhub_user	2026-07-07 17:26:04.716707	8	t
4	4	create contract	SQL	V4__create_contract.sql	24635691	cleanhub_user	2026-07-08 11:13:32.140125	31	t
5	5	add contract document	SQL	V5__add_contract_document.sql	1677557693	cleanhub_user	2026-07-08 13:39:20.594129	16	t
6	6	contract attachment to filesystem	SQL	V6__contract_attachment_to_filesystem.sql	-925682380	cleanhub_user	2026-07-08 13:57:56.284167	7	t
7	7	create quote	SQL	V7__create_quote.sql	1381239320	cleanhub_user	2026-07-08 15:48:50.648579	7	t
8	8	create company	SQL	V8__create_company.sql	91267953	cleanhub_user	2026-07-09 11:27:16.283183	14	t
9	9	alter client tax info	SQL	V9__alter_client_tax_info.sql	-1824530158	cleanhub_user	2026-07-09 11:27:16.303212	3	t
10	10	alter contract payment	SQL	V10__alter_contract_payment.sql	-1162061227	cleanhub_user	2026-07-09 11:27:16.310263	1	t
11	11	create billing payment	SQL	V11__create_billing_payment.sql	-786582322	cleanhub_user	2026-07-09 11:37:44.86258	16	t
12	12	create tax invoice	SQL	V12__create_tax_invoice.sql	994973567	cleanhub_user	2026-07-09 11:47:41.352984	14	t
13	13	create expense	SQL	V13__create_expense.sql	-1380909629	cleanhub_user	2026-07-09 11:54:18.899465	12	t
14	14	alter contract cleaning schedule	SQL	V14__alter_contract_cleaning_schedule.sql	-299442558	cleanhub_user	2026-07-09 15:40:02.923665	6	t
15	15	unique quote billing and tax invoice	SQL	V15__unique_quote_billing_and_tax_invoice.sql	-264654701	cleanhub_user	2026-07-10 13:16:10.424864	19	t
16	16	alter contract vat type	SQL	V16__alter_contract_vat_type.sql	1393961602	cleanhub_user	2026-07-10 13:30:01.744461	18	t
17	17	tax invoice period year range	SQL	V17__tax_invoice_period_year_range.sql	248652319	cleanhub_user	2026-07-10 14:40:29.581453	63	t
18	18	alter company stamp	SQL	V18__alter_company_stamp.sql	683996020	cleanhub_user	2026-07-10 15:58:17.369902	13	t
19	19	alter contract service fields	SQL	V19__alter_contract_service_fields.sql	1139762128	cleanhub_user	2026-07-10 19:20:01.5185	31	t
20	20	alter contract extra service fields	SQL	V20__alter_contract_extra_service_fields.sql	-1085870520	cleanhub_user	2026-07-13 14:14:38.028753	33	t
21	21	create supply	SQL	V21__create_supply.sql	-406898690	cleanhub_user	2026-07-20 09:27:57.522285	76	t
22	22	create pricing policy	SQL	V22__create_pricing_policy.sql	2004078344	cleanhub_user	2026-07-20 10:43:09.312815	42	t
23	23	alter supply ph type	SQL	V23__alter_supply_ph_type.sql	-301247753	cleanhub_user	2026-07-20 10:56:17.022794	21	t
24	24	fix pricing policy memo	SQL	V24__fix_pricing_policy_memo.sql	326178537	cleanhub_user	2026-07-20 11:16:38.928435	14	t
25	25	pricing coefficient formula	SQL	V25__pricing_coefficient_formula.sql	-92306234	cleanhub_user	2026-07-20 12:51:39.762968	39	t
26	26	alter contract visits per month	SQL	V26__alter_contract_visits_per_month.sql	-129839444	cleanhub_user	2026-07-20 13:24:56.578552	18	t
27	27	fix monthly visits backfill	SQL	V27__fix_monthly_visits_backfill.sql	-603984504	cleanhub_user	2026-07-20 13:31:41.902397	6	t
28	28	pricing policy single row	SQL	V28__pricing_policy_single_row.sql	-831554192	cleanhub_user	2026-07-20 17:10:19.806776	7	t
\.


--
-- PostgreSQL database dump complete
--

\unrestrict H4TfHFIdCAGiJX8pdCS4MEfJltpHuW7hTXheegakZ6oqoyzBIHfhByqjk4HcCtc



-- =====================================================================
-- 초기 데이터 — 앱을 바로 쓰기 위한 최소 시드
-- =====================================================================

-- pg_dump 가 위에서 search_path 를 비워두어(안전 목적) pgcrypto 함수를 못 찾는다.
-- 시드에서 crypt()/gen_salt() 를 쓰므로 public 을 다시 경로에 넣는다.
SET search_path TO public;

-- 최초 관리자 계정. pgcrypto 의 crypt() 로 BCrypt 해시를 만든다(위 CREATE EXTENSION 필요).
--   로그인: admin / admin1234  (배포 후 반드시 비밀번호를 바꿀 것)
INSERT INTO public.auth_user (username, password, role)
VALUES ('admin', crypt('admin1234', gen_salt('bf', 10)), 'ROLE_ADMIN');

-- 단가 정책 초기 행(단일 행). 없으면 권장가 계산이 거부되므로 함께 넣는다.
--   메모 한글은 인코딩 안전을 위해 유니코드 이스케이프로 넣는다(U&'...').
--   원문: 2026년 최저임금(시급 10,320원) 기준 초기 단가
INSERT INTO public.pricing_policy (
    base_fee, per_floor, per_household, per_toilet, elevator_fee,
    coef_base, coef_exponent, rounding_unit, memo, created_at, updated_at
) VALUES (
    20000, 6000, 1500, 15000, 5000,
    0.6224, 0.6949, 1000,
    U&'2026\B144 \CD5C\C800\C784\AE08(\C2DC\AE09 10,320\C6D0) \AE30\C900 \CD08\AE30 \B2E8\AC00',
    NOW(), NOW()
);
