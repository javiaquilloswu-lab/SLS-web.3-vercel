--
-- PostgreSQL database dump
--

\restrict sbxfIYdjLtMzSqghylPHtJyBZSr4n78wMZjL9AHzyvS3DloJTaKji3FNwe3ziYV

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.4

-- Started on 2026-10-11 10:40:05

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 240 (class 1259 OID 57407)
-- Name: admin_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.admin_tokens (
    id integer NOT NULL,
    admin_id integer NOT NULL,
    token character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp with time zone DEFAULT (CURRENT_TIMESTAMP + '7 days'::interval)
);


ALTER TABLE public.admin_tokens OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 57406)
-- Name: admin_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.admin_tokens_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.admin_tokens_id_seq OWNER TO postgres;

--
-- TOC entry 5200 (class 0 OID 0)
-- Dependencies: 239
-- Name: admin_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.admin_tokens_id_seq OWNED BY public.admin_tokens.id;


--
-- TOC entry 238 (class 1259 OID 57385)
-- Name: admin_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.admin_users (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(150) NOT NULL,
    password character varying(255) NOT NULL,
    full_name character varying(150) NOT NULL,
    department character varying(100) DEFAULT 'Student Life Office'::character varying,
    role character varying(50) DEFAULT 'SUPER_ADMIN'::character varying,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.admin_users OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 57384)
-- Name: admin_users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.admin_users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.admin_users_id_seq OWNER TO postgres;

--
-- TOC entry 5201 (class 0 OID 0)
-- Dependencies: 237
-- Name: admin_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.admin_users_id_seq OWNED BY public.admin_users.id;


--
-- TOC entry 226 (class 1259 OID 49220)
-- Name: scholarship_applications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scholarship_applications (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    program_name character varying(255) NOT NULL,
    academic_year character varying(20),
    application_type character varying(100),
    status character varying(50) DEFAULT 'Pending'::character varying NOT NULL,
    tracking_ref character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reviewed_by character varying(100),
    reviewed_at timestamp with time zone,
    admin_notes text
);


ALTER TABLE public.scholarship_applications OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 49219)
-- Name: scholarship_applications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.scholarship_applications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.scholarship_applications_id_seq OWNER TO postgres;

--
-- TOC entry 5202 (class 0 OID 0)
-- Dependencies: 225
-- Name: scholarship_applications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.scholarship_applications_id_seq OWNED BY public.scholarship_applications.id;


--
-- TOC entry 228 (class 1259 OID 49242)
-- Name: scholarship_requirements; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scholarship_requirements (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    status character varying(30) DEFAULT 'UPLOAD'::character varying NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.scholarship_requirements OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 49241)
-- Name: scholarship_requirements_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.scholarship_requirements_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.scholarship_requirements_id_seq OWNER TO postgres;

--
-- TOC entry 5203 (class 0 OID 0)
-- Dependencies: 227
-- Name: scholarship_requirements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.scholarship_requirements_id_seq OWNED BY public.scholarship_requirements.id;


--
-- TOC entry 236 (class 1259 OID 57366)
-- Name: scholarships; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scholarships (
    id integer NOT NULL,
    code character varying(50) NOT NULL,
    title character varying(150) NOT NULL,
    description text,
    academic_year character varying(50) NOT NULL,
    deadline character varying(50),
    stipend_amount character varying(50) DEFAULT '₱30,000.00'::character varying,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    requirements text
);


ALTER TABLE public.scholarships OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 57365)
-- Name: scholarships_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.scholarships_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.scholarships_id_seq OWNER TO postgres;

--
-- TOC entry 5204 (class 0 OID 0)
-- Dependencies: 235
-- Name: scholarships_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.scholarships_id_seq OWNED BY public.scholarships.id;


--
-- TOC entry 244 (class 1259 OID 65560)
-- Name: service_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.service_types (
    id integer NOT NULL,
    service_code character varying(50) NOT NULL,
    service_name character varying(150) NOT NULL,
    description text,
    department character varying(100) DEFAULT 'University Registrar'::character varying,
    category character varying(50) DEFAULT 'Document'::character varying,
    processing_days character varying(50) DEFAULT '2-3 Business Days'::character varying,
    fee_amount character varying(50) DEFAULT 'Free / Digital'::character varying,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    requirements text
);


ALTER TABLE public.service_types OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 65559)
-- Name: service_types_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.service_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.service_types_id_seq OWNER TO postgres;

