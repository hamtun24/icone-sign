--
-- PostgreSQL database dump
--

\restrict xnbtQ5Gq6Le6EPdgfBdevZgod6ZlcH3Pu8m7VTmvDnNtoUrErcpaNmXPqqqEhbP

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: invoice_processing_records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invoice_processing_records (
    id bigint NOT NULL,
    current_stage character varying(255),
    end_time timestamp(6) without time zone,
    error_message character varying(255),
    error_stage character varying(255),
    filename character varying(255) NOT NULL,
    html_preview_path character varying(255),
    matricule_fiscal character varying(255) NOT NULL,
    original_file_size bigint,
    processed_file_size bigint,
    save_completed boolean NOT NULL,
    sign_completed boolean NOT NULL,
    signed_xml_path character varying(255),
    start_time timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    transform_completed boolean NOT NULL,
    ttn_invoice_id character varying(255),
    username character varying(255) NOT NULL,
    validate_completed boolean NOT NULL,
    validation_report_path character varying(255)
);


ALTER TABLE public.invoice_processing_records OWNER TO postgres;

--
-- Name: invoice_processing_records_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.invoice_processing_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.invoice_processing_records_id_seq OWNER TO postgres;

--
-- Name: invoice_processing_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.invoice_processing_records_id_seq OWNED BY public.invoice_processing_records.id;