--
-- TOC entry 5205 (class 0 OID 0)
-- Dependencies: 243
-- Name: service_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.service_types_id_seq OWNED BY public.service_types.id;


--
-- TOC entry 242 (class 1259 OID 65538)
-- Name: student_concern_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_concern_messages (
    id integer NOT NULL,
    concern_id integer,
    ticket_ref character varying(50),
    sender_type character varying(20) NOT NULL,
    sender_id character varying(50),
    sender_name character varying(100) NOT NULL,
    message text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    student_id character varying(50)
);


ALTER TABLE public.student_concern_messages OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 65537)
-- Name: student_concern_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_concern_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_concern_messages_id_seq OWNER TO postgres;

--
-- TOC entry 5206 (class 0 OID 0)
-- Dependencies: 241
-- Name: student_concern_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_concern_messages_id_seq OWNED BY public.student_concern_messages.id;


--
-- TOC entry 232 (class 1259 OID 49283)
-- Name: student_concerns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_concerns (
    id integer NOT NULL,
    student_id character varying(50),
    ticket_ref character varying(100),
    message text NOT NULL,
    category character varying(100),
    response text,
    status character varying(50) DEFAULT 'Open'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    internal_notes text,
    assigned_to character varying(100),
    escalation_reason text,
    is_ai_paused boolean DEFAULT false,
    updated_at timestamp with time zone,
    responded_by character varying(100),
    responded_at timestamp with time zone
);


ALTER TABLE public.student_concerns OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 49282)
-- Name: student_concerns_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_concerns_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_concerns_id_seq OWNER TO postgres;

--
-- TOC entry 5207 (class 0 OID 0)
-- Dependencies: 231
-- Name: student_concerns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_concerns_id_seq OWNED BY public.student_concerns.id;


--
-- TOC entry 222 (class 1259 OID 49173)
-- Name: student_documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_documents (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    document_type character varying(100) NOT NULL,
    original_filename character varying(255) NOT NULL,
    stored_filename character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    mime_type character varying(50) NOT NULL,
    file_size_bytes integer NOT NULL,
    status character varying(30) DEFAULT 'Pending Review'::character varying,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reviewed_by character varying(100),
    reviewed_at timestamp with time zone,
    admin_notes text,
    request_id integer,
    is_current boolean DEFAULT true
);


ALTER TABLE public.student_documents OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 49172)
-- Name: student_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_documents_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_documents_id_seq OWNER TO postgres;

--
-- TOC entry 5208 (class 0 OID 0)
-- Dependencies: 221
-- Name: student_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_documents_id_seq OWNED BY public.student_documents.id;


--
-- TOC entry 230 (class 1259 OID 49262)
-- Name: student_notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_notifications (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    body text NOT NULL,
    category character varying(100),
    status_tag character varying(50),
    is_unread boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.student_notifications OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 49261)
-- Name: student_notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_notifications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_notifications_id_seq OWNER TO postgres;

--
-- TOC entry 5209 (class 0 OID 0)
-- Dependencies: 229
-- Name: student_notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_notifications_id_seq OWNED BY public.student_notifications.id;


--
-- TOC entry 224 (class 1259 OID 49197)
-- Name: student_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_requests (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    department character varying(150),
    reference_number character varying(100) NOT NULL,
    status character varying(50) DEFAULT 'Pending'::character varying NOT NULL,
    status_message text,
    category character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    assigned_to character varying(100),
    updated_at timestamp with time zone
);


ALTER TABLE public.student_requests OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 49196)
-- Name: student_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_requests_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_requests_id_seq OWNER TO postgres;

--
-- TOC entry 5210 (class 0 OID 0)
-- Dependencies: 223
-- Name: student_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_requests_id_seq OWNED BY public.student_requests.id;


--
-- TOC entry 234 (class 1259 OID 57345)
-- Name: student_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_tokens (
    id integer NOT NULL,
    student_id character varying(50) NOT NULL,
    token character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp with time zone DEFAULT (CURRENT_TIMESTAMP + '30 days'::interval)
);


ALTER TABLE public.student_tokens OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 57344)
-- Name: student_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_tokens_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_tokens_id_seq OWNER TO postgres;

--
-- TOC entry 5211 (class 0 OID 0)
-- Dependencies: 233
-- Name: student_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_tokens_id_seq OWNED BY public.student_tokens.id;


--
-- TOC entry 220 (class 1259 OID 49153)
-- Name: students; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.students (
    id integer NOT NULL,
    first_name character varying(100) NOT NULL,
    middle_name character varying(100),
    last_name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    student_id character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    course character varying(100) DEFAULT 'BS Computer Science'::character varying,
    year_level character varying(50) DEFAULT '3rd Year'::character varying,
    academic_status character varying(50) DEFAULT 'Active'::character varying,
    requirements_progress integer DEFAULT 67,
    verified_docs_count integer DEFAULT 4,
    total_docs_count integer DEFAULT 6,
    pending_actions_count integer DEFAULT 2
);


ALTER TABLE public.students OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 49152)
-- Name: students_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.students_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.students_id_seq OWNER TO postgres;

--
-- TOC entry 5212 (class 0 OID 0)
-- Dependencies: 219
-- Name: students_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.students_id_seq OWNED BY public.students.id;


--
-- TOC entry 4957 (class 2604 OID 57410)
-- Name: admin_tokens id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_tokens ALTER COLUMN id SET DEFAULT nextval('public.admin_tokens_id_seq'::regclass);


--
-- TOC entry 4952 (class 2604 OID 57388)
-- Name: admin_users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_users ALTER COLUMN id SET DEFAULT nextval('public.admin_users_id_seq'::regclass);


--
-- TOC entry 4932 (class 2604 OID 49223)
-- Name: scholarship_applications id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_applications ALTER COLUMN id SET DEFAULT nextval('public.scholarship_applications_id_seq'::regclass);


--
-- TOC entry 4935 (class 2604 OID 49245)
-- Name: scholarship_requirements id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_requirements ALTER COLUMN id SET DEFAULT nextval('public.scholarship_requirements_id_seq'::regclass);


--
-- TOC entry 4948 (class 2604 OID 57369)
-- Name: scholarships id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarships ALTER COLUMN id SET DEFAULT nextval('public.scholarships_id_seq'::regclass);


--
-- TOC entry 4962 (class 2604 OID 65563)
-- Name: service_types id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_types ALTER COLUMN id SET DEFAULT nextval('public.service_types_id_seq'::regclass);


--
-- TOC entry 4960 (class 2604 OID 65541)
-- Name: student_concern_messages id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concern_messages ALTER COLUMN id SET DEFAULT nextval('public.student_concern_messages_id_seq'::regclass);


--
-- TOC entry 4941 (class 2604 OID 49286)
-- Name: student_concerns id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concerns ALTER COLUMN id SET DEFAULT nextval('public.student_concerns_id_seq'::regclass);


--
-- TOC entry 4925 (class 2604 OID 49176)
-- Name: student_documents id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_documents ALTER COLUMN id SET DEFAULT nextval('public.student_documents_id_seq'::regclass);


--
-- TOC entry 4938 (class 2604 OID 49265)
-- Name: student_notifications id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_notifications ALTER COLUMN id SET DEFAULT nextval('public.student_notifications_id_seq'::regclass);


--
-- TOC entry 4929 (class 2604 OID 49200)
-- Name: student_requests id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_requests ALTER COLUMN id SET DEFAULT nextval('public.student_requests_id_seq'::regclass);


--
-- TOC entry 4945 (class 2604 OID 57348)
-- Name: student_tokens id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_tokens ALTER COLUMN id SET DEFAULT nextval('public.student_tokens_id_seq'::regclass);


--
-- TOC entry 4916 (class 2604 OID 49156)
-- Name: students id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students ALTER COLUMN id SET DEFAULT nextval('public.students_id_seq'::regclass);


--
-- TOC entry 5028 (class 2606 OID 57417)
-- Name: admin_tokens admin_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_tokens
    ADD CONSTRAINT admin_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5030 (class 2606 OID 57419)
-- Name: admin_tokens admin_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_tokens
    ADD CONSTRAINT admin_tokens_token_key UNIQUE (token);


--
-- TOC entry 5022 (class 2606 OID 57405)
-- Name: admin_users admin_users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_email_key UNIQUE (email);


--
-- TOC entry 5024 (class 2606 OID 57401)
-- Name: admin_users admin_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_pkey PRIMARY KEY (id);


--
-- TOC entry 5026 (class 2606 OID 57403)
-- Name: admin_users admin_users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_username_key UNIQUE (username);


--
-- TOC entry 4993 (class 2606 OID 49233)
-- Name: scholarship_applications scholarship_applications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_applications
    ADD CONSTRAINT scholarship_applications_pkey PRIMARY KEY (id);