--
-- Name: operation_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.operation_logs (
    id bigint NOT NULL,
    user_id bigint,
    session_id character varying(255),
    operation_type character varying(50) NOT NULL,
    operation_status character varying(20) NOT NULL,
    file_size bigint,
    filename character varying(255),
    details text,
    error_message text,
    processing_time_ms bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT operation_logs_operation_status_check CHECK (((operation_status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILURE'::character varying, 'WARNING'::character varying])::text[])))
);


ALTER TABLE public.operation_logs OWNER TO postgres;

--
-- Name: TABLE operation_logs; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.operation_logs IS 'Audit trail for all operations performed';


--
-- Name: operation_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.operation_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.operation_logs_id_seq OWNER TO postgres;

--
-- Name: operation_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.operation_logs_id_seq OWNED BY public.operation_logs.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    company_name character varying(255),
    ttn_username text,
    ttn_password text,
    ttn_matricule_fiscal text,
    ance_seal_pin text,
    ance_seal_alias text,
    certificate_path character varying(500),
    is_active boolean DEFAULT true,
    is_verified boolean DEFAULT false,
    role character varying(20) DEFAULT 'USER'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_login timestamp without time zone,
    CONSTRAINT users_email_format CHECK (((email)::text ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'::text)),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT users_username_length CHECK ((length((username)::text) >= 3))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.users IS 'User accounts with encrypted credentials for TTN and ANCE SEAL';


--
-- Name: COLUMN users.ttn_username; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.ttn_username IS 'Encrypted TTN username';


--
-- Name: COLUMN users.ttn_password; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.ttn_password IS 'Encrypted TTN password';


--
-- Name: COLUMN users.ance_seal_pin; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.ance_seal_pin IS 'Encrypted ANCE SEAL PIN';


--
-- Name: workflow_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_sessions (
    id bigint NOT NULL,
    session_id character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    status character varying(20) DEFAULT 'INITIALIZING'::character varying,
    current_stage character varying(20) DEFAULT 'SIGN'::character varying,
    overall_progress integer DEFAULT 0,
    total_files integer DEFAULT 0,
    successful_files integer DEFAULT 0,
    failed_files integer DEFAULT 0,
    message text,
    error_message text,
    zip_download_url character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamp without time zone,
    CONSTRAINT workflow_sessions_current_stage_check CHECK (((current_stage)::text = ANY ((ARRAY['SIGN'::character varying, 'SAVE'::character varying, 'VALIDATE'::character varying, 'TRANSFORM'::character varying, 'PACKAGE'::character varying])::text[]))),
    CONSTRAINT workflow_sessions_overall_progress_check CHECK (((overall_progress >= 0) AND (overall_progress <= 100))),
    CONSTRAINT workflow_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['INITIALIZING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.workflow_sessions OWNER TO postgres;

--
-- Name: TABLE workflow_sessions; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.workflow_sessions IS 'Workflow processing sessions tracking overall progress';


--
-- Name: COLUMN workflow_sessions.session_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.workflow_sessions.session_id IS 'Unique session identifier for API tracking';


--
-- Name: session_summary; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.session_summary AS
 SELECT ws.session_id,
    ws.status,
    ws.current_stage,
    ws.overall_progress,
    ws.total_files,
    ws.successful_files,
    ws.failed_files,
    ws.created_at,
    ws.completed_at,
    u.username,
    u.company_name,
    EXTRACT(epoch FROM (COALESCE((ws.completed_at)::timestamp with time zone, CURRENT_TIMESTAMP) - (ws.created_at)::timestamp with time zone)) AS duration_seconds
   FROM (public.workflow_sessions ws
     JOIN public.users u ON ((ws.user_id = u.id)));


ALTER VIEW public.session_summary OWNER TO postgres;

--
-- Name: user_stats; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.user_stats AS
 SELECT u.id,
    u.username,
    u.email,
    u.company_name,
    u.created_at,
    u.last_login,
    count(ws.id) AS total_sessions,
    count(
        CASE
            WHEN ((ws.status)::text = 'COMPLETED'::text) THEN 1
            ELSE NULL::integer
        END) AS completed_sessions,
    count(
        CASE
            WHEN ((ws.status)::text = 'FAILED'::text) THEN 1
            ELSE NULL::integer
        END) AS failed_sessions,
    COALESCE(sum(ws.total_files), (0)::bigint) AS total_files_processed
   FROM (public.users u
     LEFT JOIN public.workflow_sessions ws ON ((u.id = ws.user_id)))
  GROUP BY u.id, u.username, u.email, u.company_name, u.created_at, u.last_login;


ALTER VIEW public.user_stats OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: workflow_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_files (
    id bigint NOT NULL,
    workflow_session_id bigint NOT NULL,
    filename character varying(255) NOT NULL,
    file_size bigint,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    stage character varying(20) DEFAULT 'SIGN'::character varying,
    progress integer DEFAULT 0,
    error_message text,
    ttn_invoice_id character varying(100),
    signed_xml_path character varying(500),
    validation_report_path character varying(500),
    html_report_path character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamp without time zone,
    CONSTRAINT workflow_files_progress_check CHECK (((progress >= 0) AND (progress <= 100))),
    CONSTRAINT workflow_files_stage_check CHECK (((stage)::text = ANY ((ARRAY['SIGN'::character varying, 'SAVE'::character varying, 'VALIDATE'::character varying, 'TRANSFORM'::character varying, 'PACKAGE'::character varying])::text[]))),
    CONSTRAINT workflow_files_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.workflow_files OWNER TO postgres;

--
-- Name: TABLE workflow_files; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.workflow_files IS 'Individual files within workflow sessions';


--
-- Name: COLUMN workflow_files.ttn_invoice_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.workflow_files.ttn_invoice_id IS 'TTN invoice ID extracted from SOAP response';


--
-- Name: workflow_files_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.workflow_files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.workflow_files_id_seq OWNER TO postgres;

--
-- Name: workflow_files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.workflow_files_id_seq OWNED BY public.workflow_files.id;


--
-- Name: workflow_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.workflow_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.workflow_sessions_id_seq OWNER TO postgres;

--
-- Name: workflow_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.workflow_sessions_id_seq OWNED BY public.workflow_sessions.id;


--
-- Name: invoice_processing_records id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice_processing_records ALTER COLUMN id SET DEFAULT nextval('public.invoice_processing_records_id_seq'::regclass);


--
-- Name: operation_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_logs ALTER COLUMN id SET DEFAULT nextval('public.operation_logs_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: workflow_files id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_files ALTER COLUMN id SET DEFAULT nextval('public.workflow_files_id_seq'::regclass);


--
-- Name: workflow_sessions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_sessions ALTER COLUMN id SET DEFAULT nextval('public.workflow_sessions_id_seq'::regclass);


--
-- Data for Name: invoice_processing_records; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.invoice_processing_records (id, current_stage, end_time, error_message, error_stage, filename, html_preview_path, matricule_fiscal, original_file_size, processed_file_size, save_completed, sign_completed, signed_xml_path, start_time, status, transform_completed, ttn_invoice_id, username, validate_completed, validation_report_path) FROM stdin;
\.


--
-- Data for Name: operation_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.operation_logs (id, user_id, session_id, operation_type, operation_status, file_size, filename, details, error_message, processing_time_ms, created_at) FROM stdin;
1	\N	\N	ANCE_SIGN	SUCCESS	2989	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:40:26.231976
2	\N	\N	TTN_SAVE	SUCCESS	8806	2025000002 (5).xml	File processed successfully	\N	\N	2025-08-26 19:40:26.729905
3	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:40:36.206138
4	\N	\N	TTN_TRANSFORM	FAILURE	8806	2025000002 (5).xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Document non valide: Compte inexistant","errorCode":"SERV01"}"	\N	2025-08-26 19:40:36.666976
6	\N	\N	ANCE_SIGN	SUCCESS	2989	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:47:59.045695
7	\N	\N	TTN_SAVE	SUCCESS	8806	2025000002 (5).xml	File processed successfully	\N	\N	2025-08-26 19:47:59.79593
8	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:48:08.297244
9	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025000002 (5).xml	XML to HTML transformation completed	\N	\N	2025-08-26 19:48:08.866221
10	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 19:48:08.91881
11	\N	\N	ANCE_SIGN	SUCCESS	2989	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:59:21.854139
12	\N	\N	TTN_SAVE	SUCCESS	8806	2025000002 (5).xml	File processed successfully	\N	\N	2025-08-26 19:59:22.55666
13	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025000002 (5).xml	\N	\N	\N	2025-08-26 19:59:30.967064
14	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025000002 (5).xml	XML to HTML transformation completed	\N	\N	2025-08-26 19:59:31.535344
15	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 19:59:31.564888
16	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:08:49.856772
18	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:10:51.691786
19	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:10:51.753457
20	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:11:18.090415
21	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:11:18.121254
22	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:12:15.216696
23	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:12:15.243662
24	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:13:54.081841
25	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:13:54.220709
26	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	Invalid file. Only XML files are allowed.	\N	2025-08-26 20:14:50.583353
27	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:14:50.6068
28	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025000002 (5).xml	Workflow processing failed at stage: null	File is empty.	\N	2025-08-26 20:16:50.840852
29	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:16:51.022919
30	\N	\N	WORKFLOW_PROCESS	FAILURE	0	2025.xml	Workflow processing failed at stage: null	File is empty.	\N	2025-08-26 20:17:38.259021
31	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Workflow completed: 0 successful, 1 failed. ZIP created.	\N	\N	2025-08-26 20:17:38.288738
32	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-26 20:24:57.243518
33	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-26 20:24:58.01043
34	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-26 20:25:07.505193
35	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-26 20:25:08.020359
36	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 20:25:08.061623
37	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-26 20:29:30.185249
38	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-26 20:29:30.925237
39	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-26 20:29:39.351899
40	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-26 20:29:39.878352
41	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 20:29:39.896675
42	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-26 20:30:50.359744
43	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-26 20:30:50.99995
44	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-26 20:30:59.392298
45	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-26 20:30:59.923394
46	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 20:30:59.949293
47	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-26 20:33:58.636542
48	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-26 20:33:59.328949
49	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-26 20:34:07.708149
50	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-26 20:34:08.283936
51	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-26 20:34:08.312566
52	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-27 10:14:42.780265
53	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-27 10:14:43.83502
54	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-27 10:14:53.456046
55	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-27 10:14:54.052729
56	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-27 10:14:54.101888
57	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-27 15:17:39.633941
58	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-27 15:17:41.207765
59	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-27 15:17:50.415023
60	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-27 15:17:51.039628
61	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-27 15:17:51.11441
62	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:18:29.638852
63	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:19:24.452642
64	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-27 15:22:17.499134
65	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-27 15:22:18.831889
66	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-27 15:22:28.467711
67	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-27 15:22:29.052759
68	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-27 15:22:29.079676
69	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:23:30.353554
70	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:23:46.74278
71	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:23:56.163142
72	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-27 15:24:15.253322
73	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-29 13:53:14.326264
74	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-29 13:53:15.744097
75	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-29 13:53:24.378582
76	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-29 13:53:24.964104
77	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 13:53:25.046389
78	\N	\N	ANCE_SIGN	SUCCESS	2989	2025.xml	\N	\N	\N	2025-08-29 14:02:52.195048
79	\N	\N	TTN_SAVE	SUCCESS	8806	2025.xml	File processed successfully	\N	\N	2025-08-29 14:02:53.897405
80	\N	\N	ANCE_VALIDATE	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-29 14:03:02.939986
81	\N	\N	TTN_TRANSFORM	SUCCESS	8806	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-29 14:03:03.636324
82	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 14:03:03.709911
83	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 14:04:56.47543
84	\N	\N	ANCE_SIGN	SUCCESS	2989	2025-1.xml	\N	\N	\N	2025-08-29 14:22:43.694771
85	\N	\N	TTN_SAVE	SUCCESS	8816	2025-1.xml	File processed successfully	\N	\N	2025-08-29 14:22:45.140473
86	\N	\N	ANCE_VALIDATE	SUCCESS	8816	2025-1.xml	\N	\N	\N	2025-08-29 14:22:59.769414
87	\N	\N	TTN_TRANSFORM	SUCCESS	8816	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 14:23:00.464763
88	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 14:23:00.521972
89	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 14:24:00.332112
90	\N	\N	ANCE_SIGN	SUCCESS	2989	2025-1.xml	\N	\N	\N	2025-08-29 14:27:08.214413
91	\N	\N	TTN_SAVE	SUCCESS	8816	2025-1.xml	File processed successfully	\N	\N	2025-08-29 14:27:09.243361
92	\N	\N	ANCE_VALIDATE	SUCCESS	8816	2025-1.xml	\N	\N	\N	2025-08-29 14:27:23.771376
93	\N	\N	TTN_TRANSFORM	SUCCESS	8816	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 14:27:24.384645
94	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 14:27:24.431417
95	\N	\N	ANCE_SIGN	SUCCESS	2989	2025-1.xml	\N	\N	\N	2025-08-29 14:30:12.77187
96	\N	\N	TTN_SAVE	SUCCESS	8816	2025-1.xml	File processed successfully	\N	\N	2025-08-29 14:30:14.005461
97	\N	\N	ANCE_VALIDATE	SUCCESS	8816	2025-1.xml	\N	\N	\N	2025-08-29 14:30:28.600179
98	\N	\N	TTN_TRANSFORM	SUCCESS	8816	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 14:30:29.118478
99	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 14:30:29.308579
100	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 14:31:27.46483
101	\N	\N	ANCE_SIGN	SUCCESS	3049	2025-1.xml	\N	\N	\N	2025-08-29 15:04:20.469317
102	\N	\N	TTN_SAVE	SUCCESS	8872	2025-1.xml	File processed successfully	\N	\N	2025-08-29 15:04:21.789644
103	\N	\N	ANCE_VALIDATE	SUCCESS	8872	2025-1.xml	\N	\N	\N	2025-08-29 15:04:36.367412
104	\N	\N	TTN_TRANSFORM	SUCCESS	8872	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 15:04:36.798929
105	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 15:04:36.854847
106	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 15:05:22.725247
107	\N	\N	ANCE_SIGN	SUCCESS	3049	2025-1.xml	\N	\N	\N	2025-08-29 15:10:13.011183
108	\N	\N	TTN_SAVE	SUCCESS	8872	2025-1.xml	File processed successfully	\N	\N	2025-08-29 15:10:14.146221
109	\N	\N	ANCE_VALIDATE	SUCCESS	8872	2025-1.xml	\N	\N	\N	2025-08-29 15:10:28.562299
110	\N	\N	TTN_TRANSFORM	SUCCESS	8872	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 15:10:29.057386
111	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 15:10:29.088732
112	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 15:12:06.531454
113	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 15:23:06.913664
114	\N	\N	ANCE_SIGN	SUCCESS	3049	2025-1.xml	\N	\N	\N	2025-08-29 15:48:36.975945
115	\N	\N	TTN_SAVE	SUCCESS	8880	2025-1.xml	File processed successfully	\N	\N	2025-08-29 15:48:38.58246
116	\N	\N	ANCE_VALIDATE	SUCCESS	8880	2025-1.xml	\N	\N	\N	2025-08-29 15:48:53.797781
117	\N	\N	TTN_TRANSFORM	SUCCESS	8880	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 15:48:54.409173
118	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 15:48:54.706354
119	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 15:49:52.450029
120	\N	\N	ANCE_SIGN	SUCCESS	2991	2025-1.xml	\N	\N	\N	2025-08-29 15:53:19.961391
121	\N	\N	TTN_SAVE	SUCCESS	8822	2025-1.xml	File processed successfully	\N	\N	2025-08-29 15:53:21.085383
122	\N	\N	ANCE_VALIDATE	SUCCESS	8822	2025-1.xml	\N	\N	\N	2025-08-29 15:53:35.620703
123	\N	\N	TTN_TRANSFORM	SUCCESS	8822	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 15:53:36.295856
124	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 15:53:36.370928
125	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 15:53:57.945881
126	\N	\N	ANCE_SIGN	SUCCESS	2991	2025-1.xml	\N	\N	\N	2025-08-29 15:55:08.033202
127	\N	\N	TTN_SAVE	SUCCESS	8822	2025-1.xml	File processed successfully	\N	\N	2025-08-29 15:55:09.072414
128	\N	\N	ANCE_VALIDATE	SUCCESS	8822	2025-1.xml	\N	\N	\N	2025-08-29 15:55:23.557183
129	\N	\N	TTN_TRANSFORM	SUCCESS	8822	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 15:55:24.259302
130	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 15:55:24.408315
131	\N	\N	ANCE_SIGN	SUCCESS	2991	2025-1.xml	\N	\N	\N	2025-08-29 16:21:34.743798
132	\N	\N	TTN_SAVE	SUCCESS	8822	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:21:36.273109
133	\N	\N	ANCE_VALIDATE	SUCCESS	8822	2025-1.xml	\N	\N	\N	2025-08-29 16:21:51.062167
134	\N	\N	TTN_TRANSFORM	SUCCESS	8822	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:21:51.626658
135	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:21:51.702454
136	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 16:22:52.721833
137	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:34:58.084434
138	\N	\N	TTN_SAVE	SUCCESS	8822	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:34:59.223913
139	\N	\N	ANCE_VALIDATE	SUCCESS	8822	2025-1.xml	\N	\N	\N	2025-08-29 16:35:13.729452
140	\N	\N	TTN_TRANSFORM	SUCCESS	8822	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:35:14.24835
141	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:35:14.27821
142	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:39:46.271442
143	\N	\N	TTN_SAVE	SUCCESS	8812	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:39:47.320621
144	\N	\N	ANCE_VALIDATE	SUCCESS	8812	2025-1.xml	\N	\N	\N	2025-08-29 16:39:55.824164
145	\N	\N	TTN_TRANSFORM	SUCCESS	8812	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:39:56.320337
146	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:39:56.338966
147	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 16:40:44.288924
148	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:53:04.429087
149	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:53:05.672088
150	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-29 16:53:15.257918
151	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:53:15.802414
152	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:53:15.832893
153	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 16:53:32.489766
154	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:56:22.897878
155	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:56:22.896861
156	\N	\N	TTN_SAVE	SUCCESS	8784	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:56:24.126633
157	\N	\N	TTN_SAVE	SUCCESS	8784	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:56:24.126633
158	\N	\N	ANCE_VALIDATE	SUCCESS	8784	2025-1.xml	\N	\N	\N	2025-08-29 16:56:32.595486
159	\N	\N	ANCE_VALIDATE	SUCCESS	8784	2025-1.xml	\N	\N	\N	2025-08-29 16:56:32.596484
160	\N	\N	TTN_TRANSFORM	SUCCESS	8784	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:56:33.153878
161	\N	\N	TTN_TRANSFORM	SUCCESS	8784	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:56:33.163862
162	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:56:33.229041
163	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 16:56:35.546829
164	\N	\N	TTN_SAVE	SUCCESS	8784	2025-1.xml	File processed successfully	\N	\N	2025-08-29 16:56:36.566902
165	\N	\N	ANCE_VALIDATE	SUCCESS	8784	2025-1.xml	\N	\N	\N	2025-08-29 16:56:36.935192
166	\N	\N	TTN_TRANSFORM	SUCCESS	8784	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 16:56:37.352903
167	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 2 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 16:56:37.400776
168	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 16:57:12.708698
169	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 17:04:02.825984
170	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 17:04:02.825001
171	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-29 17:04:04.080772
172	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-29 17:04:04.088744
173	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-29 17:04:12.541115
174	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-29 17:04:12.541115
175	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 17:04:13.069903
176	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 17:04:13.117628
177	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 17:04:13.14582
178	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 17:04:16.536096
179	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-29 17:04:17.5107
180	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-29 17:04:17.908822
181	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 17:04:18.387376
182	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 2 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 17:04:18.428892
183	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 17:04:33.868093
184	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-29 17:07:07.612973
185	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-29 17:07:08.958045
186	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-29 17:07:17.390776
187	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-29 17:07:17.907229
188	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-29 17:07:17.955914
189	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-29 17:07:34.459843
190	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 11:59:10.288644
191	\N	\N	TTN_SAVE	SUCCESS	8801	2025-1.xml	File processed successfully	\N	\N	2025-08-30 11:59:11.629899
192	\N	\N	ANCE_VALIDATE	SUCCESS	8801	2025-1.xml	\N	\N	\N	2025-08-30 11:59:21.430749
193	\N	\N	TTN_TRANSFORM	SUCCESS	8801	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 11:59:22.016452
194	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 11:59:22.083795
195	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:14:32.346455
196	\N	\N	TTN_SAVE	SUCCESS	8820	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:14:33.578981
197	\N	\N	ANCE_VALIDATE	SUCCESS	8820	2025-1.xml	\N	\N	\N	2025-08-30 12:14:43.141399
198	\N	\N	TTN_TRANSFORM	SUCCESS	8820	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:14:43.655795
199	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:14:43.701978
200	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:19:03.473976
201	\N	\N	TTN_SAVE	SUCCESS	8824	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:19:04.764139
202	\N	\N	ANCE_VALIDATE	SUCCESS	8824	2025-1.xml	\N	\N	\N	2025-08-30 12:19:13.303067
471	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:28:08.103125
203	\N	\N	TTN_TRANSFORM	SUCCESS	8824	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:19:13.820755
204	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:19:13.870794
205	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 12:21:35.513132
206	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:22:21.793146
207	\N	\N	TTN_SAVE	SUCCESS	8825	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:22:22.846271
208	\N	\N	ANCE_VALIDATE	SUCCESS	8825	2025-1.xml	\N	\N	\N	2025-08-30 12:22:31.345654
209	\N	\N	TTN_TRANSFORM	SUCCESS	8825	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:22:31.894489
210	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:22:31.941177
211	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 12:23:04.68656
212	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:36:03.533492
213	\N	\N	TTN_SAVE	SUCCESS	8827	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:36:04.831119
214	\N	\N	ANCE_VALIDATE	SUCCESS	8827	2025-1.xml	\N	\N	\N	2025-08-30 12:36:13.493212
215	\N	\N	TTN_TRANSFORM	SUCCESS	8827	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:36:14.055837
216	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:36:14.106355
217	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:45:00.411191
218	\N	\N	TTN_SAVE	SUCCESS	8827	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:45:02.022766
219	\N	\N	ANCE_VALIDATE	SUCCESS	8827	2025-1.xml	\N	\N	\N	2025-08-30 12:45:10.535773
220	\N	\N	TTN_TRANSFORM	SUCCESS	8827	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:45:11.128464
221	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:45:11.17651
222	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 12:51:16.092362
223	\N	\N	TTN_SAVE	SUCCESS	8820	2025-1.xml	File processed successfully	\N	\N	2025-08-30 12:51:17.311613
224	\N	\N	ANCE_VALIDATE	SUCCESS	8820	2025-1.xml	\N	\N	\N	2025-08-30 12:51:25.832393
225	\N	\N	TTN_TRANSFORM	SUCCESS	8820	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 12:51:26.334037
226	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 12:51:26.376758
227	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 13:03:45.429066
228	\N	\N	TTN_SAVE	SUCCESS	8840	2025-1.xml	File processed successfully	\N	\N	2025-08-30 13:03:46.548587
229	\N	\N	ANCE_VALIDATE	SUCCESS	8840	2025-1.xml	\N	\N	\N	2025-08-30 13:03:55.038878
230	\N	\N	TTN_TRANSFORM	SUCCESS	8840	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 13:03:55.594131
231	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 13:03:55.636694
232	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 13:05:20.290612
233	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 13:11:24.257976
234	\N	\N	TTN_SAVE	SUCCESS	8820	2025-1.xml	File processed successfully	\N	\N	2025-08-30 13:11:25.532563
235	\N	\N	ANCE_VALIDATE	SUCCESS	8820	2025-1.xml	\N	\N	\N	2025-08-30 13:11:35.010869
236	\N	\N	TTN_TRANSFORM	SUCCESS	8820	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 13:11:35.540695
237	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 13:11:35.60538
238	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 13:12:22.070067
239	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 13:45:15.877422
240	\N	\N	TTN_SAVE	SUCCESS	8820	2025-1.xml	File processed successfully	\N	\N	2025-08-30 13:45:17.224557
241	\N	\N	ANCE_VALIDATE	SUCCESS	8820	2025-1.xml	\N	\N	\N	2025-08-30 13:45:26.776099
242	\N	\N	TTN_TRANSFORM	SUCCESS	8820	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 13:45:27.317994
243	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 13:45:27.359814
244	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 13:46:14.011511
245	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 13:46:50.754098
246	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 14:08:04.945869
247	\N	\N	TTN_SAVE	SUCCESS	8830	2025-1.xml	File processed successfully	\N	\N	2025-08-30 14:08:06.195298
248	\N	\N	ANCE_VALIDATE	SUCCESS	8830	2025-1.xml	\N	\N	\N	2025-08-30 14:08:14.693017
249	\N	\N	TTN_TRANSFORM	SUCCESS	8830	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:08:15.205058
250	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 14:08:15.245869
251	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 14:16:18.042442
252	\N	\N	TTN_SAVE	SUCCESS	8834	2025-1.xml	File processed successfully	\N	\N	2025-08-30 14:16:19.262351
253	\N	\N	ANCE_VALIDATE	SUCCESS	8834	2025-1.xml	\N	\N	\N	2025-08-30 14:16:27.985299
254	\N	\N	TTN_TRANSFORM	SUCCESS	8834	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:16:28.535222
255	\N	\N	ANCE_SIGN	SUCCESS	8806	2025.xml	\N	\N	\N	2025-08-30 14:16:31.77427
256	\N	\N	TTN_SAVE	SUCCESS	14669	2025.xml	File processed successfully	\N	\N	2025-08-30 14:16:32.434979
257	\N	\N	ANCE_VALIDATE	SUCCESS	14669	2025.xml	\N	\N	\N	2025-08-30 14:16:34.123523
258	\N	\N	TTN_TRANSFORM	SUCCESS	14669	2025.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:16:34.623282
259	\N	\N	ANCE_SIGN	SUCCESS	29977	exemple_signe_elfatoora.xml	\N	\N	\N	2025-08-30 14:16:37.351772
260	\N	\N	TTN_SAVE	SUCCESS	35836	exemple_signe_elfatoora.xml	File processed successfully	\N	\N	2025-08-30 14:16:37.990899
261	\N	\N	ANCE_VALIDATE	SUCCESS	35836	exemple_signe_elfatoora.xml	\N	\N	\N	2025-08-30 14:16:51.941468
262	\N	\N	TTN_TRANSFORM	SUCCESS	35836	exemple_signe_elfatoora.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:16:52.475572
263	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 3 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 14:16:52.537374
264	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 14:52:51.538861
265	\N	\N	TTN_SAVE	SUCCESS	8832	2025-1.xml	File processed successfully	\N	\N	2025-08-30 14:52:52.744592
266	\N	\N	ANCE_VALIDATE	SUCCESS	8832	2025-1.xml	\N	\N	\N	2025-08-30 14:53:02.285126
267	\N	\N	TTN_TRANSFORM	SUCCESS	8832	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:53:02.855666
268	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 14:53:02.894141
269	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 14:57:45.270317
270	\N	\N	TTN_SAVE	SUCCESS	8832	2025-1.xml	File processed successfully	\N	\N	2025-08-30 14:57:46.499087
271	\N	\N	ANCE_VALIDATE	SUCCESS	8832	2025-1.xml	\N	\N	\N	2025-08-30 14:57:54.969312
272	\N	\N	TTN_TRANSFORM	SUCCESS	8832	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 14:57:55.513343
273	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 14:57:55.564956
274	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:00:48.059227
275	\N	\N	TTN_SAVE	SUCCESS	8828	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:00:49.306926
276	\N	\N	ANCE_VALIDATE	SUCCESS	8828	2025-1.xml	\N	\N	\N	2025-08-30 15:00:57.773541
277	\N	\N	TTN_TRANSFORM	SUCCESS	8828	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:00:58.383181
278	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:00:58.434926
279	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:01:28.412596
280	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:05:56.419661
281	\N	\N	TTN_SAVE	SUCCESS	8832	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:05:57.730703
282	\N	\N	ANCE_VALIDATE	SUCCESS	8832	2025-1.xml	\N	\N	\N	2025-08-30 15:06:06.278788
283	\N	\N	TTN_TRANSFORM	SUCCESS	8832	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:06:06.846806
284	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:06:06.889582
285	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:06:26.559787
286	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:07:43.783009
287	\N	\N	TTN_SAVE	SUCCESS	8824	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:07:45.016019
288	\N	\N	ANCE_VALIDATE	SUCCESS	8824	2025-1.xml	\N	\N	\N	2025-08-30 15:07:53.474893
289	\N	\N	TTN_TRANSFORM	SUCCESS	8824	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:07:53.990397
290	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:07:54.055019
291	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:08:09.137038
292	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:17:55.173549
293	\N	\N	TTN_SAVE	SUCCESS	8826	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:17:56.431915
294	\N	\N	ANCE_VALIDATE	SUCCESS	8826	2025-1.xml	\N	\N	\N	2025-08-30 15:18:04.939963
295	\N	\N	TTN_TRANSFORM	SUCCESS	8826	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:18:05.490272
296	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:18:05.547777
297	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:18:21.80569
298	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:28:38.689358
299	\N	\N	TTN_SAVE	SUCCESS	8822	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:28:39.996064
300	\N	\N	ANCE_VALIDATE	SUCCESS	8822	2025-1.xml	\N	\N	\N	2025-08-30 15:28:48.537069
301	\N	\N	TTN_TRANSFORM	SUCCESS	8822	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:28:49.1326
302	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:28:49.175451
303	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:29:09.661926
304	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:30:29.900705
305	\N	\N	TTN_SAVE	SUCCESS	8833	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:30:31.009346
306	\N	\N	ANCE_VALIDATE	SUCCESS	8833	2025-1.xml	\N	\N	\N	2025-08-30 15:30:39.555847
307	\N	\N	TTN_TRANSFORM	SUCCESS	8833	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:30:40.110792
308	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:30:40.176574
309	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:30:54.734247
310	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-1.xml	\N	\N	\N	2025-08-30 15:37:01.609468
311	\N	\N	TTN_SAVE	SUCCESS	8803	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:37:02.903301
312	\N	\N	ANCE_VALIDATE	SUCCESS	8803	2025-1.xml	\N	\N	\N	2025-08-30 15:37:12.434148
313	\N	\N	TTN_TRANSFORM	SUCCESS	8803	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:37:12.960727
314	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:37:12.999083
315	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:37:26.810774
316	\N	\N	ANCE_SIGN	SUCCESS	8803	2025-1.xml	\N	\N	\N	2025-08-30 15:56:22.539214
317	\N	\N	TTN_SAVE	SUCCESS	14635	2025-1.xml	File processed successfully	\N	\N	2025-08-30 15:56:23.588016
318	\N	\N	ANCE_VALIDATE	SUCCESS	14635	2025-1.xml	\N	\N	\N	2025-08-30 15:56:32.538995
319	\N	\N	TTN_TRANSFORM	SUCCESS	14635	2025-1.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:56:33.165253
320	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:56:33.201819
321	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-2.xml	\N	\N	\N	2025-08-30 15:56:56.386875
322	\N	\N	TTN_SAVE	SUCCESS	8803	2025-2.xml	File processed successfully	\N	\N	2025-08-30 15:56:57.49969
323	\N	\N	ANCE_VALIDATE	SUCCESS	8803	2025-2.xml	\N	\N	\N	2025-08-30 15:56:57.819704
324	\N	\N	TTN_TRANSFORM	SUCCESS	8803	2025-2.xml	XML to HTML transformation completed	\N	\N	2025-08-30 15:56:58.297656
325	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 15:56:58.331955
326	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 15:57:14.383586
327	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-2.xml	\N	\N	\N	2025-08-30 16:00:59.870877
328	\N	\N	TTN_SAVE	SUCCESS	8803	2025-2.xml	File processed successfully	\N	\N	2025-08-30 16:01:01.116711
329	\N	\N	ANCE_VALIDATE	SUCCESS	8803	2025-2.xml	\N	\N	\N	2025-08-30 16:01:09.69988
330	\N	\N	TTN_TRANSFORM	SUCCESS	8803	2025-2.xml	XML to HTML transformation completed	\N	\N	2025-08-30 16:01:10.257348
331	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 16:01:10.314167
332	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 16:01:26.667677
333	\N	\N	ANCE_SIGN	SUCCESS	2971	2025-3.xml	\N	\N	\N	2025-08-30 16:05:34.378685
334	\N	\N	TTN_SAVE	SUCCESS	8803	2025-3.xml	File processed successfully	\N	\N	2025-08-30 16:05:35.586132
335	\N	\N	ANCE_VALIDATE	SUCCESS	8803	2025-3.xml	\N	\N	\N	2025-08-30 16:05:44.080472
336	\N	\N	TTN_TRANSFORM	SUCCESS	8803	2025-3.xml	XML to HTML transformation completed	\N	\N	2025-08-30 16:05:44.636201
337	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Workflow completed: 1 successful, 0 failed. ZIP created.	\N	\N	2025-08-30 16:05:44.681856
338	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-08-30 16:05:58.485092
339	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 10:41:02.503203
472	\N	\N	TTN_TRANSFORM	SUCCESS	690056	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:28:09.316208
340	\N	\N	TTN_SAVE	FAILURE	373703	20250659 (8).xml	File processing failed	I/O error on POST request for "https://test.elfatoora.tn:443/ElfatouraServices/EfactService": Connection timed out: connect	\N	2025-09-08 10:41:23.722898
341	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	I/O error on POST request for "https://test.elfatoora.tn:443/ElfatouraServices/EfactService": Connection timed out: connect	\N	2025-09-08 10:41:49.830589
342	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 10:41:49.919055
343	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 10:41:49.932604
344	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 10:44:38.315258
345	\N	\N	TTN_SAVE	FAILURE	373706	20250659 (1).xml	File processing failed	I/O error on POST request for "https://test.elfatoora.tn:443/ElfatouraServices/EfactService": Connection timed out: connect	\N	2025-09-08 10:44:59.41818
346	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	I/O error on POST request for "https://test.elfatoora.tn:443/ElfatouraServices/EfactService": Connection timed out: connect	\N	2025-09-08 10:45:25.478366
347	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 10:45:25.543074
348	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 10:45:25.555077
349	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 11:23:38.769186
350	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 11:23:40.123309
351	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	500 Erreur Interne de Servlet: "<?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body><S:Fault xmlns:ns4="http://www.w3.org/2003/05/soap-envelope"><faultcode>S:Client</faultcode><faultstring>Couldn't create SOAP message due to exception: XML reader error: com.ctc.wstx.exc.WstxIOException: Invalid UTF-8 middle byte 0x72 (at char #890, byte #37)</faultstring></S:Fault></S:Body></S:Envelope>"	\N	2025-09-08 11:23:45.258162
352	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 11:23:45.316484
353	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 11:23:45.326817
354	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 11:26:02.319023
355	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 11:26:04.144592
356	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 11:26:09.830825
357	\N	\N	TTN_TRANSFORM	FAILURE	0	20250659 (8).xml	XML to HTML transformation failed	400 Mauvaise Requête: "{"message":"Document et les critères sont vides","errorCode":"SERV01"}"	\N	2025-09-08 11:26:10.689815
358	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 11:26:10.748957
359	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 11:26:10.779965
360	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 12:04:03.290056
361	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 12:04:05.033319
362	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:04:10.543938
363	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:04:21.110139
364	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:04:32.580127
365	\N	\N	TTN_TRANSFORM	SUCCESS	690080	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 12:04:34.685985
366	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 12:04:34.788455
367	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 12:04:34.79443
368	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 12:06:03.768172
369	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 12:06:05.451179
370	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:06:10.84513
371	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:06:21.244043
372	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:06:32.965315
373	\N	\N	TTN_TRANSFORM	SUCCESS	690048	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 12:06:34.808899
374	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 12:06:34.886093
375	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 12:06:34.896064
376	\N	\N	ANCE_SIGN	SUCCESS	367901	20250659.xml	\N	\N	\N	2025-09-08 12:08:31.386487
377	\N	\N	TTN_SAVE	SUCCESS	373711	20250659.xml	File processed successfully	\N	\N	2025-09-08 12:08:32.546759
378	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	500 Erreur Interne de Servlet: "<?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body><S:Fault xmlns:ns4="http://www.w3.org/2003/05/soap-envelope"><faultcode>S:Client</faultcode><faultstring>Couldn't create SOAP message due to exception: XML reader error: com.ctc.wstx.exc.WstxIOException: Invalid UTF-8 middle byte 0x72 (at char #902, byte #37)</faultstring></S:Fault></S:Body></S:Envelope>"	\N	2025-09-08 12:08:37.639642
379	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 12:08:37.67123
380	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 12:08:37.673837
381	\N	\N	ANCE_SIGN	SUCCESS	8803	2025-2.xml	\N	\N	\N	2025-09-08 12:10:34.842336
382	\N	\N	TTN_SAVE	SUCCESS	14634	2025-2.xml	File processed successfully	\N	\N	2025-09-08 12:10:35.590373
383	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:10:40.738731
384	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:10:50.976532
385	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:11:01.078715
386	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:11:11.226144
387	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 12:11:21.336864
388	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 12:11:21.377097
389	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 12:11:21.3888
390	\N	\N	ANCE_SIGN	SUCCESS	8803	2025-2.xml	\N	\N	\N	2025-09-08 12:15:33.464641
391	\N	\N	TTN_SAVE	SUCCESS	14634	2025-2.xml	File processed successfully	\N	\N	2025-09-08 12:15:34.538922
473	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed, 3 total. ZIP created.	\N	\N	2025-09-08 13:28:09.33413
477	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:38:27.784731
392	\N	\N	WORKFLOW_PROCESS	FAILURE	8803	2025-2.xml	Workflow processing failed: TTN a retourné une erreur : <?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body><S:Fault xmlns:ns4="http://www.w3.org/2003/05/soap-envelope"><faultcode>S:Server</faultcode><faultstring>tn.com.tradenet.elfatoura.services.exception.EfactServiceException</faultstring><detail><ns2:FaultBean xmlns:ns2="http://services.elfatoura.tradenet.com.tn/"><faultCode>SERV09</faultCode><faultMessage>Une seule signature electronique est exigee</faultMessage></ns2:FaultBean></detail></S:Fault></S:Body></S:Envelope>	TTN a retourné une erreur : <?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body><S:Fault xmlns:ns4="http://www.w3.org/2003/05/soap-envelope"><faultcode>S:Server</faultcode><faultstring>tn.com.tradenet.elfatoura.services.exception.EfactServiceException</faultstring><detail><ns2:FaultBean xmlns:ns2="http://services.elfatoura.tradenet.com.tn/"><faultCode>SERV09</faultCode><faultMessage>Une seule signature electronique est exigee</faultMessage></ns2:FaultBean></detail></S:Fault></S:Body></S:Envelope>	\N	2025-09-08 12:15:34.556843
393	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 12:15:34.715962
394	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 12:15:34.749496
395	\N	\N	ANCE_SIGN	SUCCESS	8803	2025-2.xml	\N	\N	\N	2025-09-08 13:07:20.599009
396	\N	\N	TTN_SAVE	SUCCESS	14634	2025-2.xml	File processed successfully	\N	\N	2025-09-08 13:07:21.890506
397	\N	\N	WORKFLOW_PROCESS	FAILURE	8803	2025-2.xml	Workflow processing failed: Une seule signature electronique est exigee	Une seule signature electronique est exigee	\N	2025-09-08 13:07:22.052689
398	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:07:22.089279
399	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:07:22.101718
400	\N	\N	ANCE_SIGN	SUCCESS	367901	20250659.xml	\N	\N	\N	2025-09-08 13:08:01.656401
401	\N	\N	TTN_SAVE	SUCCESS	373708	20250659.xml	File processed successfully	\N	\N	2025-09-08 13:08:02.83379
402	\N	\N	WORKFLOW_PROCESS	FAILURE	367901	20250659.xml	Workflow processing failed: Votre matricule est invalide : Merci de vérifier votre matricule dans la facture xml, la balise 'MessageSenderIdentifier'	Votre matricule est invalide : Merci de vérifier votre matricule dans la facture xml, la balise 'MessageSenderIdentifier'	\N	2025-09-08 13:08:02.86877
403	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:08:02.912194
404	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:08:02.924518
405	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659.xml	\N	\N	\N	2025-09-08 13:08:40.29396
406	\N	\N	TTN_SAVE	SUCCESS	373706	20250659.xml	File processed successfully	\N	\N	2025-09-08 13:08:41.477155
407	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659.xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:08:41.499768
408	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:08:41.522316
409	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:08:41.53448
410	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:09:08.43268
411	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:09:10.139855
412	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:09:15.653883
413	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:09:26.004519
414	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:09:38.061414
415	\N	\N	TTN_TRANSFORM	SUCCESS	690072	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:09:39.752226
416	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:09:39.819449
417	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 13:09:39.827433
418	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:15:15.293479
419	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:15:16.504958
420	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:15:16.535942
421	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:15:16.567843
422	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:15:16.58028
423	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:18:17.279663
424	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:18:18.69799
425	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:18:18.706929
426	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:18:18.7179
427	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:18:18.720742
428	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:19:43.835845
429	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:19:45.71495
474	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed	\N	\N	2025-09-08 13:28:09.337151
475	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:38:20.599025
476	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:38:22.318571
430	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:19:45.911337
431	\N	\N	PARALLEL_WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:19:45.957045
432	\N	\N	WORKFLOW_COMPLETE	FAILURE	\N	\N	Parallel workflow completed: 0 successful, 1 failed	\N	\N	2025-09-08 13:19:45.968246
433	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:20:54.948877
434	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:20:56.187427
435	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:20:56.211657
436	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:20:56.315642
437	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659.xml	\N	\N	\N	2025-09-08 13:20:58.26777
438	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:20:58.268786
439	\N	\N	TTN_SAVE	SUCCESS	373706	20250659.xml	File processed successfully	\N	\N	2025-09-08 13:20:59.467526
440	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659.xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:20:59.488862
441	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:21:03.69667
442	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:21:14.357571
443	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:21:26.747783
444	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:21:28.575748
445	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed, 3 total. ZIP created.	\N	\N	2025-09-08 13:21:28.598126
446	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed	\N	\N	2025-09-08 13:21:28.602096
447	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:23:29.61851
448	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:23:30.438344
449	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:23:30.876715
450	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659.xml	\N	\N	\N	2025-09-08 13:23:30.883629
451	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:23:30.909396
452	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:23:32.074248
453	\N	\N	TTN_SAVE	SUCCESS	373706	20250659.xml	File processed successfully	\N	\N	2025-09-08 13:23:32.137318
454	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659.xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:23:32.15491
455	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:23:37.55995
456	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:23:47.928488
457	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:23:59.078758
458	\N	\N	TTN_TRANSFORM	SUCCESS	690052	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:24:00.390044
459	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed, 3 total. ZIP created.	\N	\N	2025-09-08 13:24:00.412314
460	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 2 failed	\N	\N	2025-09-08 13:24:00.416215
461	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:27:39.709133
462	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659.xml	\N	\N	\N	2025-09-08 13:27:39.935796
463	\N	\N	ANCE_SIGN	SUCCESS	367896	20250659 (1).xml	\N	\N	\N	2025-09-08 13:27:40.041292
464	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:27:41.314654
465	\N	\N	TTN_SAVE	SUCCESS	373706	20250659.xml	File processed successfully	\N	\N	2025-09-08 13:27:41.64703
466	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659.xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:27:41.664849
467	\N	\N	TTN_SAVE	SUCCESS	373706	20250659 (1).xml	File processed successfully	\N	\N	2025-09-08 13:27:41.765811
468	\N	\N	WORKFLOW_PROCESS	FAILURE	367896	20250659 (1).xml	Workflow processing failed: Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	Document non valide ligne numéro : 2157 colonne numéro: 52 message: cvc-pattern-valid: Value '-  34.954' is not facet-valid with respect to pattern '-?[0-9]{1,15}([,.][0-9]{1,5})?' for type 'monetaryAmountType'.	\N	2025-09-08 13:27:41.781769
469	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:27:46.764944
470	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:27:57.182076
478	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:38:38.136612
479	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:38:49.586707
480	\N	\N	TTN_TRANSFORM	SUCCESS	690048	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:38:51.706291
481	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:38:51.743708
482	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 13:38:51.755715
483	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:50:31.497456
484	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:50:34.948024
485	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:50:40.421487
486	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:50:50.905275
487	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:51:02.41217
488	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:51:04.526206
489	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:51:04.584725
490	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 13:51:04.58971
491	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:55:39.755553
492	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:55:41.529929
493	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:55:46.987101
494	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:55:57.53113
495	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:56:09.509305
496	\N	\N	TTN_TRANSFORM	SUCCESS	690080	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 13:56:11.596664
497	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 13:56:11.629672
498	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 13:56:11.632664
499	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 13:59:41.909279
500	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 13:59:44.692786
501	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 13:59:52.327989
502	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:00:03.14728
503	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:00:15.926809
504	\N	\N	TTN_TRANSFORM	SUCCESS	690084	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 14:00:19.268443
505	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 14:00:19.292898
506	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 14:00:19.299774
507	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 14:00:43.036215
508	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 14:00:45.904817
509	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:00:51.243629
510	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:01:01.634392
511	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:01:13.362464
512	\N	\N	TTN_TRANSFORM	SUCCESS	690080	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 14:01:14.661637
513	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 14:01:14.701024
514	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 14:01:14.703017
515	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 14:26:31.168182
516	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 14:26:32.905829
517	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:26:38.459805
518	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:26:48.840317
519	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:27:00.328421
520	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 14:27:01.268501
521	\N	\N	TTN_TRANSFORM	SUCCESS	690080	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 14:27:02.632737
522	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 14:27:02.68906
523	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 14:27:02.700034
524	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 14:27:03.443756
525	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:27:08.924906
526	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:27:19.304331
527	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 14:27:30.796349
528	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 14:27:32.827259
529	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 14:27:32.870116
530	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 14:27:32.878099
531	\N	\N	ANCE_VALIDATE	SUCCESS	517554	20250659__8_.xml	\N	\N	\N	2025-09-08 15:28:29.011708
532	\N	\N	ANCE_VALIDATE	SUCCESS	517554	20250659__8_.xml	\N	\N	\N	2025-09-08 17:59:08.834726
533	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 18:29:10.144323
534	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 18:29:13.198843
535	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:29:19.370589
536	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:29:30.192209
537	\N	\N	TTN_TRANSFORM	FAILURE	920088	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:29:47.554651
538	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:29:47.572824
539	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 18:29:47.647923
540	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 18:29:47.66008
541	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 18:32:12.790355
542	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 18:32:14.75933
543	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:32:20.252984
544	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:32:30.900504
545	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:32:43.414068
546	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 18:32:45.840199
547	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 18:32:45.9691
548	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 18:32:45.982077
549	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 18:42:25.514544
550	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 18:42:27.435599
551	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:42:32.968386
552	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:42:43.621626
553	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:42:55.611584
554	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 18:42:57.758403
555	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 18:42:58.054507
556	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 18:42:58.071622
557	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:43:34.816178
558	\N	\N	TTN_TRANSFORM	FAILURE	920088	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:48:43.594131
559	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:48:43.730377
560	\N	\N	TTN_TRANSFORM	FAILURE	920088	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:51:12.588147
561	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:51:12.60991
562	\N	\N	TTN_TRANSFORM	FAILURE	920088	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:54:21.953996
563	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:54:22.057845
564	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 18:55:53.726188
565	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 18:55:55.376173
566	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:56:00.891817
567	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:56:11.556402
568	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:56:23.81896
569	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 18:56:25.045343
570	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 18:56:25.172811
571	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 18:56:25.186079
572	\N	\N	TTN_TRANSFORM	FAILURE	920092	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:56:57.901105
573	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-08 18:56:57.939423
574	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 18:59:18.178653
575	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 18:59:19.848215
576	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:59:25.483198
577	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:59:35.93275
578	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 18:59:48.328124
579	\N	\N	TTN_TRANSFORM	SUCCESS	690064	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 18:59:49.539456
580	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 18:59:49.623088
581	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 18:59:49.633699
582	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 19:10:10.027885
583	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 19:10:10.543487
584	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 19:10:11.728941
585	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 19:10:12.2012
586	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:17.178732
587	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:17.628307
588	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:27.604304
589	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:27.940259
590	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 19:10:29.811271
591	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 19:10:31.395142
592	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:36.92068
593	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:41.852608
594	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:41.951074
595	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 19:10:43.404829
596	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 19:10:43.534849
597	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 19:10:43.557001
598	\N	\N	TTN_TRANSFORM	SUCCESS	690072	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 19:10:43.646015
599	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 19:10:43.72907
600	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 19:10:43.738698
601	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:47.30592
602	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:10:59.833591
603	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 19:11:00.890726
604	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 19:11:00.979844
605	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 19:11:00.988001
606	\N	\N	ANCE_VALIDATE	SUCCESS	517554	20250659__8_.xml	\N	\N	\N	2025-09-08 19:30:06.42199
607	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-08 19:32:01.978979
608	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-08 19:32:03.781231
609	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:32:09.311975
610	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:32:19.862926
611	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-08 19:32:32.140056
612	\N	\N	TTN_TRANSFORM	SUCCESS	690052	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-08 19:32:33.290903
613	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-08 19:32:33.372509
614	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-08 19:32:33.382327
615	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-09 10:08:37.35701
616	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-09 10:08:39.366709
617	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 10:08:44.900299
618	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 10:08:57.069478
619	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-09 10:08:58.095973
620	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-09 10:08:58.221413
621	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-09 10:08:58.227856
622	\N	\N	TTN_TRANSFORM	FAILURE	920092	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:09:19.139588
623	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:09:19.191677
624	\N	\N	TTN_TRANSFORM	FAILURE	920092	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:25:48.637248
625	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:25:48.729675
626	\N	\N	TTN_TRANSFORM	FAILURE	920092	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:27:09.906324
627	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:27:09.925554
628	\N	\N	TTN_TRANSFORM	FAILURE	920092	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:28:38.550104
629	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	Failed to transform XML to HTML: 500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 10:28:38.690777
630	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:41:59.528548
631	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:45:21.003717
632	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:46:06.053668
633	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:46:52.661677
634	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:48:08.859688
635	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:55:32.572355
636	\N	\N	TTN_CONSULT	FAILURE	\N	\N	Consult operation failed	415 Type de Support Non Supporté: [no body]	\N	2025-09-09 10:56:22.314795
637	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 11:52:07.324734
638	\N	\N	TTN_TRANSFORM	FAILURE	920088	consult-efact.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 11:52:10.097603
639	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 11:55:21.695051
640	\N	\N	TTN_TRANSFORM	FAILURE	920088	20250659.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 11:55:23.129065
641	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 12:17:43.942499
642	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 12:20:16.871941
643	\N	\N	TTN_TRANSFORM	FAILURE	920088	consultEfact_1757413216968.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 12:20:18.083828
644	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 14:38:46.878825
645	\N	\N	TTN_TRANSFORM	FAILURE	920088	consultEfact_1757421526963.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 14:38:48.310918
646	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-09 14:46:36.091365
647	\N	\N	TTN_TRANSFORM	FAILURE	920088	consultEfact_1757421996191.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-09 14:46:37.370738
648	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 11:16:50.925862
649	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 11:16:52.748441
650	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:16:58.291923
651	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:17:09.234599
652	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:17:21.815383
653	\N	\N	TTN_TRANSFORM	SUCCESS	690084	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 11:17:22.943487
654	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 11:17:23.175603
655	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 11:17:23.186179
656	\N	\N	ANCE_VALIDATE	SUCCESS	517554	20250659__8_.xml	\N	\N	\N	2025-09-10 11:25:10.76541
657	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:38:25.222293
720	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:56:45.229732
658	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757497105236.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 11:38:26.533041
659	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:52:43.619455
660	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757497963751.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 11:52:44.933693
661	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 11:57:46.627427
662	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757498266824.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 11:57:48.025525
663	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 12:02:19.865785
664	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757498539999.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 12:02:21.2531
665	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 12:07:18.881392
666	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757498839041.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 12:07:20.271823
667	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 12:13:22.081649
668	\N	\N	TTN_TRANSFORM	FAILURE	920112	consultEfact_1757499202269.xml	XML to HTML transformation failed	500 Erreur Interne de Servlet: "{"message":"Erreur interne du serveur","errorCode":"SERV07"}"	\N	2025-09-10 12:13:23.584264
669	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 14:59:30.939297
670	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 14:59:37.06573
671	\N	\N	TTN_TRANSFORM	SUCCESS	690084	invoice_1810910.xml	XML to HTML transformation completed	\N	\N	2025-09-10 14:59:38.222945
672	\N	\N	TTN_CONSULT_HTML	SUCCESS	\N	\N	TTN consultation with HTML completed successfully	\N	\N	2025-09-10 14:59:38.232849
673	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:00:28.510385
674	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:00:34.570005
675	\N	\N	TTN_TRANSFORM	SUCCESS	690084	invoice_1810910.xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:00:35.578133
676	\N	\N	TTN_CONSULT_HTML	SUCCESS	\N	\N	TTN consultation with HTML completed successfully	\N	\N	2025-09-10 15:00:35.582261
677	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:25:13.493909
678	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:25:13.494906
679	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:25:15.603481
680	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:25:15.676242
681	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:21.025642
682	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:21.131069
683	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:31.378591
684	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:31.435065
685	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:42.536046
686	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:25:42.558418
687	\N	\N	TTN_TRANSFORM	SUCCESS	690048	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:25:44.003372
688	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:25:44.029766
689	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:25:44.03176
690	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:25:44.231757
691	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:25:44.249695
692	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:25:44.252659
693	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:29:14.590569
694	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:29:20.49594
695	\N	\N	TTN_TRANSFORM	SUCCESS	690084	invoice_1810910.xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:29:21.630573
696	\N	\N	TTN_CONSULT_HTML	SUCCESS	\N	\N	TTN consultation with HTML completed successfully	\N	\N	2025-09-10 15:29:21.635747
697	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:34:47.802925
698	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:34:49.8564
699	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:34:55.323507
700	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:35:08.335629
701	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:35:09.545913
702	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:35:10.18325
703	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:35:10.19925
704	\N	\N	ANCE_VALIDATE	SUCCESS	373703	20250659__8_.xml	\N	\N	\N	2025-09-10 15:40:20.436411
705	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:56:14.070638
706	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:56:15.486477
707	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:56:15.820993
708	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:56:17.10312
709	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:21.173486
710	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:22.453805
711	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:31.525118
712	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:32.801995
713	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:42.341824
714	\N	\N	TTN_TRANSFORM	SUCCESS	690068	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:56:43.307937
715	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:56:43.331567
716	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:56:43.333918
717	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:56:43.710981
718	\N	\N	TTN_TRANSFORM	SUCCESS	690052	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:56:45.177426
719	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:56:45.226261
721	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 15:57:46.22691
722	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 15:57:47.832127
723	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:57:53.21472
724	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:58:03.589102
725	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:58:14.92723
726	\N	\N	TTN_TRANSFORM	SUCCESS	690056	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:58:15.95023
727	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 15:58:16.040989
728	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 15:58:16.047972
729	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:59:36.666332
730	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 15:59:42.925776
731	\N	\N	TTN_TRANSFORM	SUCCESS	690084	invoice_1810910.xml	XML to HTML transformation completed	\N	\N	2025-09-10 15:59:43.933933
732	\N	\N	TTN_CONSULT_HTML	SUCCESS	\N	\N	TTN consultation with HTML completed successfully	\N	\N	2025-09-10 15:59:43.950855
733	\N	\N	ANCE_VALIDATE	SUCCESS	517542	20250659__8_.xml	\N	\N	\N	2025-09-10 16:04:29.036468
734	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 16:38:06.618539
735	\N	\N	ANCE_SIGN	SUCCESS	367893	20250659 (8).xml	\N	\N	\N	2025-09-10 16:38:06.619539
736	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 16:38:08.703092
737	\N	\N	TTN_SAVE	SUCCESS	373703	20250659 (8).xml	File processed successfully	\N	\N	2025-09-10 16:38:08.781387
738	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:14.056338
739	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:14.1533
740	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:24.4682
741	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:24.533454
742	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:36.092883
743	\N	\N	TTN_CONSULT	SUCCESS	\N	\N	Consult operation completed	\N	\N	2025-09-10 16:38:36.132025
744	\N	\N	TTN_TRANSFORM	SUCCESS	690052	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 16:38:37.61926
745	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 16:38:37.661044
746	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 16:38:37.665003
747	\N	\N	TTN_TRANSFORM	SUCCESS	690048	20250659 (8).xml	XML to HTML transformation completed	\N	\N	2025-09-10 16:38:37.823349
748	\N	\N	PARALLEL_WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed, 1 total. ZIP created.	\N	\N	2025-09-10 16:38:37.855754
749	\N	\N	WORKFLOW_COMPLETE	SUCCESS	\N	\N	Parallel workflow completed: 1 successful, 0 failed	\N	\N	2025-09-10 16:38:37.860732
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, password, first_name, last_name, company_name, ttn_username, ttn_password, ttn_matricule_fiscal, ance_seal_pin, ance_seal_alias, certificate_path, is_active, is_verified, role, created_at, updated_at, last_login) FROM stdin;
2	testuser	user@xmlsign.com	$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi	Test	User	Test Company SARL	encrypted_ttn_username_test	encrypted_ttn_password_test	encrypted_matricule_fiscal_test	encrypted_ance_pin_test	\N	resources/certificates/test_cert.p12	t	t	USER	2025-08-26 18:06:42.56378	2025-08-26 18:06:42.56378	\N
3	mabrouk	mabrouk@iconeSign.com	$2a$10$36FTlM2NRBEj2J.atrZ1gufziiSOCpvOkfTvF1yCb2.ttmUlRP1QC	boukari	mabrouk	Icone	GiN3tiG5h8PZGjkF7wQ5vw==	E9MnWxx4mmEem+alEpm47fOpVf7TJZHlcNlfaNlYaG0=	pTTUyruxo39L4Ii3H3MYQQ==	jT21lHvZx85TkTxLVTq5cg==	qV2ElX09JVHPndFB/gqAfg==	resources/certificates/icone.cer	t	t	USER	2025-08-26 18:53:32.153671	2025-08-30 14:51:51.847786	2025-08-30 14:51:51.940236
1	admin	admin@iconeSign.com	$2a$10$FpiXvWHqm4QgSnF98voGdeYlNIrsI/y4xnnlo/eim77dVunAB/XHq	Admin	User	XMLSign Technologies	encrypted_ttn_username_admin	encrypted_ttn_password_admin	encrypted_matricule_fiscal_admin	encrypted_ance_pin_admin	\N	resources/certificates/admin_cert.p12	t	t	ADMIN	2025-08-26 18:06:42.55994	2025-09-08 10:34:27.278469	2025-09-08 10:34:27.462838
4	icone@topnet.tn	icone@topnet.tn	$2a$10$XrHcxTkFD3Vi8MzFtT866O2bg6FhtWmyQZiD/wyg4N4zQ4PBNn2Su	boukari	mabrouk	icone	GiN3tiG5h8PZGjkF7wQ5vw==	E9MnWxx4mmEem+alEpm47fOpVf7TJZHlcNlfaNlYaG0=	pTTUyruxo39L4Ii3H3MYQQ==	jT21lHvZx85TkTxLVTq5cg==	qV2ElX09JVHPndFB/gqAfg==	resources/certificates/icone.cer	t	t	USER	2025-09-08 10:38:37.119834	2025-09-10 15:57:17.929029	2025-09-10 15:57:18.027119
\.


--
-- Data for Name: workflow_files; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.workflow_files (id, workflow_session_id, filename, file_size, status, stage, progress, error_message, ttn_invoice_id, signed_xml_path, validation_report_path, html_report_path, created_at, updated_at, completed_at) FROM stdin;
\.


--
-- Data for Name: workflow_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.workflow_sessions (id, session_id, user_id, status, current_stage, overall_progress, total_files, successful_files, failed_files, message, error_message, zip_download_url, created_at, updated_at, completed_at) FROM stdin;
\.


--
-- Name: invoice_processing_records_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.invoice_processing_records_id_seq', 1, false);


--
-- Name: operation_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.operation_logs_id_seq', 749, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 4, true);


--
-- Name: workflow_files_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.workflow_files_id_seq', 1, false);


--
-- Name: workflow_sessions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.workflow_sessions_id_seq', 1, false);


--
-- Name: invoice_processing_records invoice_processing_records_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice_processing_records
    ADD CONSTRAINT invoice_processing_records_pkey PRIMARY KEY (id);


--
-- Name: operation_logs operation_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_logs
    ADD CONSTRAINT operation_logs_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: workflow_files workflow_files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_files
    ADD CONSTRAINT workflow_files_pkey PRIMARY KEY (id);


--
-- Name: workflow_sessions workflow_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_sessions
    ADD CONSTRAINT workflow_sessions_pkey PRIMARY KEY (id);


--
-- Name: workflow_sessions workflow_sessions_session_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_sessions
    ADD CONSTRAINT workflow_sessions_session_id_key UNIQUE (session_id);


--
-- Name: idx_operation_logs_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_operation_logs_created_at ON public.operation_logs USING btree (created_at);


--
-- Name: idx_operation_logs_operation_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_operation_logs_operation_type ON public.operation_logs USING btree (operation_type);


--
-- Name: idx_operation_logs_session_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_operation_logs_session_id ON public.operation_logs USING btree (session_id);


--
-- Name: idx_operation_logs_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_operation_logs_user_id ON public.operation_logs USING btree (user_id);


--
-- Name: idx_users_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_active ON public.users USING btree (is_active);


--
-- Name: idx_users_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_created_at ON public.users USING btree (created_at);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_workflow_files_filename; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_files_filename ON public.workflow_files USING btree (filename);


--
-- Name: idx_workflow_files_session_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_files_session_id ON public.workflow_files USING btree (workflow_session_id);


--
-- Name: idx_workflow_files_stage; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_files_stage ON public.workflow_files USING btree (stage);


--
-- Name: idx_workflow_files_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_files_status ON public.workflow_files USING btree (status);


--
-- Name: idx_workflow_sessions_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_sessions_created_at ON public.workflow_sessions USING btree (created_at);


--
-- Name: idx_workflow_sessions_session_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_sessions_session_id ON public.workflow_sessions USING btree (session_id);


--
-- Name: idx_workflow_sessions_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_sessions_status ON public.workflow_sessions USING btree (status);


--
-- Name: idx_workflow_sessions_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workflow_sessions_user_id ON public.workflow_sessions USING btree (user_id);


--
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: workflow_files update_workflow_files_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_workflow_files_updated_at BEFORE UPDATE ON public.workflow_files FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: workflow_sessions update_workflow_sessions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_workflow_sessions_updated_at BEFORE UPDATE ON public.workflow_sessions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: operation_logs operation_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_logs
    ADD CONSTRAINT operation_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: workflow_files workflow_files_workflow_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_files
    ADD CONSTRAINT workflow_files_workflow_session_id_fkey FOREIGN KEY (workflow_session_id) REFERENCES public.workflow_sessions(id) ON DELETE CASCADE;


--
-- Name: workflow_sessions workflow_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_sessions
    ADD CONSTRAINT workflow_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict xnbtQ5Gq6Le6EPdgfBdevZgod6ZlcH3Pu8m7VTmvDnNtoUrErcpaNmXPqqqEhbP