--
-- TOC entry 4995 (class 2606 OID 49235)
-- Name: scholarship_applications scholarship_applications_tracking_ref_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_applications
    ADD CONSTRAINT scholarship_applications_tracking_ref_key UNIQUE (tracking_ref);


--
-- TOC entry 4999 (class 2606 OID 49255)
-- Name: scholarship_requirements scholarship_requirements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_requirements
    ADD CONSTRAINT scholarship_requirements_pkey PRIMARY KEY (id);


--
-- TOC entry 5018 (class 2606 OID 57382)
-- Name: scholarships scholarships_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarships
    ADD CONSTRAINT scholarships_code_key UNIQUE (code);


--
-- TOC entry 5020 (class 2606 OID 57380)
-- Name: scholarships scholarships_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarships
    ADD CONSTRAINT scholarships_pkey PRIMARY KEY (id);


--
-- TOC entry 5035 (class 2606 OID 65576)
-- Name: service_types service_types_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_types
    ADD CONSTRAINT service_types_pkey PRIMARY KEY (id);


--
-- TOC entry 5037 (class 2606 OID 65578)
-- Name: service_types service_types_service_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_types
    ADD CONSTRAINT service_types_service_code_key UNIQUE (service_code);


--
-- TOC entry 5033 (class 2606 OID 65552)
-- Name: student_concern_messages student_concern_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concern_messages
    ADD CONSTRAINT student_concern_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 5007 (class 2606 OID 49295)
-- Name: student_concerns student_concerns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concerns
    ADD CONSTRAINT student_concerns_pkey PRIMARY KEY (id);


--
-- TOC entry 5009 (class 2606 OID 49297)
-- Name: student_concerns student_concerns_ticket_ref_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concerns
    ADD CONSTRAINT student_concerns_ticket_ref_key UNIQUE (ticket_ref);


--
-- TOC entry 4982 (class 2606 OID 49190)
-- Name: student_documents student_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_documents
    ADD CONSTRAINT student_documents_pkey PRIMARY KEY (id);


--
-- TOC entry 5003 (class 2606 OID 49276)
-- Name: student_notifications student_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_notifications
    ADD CONSTRAINT student_notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 4987 (class 2606 OID 49211)
-- Name: student_requests student_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_requests
    ADD CONSTRAINT student_requests_pkey PRIMARY KEY (id);


--
-- TOC entry 4989 (class 2606 OID 49213)
-- Name: student_requests student_requests_reference_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_requests
    ADD CONSTRAINT student_requests_reference_number_key UNIQUE (reference_number);


--
-- TOC entry 5013 (class 2606 OID 57355)
-- Name: student_tokens student_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_tokens
    ADD CONSTRAINT student_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5015 (class 2606 OID 57357)
-- Name: student_tokens student_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_tokens
    ADD CONSTRAINT student_tokens_token_key UNIQUE (token);


--
-- TOC entry 4972 (class 2606 OID 49169)
-- Name: students students_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_email_key UNIQUE (email);


--
-- TOC entry 4974 (class 2606 OID 49167)
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- TOC entry 4976 (class 2606 OID 49171)
-- Name: students students_student_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_student_id_key UNIQUE (student_id);


--
-- TOC entry 5031 (class 1259 OID 65558)
-- Name: idx_concern_msgs_ticket; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_concern_msgs_ticket ON public.student_concern_messages USING btree (ticket_ref);


--
-- TOC entry 5004 (class 1259 OID 49324)
-- Name: idx_concerns_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_concerns_student_id ON public.student_concerns USING btree (student_id);


--
-- TOC entry 4977 (class 1259 OID 73735)
-- Name: idx_documents_is_current; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_documents_is_current ON public.student_documents USING btree (is_current);


--
-- TOC entry 4978 (class 1259 OID 73733)
-- Name: idx_documents_request_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_documents_request_id ON public.student_documents USING btree (request_id);


--
-- TOC entry 4979 (class 1259 OID 49318)
-- Name: idx_documents_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_documents_student_id ON public.student_documents USING btree (student_id);


--
-- TOC entry 5000 (class 1259 OID 49323)
-- Name: idx_notifications_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_student_id ON public.student_notifications USING btree (student_id);


--
-- TOC entry 4983 (class 1259 OID 49320)
-- Name: idx_requests_ref_num; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_requests_ref_num ON public.student_requests USING btree (reference_number);


--
-- TOC entry 4984 (class 1259 OID 49319)
-- Name: idx_requests_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_requests_student_id ON public.student_requests USING btree (student_id);


--
-- TOC entry 4990 (class 1259 OID 49305)
-- Name: idx_scholarship_applications_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scholarship_applications_student_id ON public.scholarship_applications USING btree (student_id);


--
-- TOC entry 4991 (class 1259 OID 49321)
-- Name: idx_scholarship_apps_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scholarship_apps_student_id ON public.scholarship_applications USING btree (student_id);


--
-- TOC entry 4996 (class 1259 OID 49322)
-- Name: idx_scholarship_reqs_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scholarship_reqs_student_id ON public.scholarship_requirements USING btree (student_id);


--
-- TOC entry 4997 (class 1259 OID 49306)
-- Name: idx_scholarship_requirements_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scholarship_requirements_student_id ON public.scholarship_requirements USING btree (student_id);


--
-- TOC entry 5016 (class 1259 OID 57383)
-- Name: idx_scholarships_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scholarships_code ON public.scholarships USING btree (code);


--
-- TOC entry 5005 (class 1259 OID 49308)
-- Name: idx_student_concerns_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_concerns_student_id ON public.student_concerns USING btree (student_id);


--
-- TOC entry 4980 (class 1259 OID 49303)
-- Name: idx_student_documents_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_documents_student_id ON public.student_documents USING btree (student_id);


--
-- TOC entry 5001 (class 1259 OID 49307)
-- Name: idx_student_notifications_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_notifications_student_id ON public.student_notifications USING btree (student_id);


--
-- TOC entry 4985 (class 1259 OID 49304)
-- Name: idx_student_requests_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_requests_student_id ON public.student_requests USING btree (student_id);


--
-- TOC entry 5010 (class 1259 OID 57364)
-- Name: idx_student_tokens_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_tokens_student_id ON public.student_tokens USING btree (student_id);


--
-- TOC entry 5011 (class 1259 OID 57363)
-- Name: idx_student_tokens_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_tokens_token ON public.student_tokens USING btree (token);


--
-- TOC entry 4969 (class 1259 OID 49317)
-- Name: idx_students_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_students_email ON public.students USING btree (email);


--
-- TOC entry 4970 (class 1259 OID 49316)
-- Name: idx_students_student_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_students_student_id ON public.students USING btree (student_id);


--
-- TOC entry 5046 (class 2606 OID 57420)
-- Name: admin_tokens admin_tokens_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin_tokens
    ADD CONSTRAINT admin_tokens_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES public.admin_users(id) ON DELETE CASCADE;


--
-- TOC entry 5041 (class 2606 OID 49236)
-- Name: scholarship_applications fk_scholarship_applications_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_applications
    ADD CONSTRAINT fk_scholarship_applications_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5042 (class 2606 OID 49256)
-- Name: scholarship_requirements fk_scholarship_requirements_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scholarship_requirements
    ADD CONSTRAINT fk_scholarship_requirements_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5044 (class 2606 OID 49298)
-- Name: student_concerns fk_student_concerns_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concerns
    ADD CONSTRAINT fk_student_concerns_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5038 (class 2606 OID 49191)
-- Name: student_documents fk_student_documents_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_documents
    ADD CONSTRAINT fk_student_documents_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5043 (class 2606 OID 49277)
-- Name: student_notifications fk_student_notifications_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_notifications
    ADD CONSTRAINT fk_student_notifications_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5040 (class 2606 OID 49214)
-- Name: student_requests fk_student_requests_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_requests
    ADD CONSTRAINT fk_student_requests_student FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 5047 (class 2606 OID 65553)
-- Name: student_concern_messages student_concern_messages_concern_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_concern_messages
    ADD CONSTRAINT student_concern_messages_concern_id_fkey FOREIGN KEY (concern_id) REFERENCES public.student_concerns(id) ON DELETE CASCADE;


--
-- TOC entry 5039 (class 2606 OID 73728)
-- Name: student_documents student_documents_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_documents
    ADD CONSTRAINT student_documents_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.student_requests(id) ON DELETE SET NULL;


--
-- TOC entry 5045 (class 2606 OID 57358)
-- Name: student_tokens student_tokens_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_tokens
    ADD CONSTRAINT student_tokens_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(student_id) ON DELETE CASCADE;


-- Completed on 2026-10-11 10:40:05

--
-- PostgreSQL database dump complete
--

\unrestrict sbxfIYdjLtMzSqghylPHtJyBZSr4n78wMZjL9AHzyvS3DloJTaKji3FNwe3ziYV
