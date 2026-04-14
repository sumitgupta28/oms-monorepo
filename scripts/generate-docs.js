const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, AlignmentType, PageBreak, BorderStyle, WidthType,
  ShadingType, LevelFormat, TableOfContents, Header, Footer,
  PageNumber, NumberFormat
} = require('docx');
const fs = require('fs');
const path = require('path');

// ── Shared style helpers ─────────────────────────────────────────────────────
const COLORS = {
  primary:    '1F3864',
  secondary:  '2E75B6',
  accent:     '1D9E75',
  lightBg:    'EBF3FB',
  altRow:     'F5F9FF',
  headerText: 'FFFFFF',
  border:     'C9D9ED',
};

function makeDoc(sections) {
  return new Document({
    numbering: {
      config: [
        { reference: 'bullets', levels: [
          { level: 0, format: LevelFormat.BULLET, text: '\u2022',
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
          { level: 1, format: LevelFormat.BULLET, text: '-',
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 1080, hanging: 360 } } } },
        ]},
        { reference: 'numbers', levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: '%1.',
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        ]},
      ],
    },
    styles: {
      default: {
        document: { run: { font: 'Arial', size: 22, color: '2C2C2A' } },
      },
      paragraphStyles: [
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run:  { size: 36, bold: true, font: 'Arial', color: COLORS.primary },
          paragraph: { spacing: { before: 360, after: 120 }, outlineLevel: 0,
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: COLORS.secondary, space: 1 } } } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run:  { size: 28, bold: true, font: 'Arial', color: COLORS.secondary },
          paragraph: { spacing: { before: 280, after: 100 }, outlineLevel: 1 } },
        { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run:  { size: 24, bold: true, font: 'Arial', color: COLORS.accent },
          paragraph: { spacing: { before: 200, after: 80 }, outlineLevel: 2 } },
        { id: 'Normal', name: 'Normal',
          run:  { size: 22, font: 'Arial' },
          paragraph: { spacing: { after: 120, line: 276, lineRule: 'auto' } } },
      ],
    },
    sections,
  });
}

function h1(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(text)] });
}
function h2(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });
}
function h3(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(text)] });
}
function p(text, opts = {}) {
  return new Paragraph({
    children: [new TextRun({ text, size: opts.size || 22, bold: opts.bold || false,
      font: 'Arial', color: opts.color || '2C2C2A', italics: opts.italic || false })],
    alignment: opts.align || AlignmentType.LEFT,
    spacing: { after: opts.afterSpacing !== undefined ? opts.afterSpacing : 120 },
  });
}
function bullet(text, level = 0) {
  return new Paragraph({
    numbering: { reference: 'bullets', level },
    children: [new TextRun({ text, size: 22, font: 'Arial' })],
    spacing: { after: 60 },
  });
}
function numbered(text) {
  return new Paragraph({
    numbering: { reference: 'numbers', level: 0 },
    children: [new TextRun({ text, size: 22, font: 'Arial' })],
    spacing: { after: 60 },
  });
}
function spacer() {
  return new Paragraph({ children: [new TextRun('')], spacing: { after: 60 } });
}
function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}

function infoTable(rows) {
  const brd = { style: BorderStyle.SINGLE, size: 1, color: COLORS.border };
  const borders = { top: brd, bottom: brd, left: brd, right: brd };
  return new Table({
    width: { size: 9360, type: WidthType.DXA },
    columnWidths: [2800, 6560],
    rows: rows.map(([label, value], i) =>
      new TableRow({
        children: [
          new TableCell({
            borders, width: { size: 2800, type: WidthType.DXA },
            margins: { top: 80, bottom: 80, left: 120, right: 120 },
            shading: { fill: COLORS.lightBg, type: ShadingType.CLEAR },
            children: [new Paragraph({ children: [new TextRun({ text: label, bold: true, size: 20, font: 'Arial' })] })],
          }),
          new TableCell({
            borders, width: { size: 6560, type: WidthType.DXA },
            margins: { top: 80, bottom: 80, left: 120, right: 120 },
            shading: { fill: i % 2 === 0 ? 'FFFFFF' : COLORS.altRow, type: ShadingType.CLEAR },
            children: [new Paragraph({ children: [new TextRun({ text: value, size: 20, font: 'Arial' })] })],
          }),
        ],
      })
    ),
  });
}

function headerTable(cols, rows) {
  const brd = { style: BorderStyle.SINGLE, size: 1, color: COLORS.border };
  const borders = { top: brd, bottom: brd, left: brd, right: brd };
  const colW = Math.floor(9360 / cols.length);
  return new Table({
    width: { size: 9360, type: WidthType.DXA },
    columnWidths: cols.map(() => colW),
    rows: [
      new TableRow({
        tableHeader: true,
        children: cols.map(c => new TableCell({
          borders,
          width: { size: colW, type: WidthType.DXA },
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          shading: { fill: COLORS.primary, type: ShadingType.CLEAR },
          children: [new Paragraph({ children: [new TextRun({ text: c, bold: true, size: 20, font: 'Arial', color: COLORS.headerText })] })],
        })),
      }),
      ...rows.map((row, ri) => new TableRow({
        children: row.map(cell => new TableCell({
          borders,
          width: { size: colW, type: WidthType.DXA },
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          shading: { fill: ri % 2 === 0 ? 'FFFFFF' : COLORS.altRow, type: ShadingType.CLEAR },
          children: [new Paragraph({ children: [new TextRun({ text: cell, size: 20, font: 'Arial' })] })],
        })),
      })),
    ],
  });
}

function coverPage(title, subtitle, docType, version, date) {
  return [
    spacer(), spacer(), spacer(),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 200 },
      children: [new TextRun({ text: 'ORDER MANAGEMENT SYSTEM', size: 52, bold: true, font: 'Arial', color: COLORS.primary })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 120 },
      children: [new TextRun({ text: title, size: 36, bold: true, font: 'Arial', color: COLORS.secondary })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 480 },
      children: [new TextRun({ text: subtitle, size: 28, font: 'Arial', color: '5F5E5A', italics: true })],
    }),
    spacer(), spacer(),
    infoTable([
      ['Document Type', docType],
      ['Version',       version],
      ['Date',          date],
      ['Status',        'Draft'],
      ['Author',        'OMS Engineering Team'],
    ]),
    pageBreak(),
  ];
}

// ═══════════════════════════════════════════════════════════════════════════
// ROOT BRD
// ═══════════════════════════════════════════════════════════════════════════
function buildRootBRD() {
  const children = [
    ...coverPage('Business Requirements Document', 'Root — All Modules', 'BRD', '1.0', '2025-04-06'),
    h1('1. Executive Summary'),
    p('The Order Management System (OMS) is an enterprise-grade, AI-powered platform that enables customers to browse products, place orders, make payments, and track fulfilment through a conversational AI interface. Administrators manage the product catalog, monitor orders, and observe AI agent activity in real time.'),
    spacer(),
    p('The system is built as a learning-oriented monorepo using Java 21, Spring Boot 3.x, Spring AI, React, Kafka, PostgreSQL, MongoDB, Redis, and Keycloak, deployed locally via Docker Compose.'),

    h1('2. Business Objectives'),
    bullet('Provide a natural-language ordering experience powered by Claude (Anthropic)'),
    bullet('Allow product browsing without authentication to reduce conversion friction'),
    bullet('Support a unified login for all roles: Customer, Admin, Agent Manager'),
    bullet('Build enterprise patterns: event-driven architecture, saga, outbox, RAG, MCP tool calling'),
    bullet('Serve as a reference implementation for learning agentic AI systems'),

    h1('3. Scope'),
    h2('3.1 In Scope'),
    bullet('Customer self-registration and login via custom React pages backed by Keycloak'),
    bullet('Public product catalog browsable without authentication'),
    bullet('AI-powered order placement, tracking, and cancellation via agent chat'),
    bullet('Mock payment processing with idempotent ledger'),
    bullet('Inventory reservation and validation'),
    bullet('Kafka-based event-driven saga across services'),
    bullet('RAG-powered product search using pgvector'),
    bullet('Admin dashboard for order management and agent activity monitoring'),
    bullet('Role-based access control: CUSTOMER, ADMIN, AGENT_MANAGER'),

    h2('3.2 Out of Scope'),
    bullet('Real payment gateway integration (Stripe / Razorpay) — mock only'),
    bullet('Email verification on registration (disabled locally)'),
    bullet('Mobile native apps'),
    bullet('Multi-region deployment'),

    h1('4. Stakeholders'),
    headerTable(
      ['Role', 'Responsibility', 'System Access'],
      [
        ['Customer',       'Browse, order, pay, track',           'React UI — Chat + Orders'],
        ['Admin',          'Manage orders, products, users',      'React UI — Admin Dashboard'],
        ['Agent Manager',  'Monitor agent calls and logs',        'React UI — Agent Logs'],
        ['Developer',      'Build, maintain, extend the system',  'Full stack + Docker Compose'],
      ]
    ),

    h1('5. Functional Requirements'),
    h2('FR-01 — Authentication & Registration'),
    bullet('Custom React login page used by all roles'),
    bullet('Customer self-registration with first name, last name, email, password'),
    bullet('CUSTOMER role assigned automatically on registration'),
    bullet('JWT tokens issued by Keycloak, validated at Spring Cloud Gateway'),
    bullet('Password strength validation client-side and server-side'),
    bullet('Forgot-password flow via Keycloak'),

    h2('FR-02 — Product Catalog'),
    bullet('Public browsing — no authentication required'),
    bullet('Full-text and semantic (RAG) search powered by pgvector'),
    bullet('Product details: name, description, price, stock level, category'),
    bullet('Guest users prompted to sign in when attempting to order'),

    h2('FR-03 — Order Management'),
    bullet('Order placement through conversational AI agent'),
    bullet('Order state machine: PENDING → VALIDATED → PAYMENT_INITIATED → PAID → SHIPPED → DELIVERED'),
    bullet('Order cancellation allowed from PENDING or VALIDATED states'),
    bullet('Customers view their own orders; Admins view all orders'),

    h2('FR-04 — Payment'),
    bullet('Mock payment gateway simulates real Stripe-style flow'),
    bullet('Idempotency keys prevent double processing'),
    bullet('Configurable failure rate for testing error paths'),
    bullet('Refund workflow for cancelled orders'),

    h2('FR-05 — AI Agent'),
    bullet('Spring AI ChatClient with @Tool-annotated MCP tools'),
    bullet('Claude (claude-sonnet-4-6) as the LLM provider'),
    bullet('Multi-turn memory via Redis-backed ChatMemory'),
    bullet('Four agents: Order, Payment, Validation, Product'),
    bullet('Tool calls visible in React UI for learning/debugging'),

    h1('6. Non-Functional Requirements'),
    headerTable(
      ['Attribute', 'Requirement', 'Notes'],
      [
        ['Performance',   'API response < 500ms p95 (ex-LLM)', 'LLM latency ~2–5s expected'],
        ['Availability',  '99% local uptime',                   'Docker Compose restart policies'],
        ['Security',      'JWT on all protected endpoints',      'Keycloak PKCE flow'],
        ['Observability', 'Prometheus + Grafana metrics',        'Spring Actuator on all services'],
        ['Scalability',   'Stateless services, Kafka for async', 'Redis session store'],
        ['Auditability',  'Full login event log',                'Keycloak Events API'],
      ]
    ),

    h1('7. Constraints'),
    bullet('Java 21 with Gradle 8.14.3 for all Spring Boot modules'),
    bullet('Docker Compose only — no Kubernetes for local development'),
    bullet('Anthropic API key required for Claude LLM calls'),
    bullet('Single Git repository — IntelliJ multi-module project structure'),

    h1('8. Glossary'),
    headerTable(
      ['Term', 'Definition'],
      [
        ['MCP',    'Model Context Protocol — Spring AI @Tool methods exposed to Claude'],
        ['RAG',    'Retrieval-Augmented Generation — pgvector similarity search + LLM'],
        ['Saga',   'Distributed transaction pattern using Kafka events'],
        ['Outbox', 'Transactional pattern ensuring event publishing reliability'],
        ['JWT',    'JSON Web Token — signed bearer token issued by Keycloak'],
      ]
    ),
  ];

  return makeDoc([{
    properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } },
    children,
  }]);
}

// ═══════════════════════════════════════════════════════════════════════════
// ROOT HLD
// ═══════════════════════════════════════════════════════════════════════════
function buildRootHLD() {
  const children = [
    ...coverPage('High-Level Design Document', 'Root — System Architecture', 'HLD', '1.0', '2025-04-06'),

    h1('1. Architecture Overview'),
    p('The OMS uses a microservices architecture with an event-driven backbone. All services are deployed as Docker containers, communicating internally via REST and Kafka. Spring Cloud Gateway is the single public entry point. Keycloak provides identity and access management.'),

    h1('2. System Layers'),
    headerTable(
      ['Layer', 'Components', 'Technology'],
      [
        ['Presentation',  'React UI — Chat, Dashboard, Admin',           'React 18, TypeScript, Tailwind'],
        ['Gateway',       'Spring Cloud Gateway + JWT validation',        'Spring Cloud Gateway 2023.x'],
        ['AI / Agent',    'Spring AI agents with @Tool MCP tools',        'Spring AI 1.x, Claude API'],
        ['Application',   'Order, Payment, Inventory, Product services',  'Spring Boot 3.3, Java 21'],
        ['Messaging',     'Async event bus',                              'Apache Kafka 3.x'],
        ['Data',          'Relational, Document, Vector, Cache',          'PostgreSQL 16, MongoDB 7, pgvector, Redis 7'],
        ['Identity',      'Auth server, user store, audit log',           'Keycloak 24'],
      ]
    ),

    h1('3. Service Inventory'),
    headerTable(
      ['Service', 'Port', 'Responsibility', 'Primary Store'],
      [
        ['gateway',              '8080', 'Routing, JWT validation, CORS',         '—'],
        ['agent-service',        '8085', 'LLM orchestration, MCP tools',          'Redis (ChatMemory)'],
        ['order-service',        '8081', 'Order CRUD, state machine',             'PostgreSQL'],
        ['payment-service',      '8082', 'Mock payment, ledger, refunds',         'PostgreSQL'],
        ['inventory-service',    '8083', 'Stock management, reservations',        'PostgreSQL'],
        ['product-service',      '8084', 'Product catalog, RAG embeddings',       'MongoDB + pgvector'],
        ['notification-service', '8086', 'Email / webhook notifications',         '— (event consumer)'],
        ['keycloak',             '8180', 'Identity, auth, login audit',           'PostgreSQL'],
        ['react-ui',             '3000', 'Customer and admin frontend',           '—'],
      ]
    ),

    h1('4. Key Design Decisions'),
    h2('4.1 Single Git Repo — Multi-Module Gradle'),
    p('All services live in one IntelliJ project with Gradle submodules. This simplifies dependency management, enables shared libraries, and is ideal for learning.'),

    h2('4.2 Event-Driven Saga'),
    p('Order placement triggers a Kafka saga: OrderPlaced → InventoryReserved → PaymentInitiated → PaymentConfirmed → OrderConfirmed. Each step is a separate Kafka topic. Failure at any step publishes a compensating event.'),

    h2('4.3 MCP via Spring AI @Tool'),
    p('MCP tool definitions are Java methods annotated with @Tool inside agent-service. Spring AI registers them with Claude automatically. No separate MCP server process is needed.'),

    h2('4.4 RAG with pgvector'),
    p('Product embeddings are stored in PostgreSQL with the pgvector extension. The Product Agent embeds the user query and runs cosine similarity search to retrieve relevant products before calling Claude.'),

    h2('4.5 Public Product Browsing'),
    p('The /api/products/** route in the Gateway is permitAll(). Authenticated users get personalised results. Guests see the full catalog but cannot order.'),

    h1('5. Kafka Topic Map'),
    headerTable(
      ['Topic', 'Producer', 'Consumer(s)', 'Purpose'],
      [
        ['oms.orders.placed',       'order-service',     'inventory-service, payment-service', 'New order created'],
        ['oms.inventory.reserved',  'inventory-service', 'payment-service',                    'Stock confirmed'],
        ['oms.payment.initiated',   'payment-service',   'payment-service (async)',             'Payment in progress'],
        ['oms.payment.confirmed',   'payment-service',   'order-service',                      'Payment success'],
        ['oms.payment.failed',      'payment-service',   'order-service, inventory-service',   'Compensate saga'],
        ['oms.orders.shipped',      'order-service',     'notification-service',               'Trigger email'],
        ['oms.orders.cancelled',    'order-service',     'inventory-service, notification-service', 'Release stock'],
      ]
    ),

    h1('6. Security Architecture'),
    bullet('Keycloak realm: oms — issues JWTs for all users'),
    bullet('react-ui client: public, PKCE, directAccessGrants enabled'),
    bullet('Gateway validates JWT on every request using Keycloak public key (auto-fetched)'),
    bullet('TokenRelay filter forwards JWT to downstream services'),
    bullet('Each microservice validates the forwarded JWT independently'),
    bullet('@PreAuthorize annotations enforce role-based access at method level'),

    h1('7. Observability Stack'),
    headerTable(
      ['Tool', 'Port', 'Purpose'],
      [
        ['Spring Actuator',  'per-service /actuator', 'Health, metrics, httptrace'],
        ['Prometheus',       '9090',                  'Metrics scraping'],
        ['Grafana',          '3001',                  'Dashboards — latency, throughput, errors'],
        ['Kafdrop',          '9000',                  'Kafka topic and consumer group viewer'],
        ['Keycloak Console', '8180/admin',             'User management, login event log'],
      ]
    ),
  ];

  return makeDoc([{
    properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } },
    children,
  }]);
}

// ═══════════════════════════════════════════════════════════════════════════
// ROOT LLD
// ═══════════════════════════════════════════════════════════════════════════
function buildRootLLD() {
  const children = [
    ...coverPage('Low-Level Design Document', 'Root — Cross-Cutting Concerns', 'LLD', '1.0', '2025-04-06'),

    h1('1. Monorepo Structure'),
    p('The repository is structured as a Gradle multi-module project. Each Spring Boot service is a submodule. The root build.gradle defines shared plugins, Java 21 toolchain, and common dependencies.'),

    h2('1.1 Directory Layout'),
    p('oms-monorepo/                     ← root IntelliJ project', { bold: false }),
    p('├── build.gradle                  ← shared plugin + dep config'),
    p('├── settings.gradle               ← submodule declarations'),
    p('├── gradle/wrapper/               ← Gradle 8.14.3'),
    p('├── docker-compose.yml'),
    p('├── .env.example'),
    p('├── root-docs/                    ← BRD, HLD, LLD for full system'),
    p('├── gateway/                      ← Spring Cloud Gateway submodule'),
    p('│   ├── build.gradle'),
    p('│   ├── src/'),
    p('│   └── docs/                     ← BRD, HLD, LLD, HOW-TO-RUN.md'),
    p('├── agent-service/'),
    p('├── order-service/'),
    p('├── payment-service/'),
    p('├── inventory-service/'),
    p('├── product-service/'),
    p('├── notification-service/'),
    p('├── react-ui/                     ← React + TypeScript (npm)'),
    p('│   └── docs/'),
    p('└── keycloak/realms/              ← oms-realm.json auto-import'),

    h1('2. Shared Conventions'),
    h2('2.1 Package Structure (per service)'),
    p('com.oms.<service>/'),
    bullet('controller  — REST endpoints'),
    bullet('service     — business logic'),
    bullet('repository  — Spring Data interfaces'),
    bullet('domain      — JPA entities / MongoDB documents'),
    bullet('event       — Kafka event records (shared via root)'),
    bullet('config      — Spring configuration beans'),
    bullet('exception   — custom exceptions + global handler'),

    h2('2.2 Error Handling'),
    p('Every service has a @RestControllerAdvice that maps domain exceptions to RFC 7807 ProblemDetail responses. HTTP status codes follow REST conventions: 400 validation, 401 unauthorized, 403 forbidden, 404 not found, 409 conflict, 500 internal.'),

    h2('2.3 Kafka Event Schema'),
    p('All Kafka events are Java records in the shared events package serialised as JSON:'),
    bullet('correlationId: UUID — traces a full order journey across services'),
    bullet('eventType: String — e.g. ORDER_PLACED'),
    bullet('timestamp: Instant'),
    bullet('payload: service-specific nested record'),

    h1('3. JWT Token Structure'),
    p('Keycloak issues access tokens containing:'),
    headerTable(
      ['Claim', 'Type', 'Example'],
      [
        ['sub',          'UUID string',   'a1b2c3d4-...'],
        ['email',        'String',        'user@example.com'],
        ['given_name',   'String',        'Alex'],
        ['family_name',  'String',        'Chen'],
        ['realm_access', 'Object',        '{"roles":["CUSTOMER"]}'],
        ['exp',          'Unix timestamp','1700000000'],
      ]
    ),

    h1('4. Gradle 8.14.3 + Java 21 Setup'),
    h2('4.1 Root build.gradle Key Config'),
    p('java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }'),
    p('This ensures all submodules compile with Java 21 regardless of the developer\'s local JDK.'),
    p('The Spring Boot BOM and Spring Cloud BOM are imported in dependencyManagement at root level, so no submodule needs to declare Spring versions.'),

    h2('4.2 Wrapper Configuration'),
    p('gradle/wrapper/gradle-wrapper.properties pins distributionUrl to gradle-8.14.3-bin.zip. Run ./gradlew wrapper --gradle-version 8.14.3 to regenerate if needed.'),

    h1('5. Docker Compose Network'),
    p('All containers share a single bridge network called oms-network. Services address each other by container name, e.g. http://order-service:8081. The Anthropic API is the only external call that leaves the Docker network.'),

    h1('6. Environment Variables'),
    headerTable(
      ['Variable', 'Used By', 'Example Value'],
      [
        ['ANTHROPIC_API_KEY',    'agent-service, product-service', 'sk-ant-...'],
        ['KEYCLOAK_ADMIN',       'keycloak',                       'admin'],
        ['KEYCLOAK_ADMIN_PASSWORD', 'keycloak',                    'admin'],
        ['POSTGRES_PASSWORD',    'postgres',                       'postgres'],
        ['SPRING_PROFILES_ACTIVE', 'all services',                 'local'],
      ]
    ),
    p('Copy .env.example to .env and fill in ANTHROPIC_API_KEY before running docker compose up.'),
  ];

  return makeDoc([{
    properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } },
    children,
  }]);
}

// ═══════════════════════════════════════════════════════════════════════════
// MODULE DOC BUILDER (reusable for each service)
// ═══════════════════════════════════════════════════════════════════════════
function buildModuleDocs(cfg) {
  // BRD
  const brdChildren = [
    ...coverPage(cfg.title, `${cfg.name} — Business Requirements`, 'BRD', '1.0', '2025-04-06'),
    h1('1. Purpose'),
    p(cfg.brd.purpose),
    h1('2. Business Goals'),
    ...cfg.brd.goals.map(bullet),
    h1('3. Functional Requirements'),
    ...cfg.brd.requirements.flatMap(r => [h2(r.id + ' — ' + r.title), ...r.items.map(bullet)]),
    h1('4. Non-Functional Requirements'),
    headerTable(['Attribute', 'Requirement'], cfg.brd.nfr),
    h1('5. Acceptance Criteria'),
    ...cfg.brd.acceptance.map(numbered),
  ];

  // HLD
  const hldChildren = [
    ...coverPage(cfg.title, `${cfg.name} — High-Level Design`, 'HLD', '1.0', '2025-04-06'),
    h1('1. Service Overview'),
    p(cfg.hld.overview),
    h1('2. API Endpoints'),
    headerTable(['Method', 'Path', 'Auth', 'Description'], cfg.hld.endpoints),
    h1('3. Dependencies'),
    headerTable(['Dependency', 'Type', 'Purpose'], cfg.hld.deps),
    h1('4. Kafka Events'),
    headerTable(['Topic', 'Direction', 'Event Type'], cfg.hld.kafka),
    h1('5. Data Model Summary'),
    p(cfg.hld.dataModel),
    h1('6. Security'),
    ...cfg.hld.security.map(bullet),
  ];

  // LLD
  const lldChildren = [
    ...coverPage(cfg.title, `${cfg.name} — Low-Level Design`, 'LLD', '1.0', '2025-04-06'),
    h1('1. Package Structure'),
    ...cfg.lld.packages.map(pkg => p(pkg)),
    h1('2. Key Classes'),
    headerTable(['Class', 'Type', 'Responsibility'], cfg.lld.classes),
    h1('3. Database Schema'),
    p(cfg.lld.schema),
    h1('4. Configuration Properties'),
    headerTable(['Property', 'Default', 'Description'], cfg.lld.config),
    h1('5. Error Codes'),
    headerTable(['Code', 'HTTP Status', 'Description'], cfg.lld.errors),
  ];

  return { brd: makeDoc([{ properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } }, children: brdChildren }]),
           hld: makeDoc([{ properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } }, children: hldChildren }]),
           lld: makeDoc([{ properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } }, children: lldChildren }]) };
}

// ── Module configs ───────────────────────────────────────────────────────────
const modules = [
  {
    name: 'gateway', title: 'Spring Cloud Gateway',
    brd: {
      purpose: 'The Gateway is the single public entry point for all OMS API traffic. It validates JWT tokens issued by Keycloak and routes requests to downstream microservices.',
      goals: ['Centralise authentication — no downstream service needs to handle raw login', 'Enable CORS for the React UI', 'Route traffic by path prefix to appropriate services', 'Forward JWT via TokenRelay to downstream services'],
      requirements: [
        { id: 'FR-GW-01', title: 'JWT Validation', items: ['Validate every incoming JWT against Keycloak public key', 'Reject expired or tampered tokens with 401', 'Extract roles and forward as headers'] },
        { id: 'FR-GW-02', title: 'Routing', items: ['Route /api/chat/** to agent-service:8085', 'Route /api/orders/** to order-service:8081', 'Route /api/products/** to product-service:8084 (public)', 'Route /api/payments/** to payment-service:8082'] },
        { id: 'FR-GW-03', title: 'CORS', items: ['Allow origin http://localhost:3000', 'Allow all HTTP methods and headers'] },
      ],
      nfr: [['Latency added', '< 5ms per request'], ['Availability', 'Auto-restart on crash']],
      acceptance: ['All /api/chat calls without JWT return 401', 'All /api/products calls without JWT return 200', 'TokenRelay header present on all forwarded requests'],
    },
    hld: {
      overview: 'Spring Cloud Gateway 2023.x reactive gateway. Uses WebFlux. JWT validation via spring-security-oauth2-resource-server. Keycloak JWKS endpoint polled automatically.',
      endpoints: [['ANY', '/api/**', 'JWT (except /api/products)', 'Routes to downstream service'], ['GET', '/actuator/health', 'None', 'Health probe']],
      deps: [['Keycloak', 'External', 'JWKS key fetch for JWT validation'], ['All microservices', 'Internal Docker', 'Route targets']],
      kafka: [['—', '—', 'Gateway does not produce or consume Kafka events']],
      dataModel: 'Gateway is stateless — no database. Route config is in application.yml.',
      security: ['JWT validation on all routes except /api/products and /actuator/health', 'PKCE enforcement for react-ui client', 'Rate limiting via Redis (optional, add in Phase 6)'],
    },
    lld: {
      packages: ['com.oms.gateway.config  — SecurityConfig, RouteConfig', 'com.oms.gateway.filter — JwtClaimsForwardFilter'],
      classes: [['SecurityConfig', 'Config', 'Defines SecurityWebFilterChain with JWT resource server'], ['KeycloakJwtConverter', 'Converter', 'Extracts realm_access.roles into Spring GrantedAuthority list']],
      schema: 'No database. Routes defined in application.yml under spring.cloud.gateway.routes.',
      config: [['keycloak.url', 'http://keycloak:8080', 'Keycloak base URL'], ['keycloak.realm', 'oms', 'Realm name'], ['spring.cloud.gateway.globalcors.*', 'localhost:3000', 'CORS origin']],
      errors: [['GW-001', '401', 'Missing or invalid JWT'], ['GW-002', '403', 'Insufficient role'], ['GW-003', '503', 'Downstream service unavailable']],
    },
  },
  {
    name: 'agent-service', title: 'Agent Service (Spring AI + MCP)',
    brd: {
      purpose: 'The Agent Service hosts all AI agents powered by Claude via Spring AI. It exposes @Tool-annotated MCP tools for order, payment, validation, and product operations. It manages multi-turn conversation memory in Redis.',
      goals: ['Enable natural language order placement and management', 'Expose MCP tools callable by Claude', 'Maintain conversation context across turns', 'Surface tool call activity to the UI for transparency'],
      requirements: [
        { id: 'FR-AS-01', title: 'Chat Streaming', items: ['Stream Claude responses token-by-token via SSE', 'Emit tool-call events alongside text tokens', 'Support session-based conversation history'] },
        { id: 'FR-AS-02', title: 'MCP Tools', items: ['placeOrder(userId, items) — calls order-service', 'trackOrder(orderId) — returns order status', 'cancelOrder(orderId) — cancels if eligible', 'initiatePayment(orderId) — calls payment-service', 'searchProducts(query) — RAG search', 'checkInventory(productId, qty) — stock check'] },
      ],
      nfr: [['LLM latency', '< 10s p95'], ['Memory retention', 'Last 20 turns per session'], ['Concurrent sessions', '50+ locally']],
      acceptance: ['User sends "order laptop" — agent calls placeOrder tool automatically', 'Tool call names visible in SSE stream', 'Session context persists across page reload via Redis'],
    },
    hld: {
      overview: 'Spring Boot 3.3 + Spring AI 1.x. ChatClient with registered tool beans. Redis ChatMemory. SSE endpoint for streaming. Calls downstream services via RestTemplate.',
      endpoints: [['GET', '/api/chat/stream', 'CUSTOMER JWT', 'SSE stream — chat with agents'], ['DELETE', '/api/chat/session', 'CUSTOMER JWT', 'Clear conversation history'], ['GET', '/actuator/health', 'None', 'Health probe']],
      deps: [['Claude API', 'External HTTPS', 'LLM inference and tool calling'], ['order-service', 'Internal REST', 'Order CRUD tools'], ['payment-service', 'Internal REST', 'Payment tools'], ['product-service', 'Internal REST', 'Product search + RAG'], ['Redis', 'Internal', 'ChatMemory session store']],
      kafka: [['—', '—', 'Agent service does not use Kafka directly']],
      dataModel: 'No SQL database. Conversation history stored in Redis as serialised message lists keyed by sessionId.',
      security: ['Requires CUSTOMER JWT on /api/chat/stream', 'JWT subject (userId) passed to downstream tool calls', 'ANTHROPIC_API_KEY loaded from environment variable'],
    },
    lld: {
      packages: ['com.oms.agent.config   — AgentConfig (ChatClient bean)', 'com.oms.agent.tools    — OrderTools, PaymentTools, ProductTools, ValidationTools', 'com.oms.agent.controller — ChatController (SSE endpoint)', 'com.oms.agent.client   — OrderClient, PaymentClient, ProductClient (RestTemplate wrappers)'],
      classes: [['AgentConfig', 'Config', 'Builds ChatClient with all @Tool beans and Redis ChatMemory'], ['OrderTools', 'Service + Tools', 'Four @Tool methods for order operations'], ['ChatController', 'Controller', 'SSE endpoint — calls chatClient.stream()'], ['OrderClient', 'Client', 'RestTemplate wrapper for order-service REST API']],
      schema: 'Redis key pattern: chat:session:{sessionId} — List of serialised ChatMessage objects. TTL: 24 hours.',
      config: [['spring.ai.anthropic.api-key', '${ANTHROPIC_API_KEY}', 'Claude API key'], ['spring.ai.anthropic.chat.model', 'claude-sonnet-4-6', 'Model selection'], ['spring.ai.anthropic.chat.max-tokens', '4096', 'Max response tokens'], ['spring.data.redis.host', 'redis', 'Redis host inside Docker']],
      errors: [['AS-001', '503', 'Claude API unreachable'], ['AS-002', '400', 'Empty message'], ['AS-003', '401', 'Missing JWT']],
    },
  },
  {
    name: 'order-service', title: 'Order Service',
    brd: {
      purpose: 'The Order Service is the core domain service managing the full order lifecycle from creation through delivery. It implements a state machine and publishes Kafka events to drive the saga.',
      goals: ['Provide reliable order CRUD with state machine enforcement', 'Publish domain events to drive the distributed saga', 'Allow customers to view their own orders and admins to view all'],
      requirements: [
        { id: 'FR-OS-01', title: 'Order Lifecycle', items: ['Create order with line items and userId', 'Transition states: PENDING→VALIDATED→PAYMENT_INITIATED→PAID→SHIPPED→DELIVERED', 'Cancel from PENDING or VALIDATED only', 'Reject invalid transitions with 409'] },
        { id: 'FR-OS-02', title: 'Kafka Events', items: ['Publish OrderPlacedEvent on creation', 'Consume PaymentConfirmedEvent to advance to PAID', 'Publish OrderShippedEvent when admin marks shipped', 'Publish OrderCancelledEvent on cancellation'] },
      ],
      nfr: [['Consistency', 'Outbox pattern for reliable event publishing'], ['Idempotency', 'Duplicate event consumption is safe']],
      acceptance: ['Order creation returns 201 with orderId', 'Invalid state transition returns 409', 'OrderPlacedEvent appears in Kafka within 500ms'],
    },
    hld: {
      overview: 'Spring Boot 3.3 + Spring Data JPA on PostgreSQL. Spring State Machine for state transitions. Spring Kafka for event publishing and consumption. Outbox pattern for reliability.',
      endpoints: [['POST', '/orders', 'CUSTOMER JWT', 'Create new order'], ['GET', '/orders/my', 'CUSTOMER JWT', 'Get caller\'s orders'], ['GET', '/orders/{id}', 'CUSTOMER JWT', 'Get order by ID'], ['GET', '/orders', 'ADMIN JWT', 'Get all orders'], ['PATCH', '/orders/{id}/cancel', 'CUSTOMER JWT', 'Cancel order'], ['PATCH', '/orders/{id}/ship', 'ADMIN JWT', 'Mark as shipped']],
      deps: [['PostgreSQL', 'Internal', 'Order and outbox tables'], ['Kafka', 'Internal', 'Event publishing and consumption'], ['Keycloak', 'Internal', 'JWT validation via issuer URI']],
      kafka: [['oms.orders.placed', 'Produces', 'OrderPlacedEvent'], ['oms.payment.confirmed', 'Consumes', 'PaymentConfirmedEvent → advance to PAID'], ['oms.orders.shipped', 'Produces', 'OrderShippedEvent'], ['oms.orders.cancelled', 'Produces', 'OrderCancelledEvent']],
      dataModel: 'Tables: orders (id, user_id, status, total_amount, created_at), order_items (id, order_id, product_id, qty, unit_price), outbox_events (id, event_type, payload, published, created_at).',
      security: ['JWT required on all endpoints', '@PreAuthorize("hasRole(\'CUSTOMER\')") on /my', '@PreAuthorize("hasRole(\'ADMIN\')") on /orders (all)'],
    },
    lld: {
      packages: ['com.oms.order.domain     — Order, OrderItem, OrderStatus (enum)', 'com.oms.order.statemachine — OrderStateMachineConfig', 'com.oms.order.repository  — OrderRepository, OutboxRepository', 'com.oms.order.service     — OrderService, OutboxPublisher', 'com.oms.order.kafka       — OrderEventProducer, PaymentEventConsumer', 'com.oms.order.controller  — OrderController'],
      classes: [['OrderStateMachineConfig', 'Config', 'Defines all states, transitions, and guards'], ['OrderService', 'Service', 'Business logic — delegates transitions to state machine'], ['OutboxPublisher', 'Service', '@Scheduled job — publishes unpublished outbox events to Kafka'], ['OrderEventProducer', 'Kafka', 'Wraps KafkaTemplate for domain events']],
      schema: 'CREATE TABLE orders (id UUID PK, user_id VARCHAR, status VARCHAR, total_amount DECIMAL, created_at TIMESTAMP). CREATE TABLE outbox_events (id UUID PK, event_type VARCHAR, payload JSONB, published BOOLEAN, created_at TIMESTAMP).',
      config: [['spring.datasource.url', 'jdbc:postgresql://postgres:5432/orders', 'DB URL'], ['spring.kafka.bootstrap-servers', 'kafka:9092', 'Kafka broker'], ['keycloak.issuer', 'http://keycloak:8080/realms/oms', 'JWT issuer']],
      errors: [['OS-001', '404', 'Order not found'], ['OS-002', '409', 'Invalid state transition'], ['OS-003', '403', 'Not order owner']],
    },
  },
  {
    name: 'payment-service', title: 'Payment Service (Mock)',
    brd: {
      purpose: 'The Payment Service simulates a production-grade payment gateway for learning purposes. It implements idempotency, a transaction ledger, refunds, and a configurable failure rate.',
      goals: ['Teach idempotency pattern without a real payment gateway dependency', 'Maintain an accurate financial ledger per order', 'Support refunds for cancelled orders', 'Simulate async payment confirmation via Kafka'],
      requirements: [
        { id: 'FR-PS-01', title: 'Payment Flow', items: ['Initiate payment with orderId and amount', 'Idempotency key prevents duplicate charges', 'Async confirmation published to Kafka after configurable delay', 'Configurable failure rate (default 10%) for testing error paths'] },
        { id: 'FR-PS-02', title: 'Refunds', items: ['Refund endpoint reverses a confirmed payment', 'Ledger updated with REFUND entry', 'PaymentRefundedEvent published to Kafka'] },
      ],
      nfr: [['Idempotency', 'Same idempotency key always returns same result'], ['Auditability', 'Every transaction recorded in ledger']],
      acceptance: ['Two calls with same idempotency key return identical response', 'Failure rate produces ~10% failed payments in test runs'],
    },
    hld: {
      overview: 'Spring Boot 3.3 + Spring Data JPA. In-memory mock gateway bean with configurable delay and failure rate. Full ledger in PostgreSQL. Kafka for async confirmation events.',
      endpoints: [['POST', '/payments', 'CUSTOMER JWT', 'Initiate payment'], ['GET', '/payments/{id}', 'CUSTOMER JWT', 'Get payment status'], ['POST', '/payments/{id}/refund', 'ADMIN JWT', 'Refund payment']],
      deps: [['PostgreSQL', 'Internal', 'Payments and ledger tables'], ['Kafka', 'Internal', 'Publish confirmation events']],
      kafka: [['oms.payment.confirmed', 'Produces', 'PaymentConfirmedEvent after mock processing'], ['oms.payment.failed', 'Produces', 'PaymentFailedEvent on mock failure'], ['oms.orders.cancelled', 'Consumes', 'Triggers refund flow']],
      dataModel: 'Tables: payments (id, order_id, amount, status, idempotency_key, created_at), payment_ledger (id, payment_id, entry_type CHARGE/REFUND, amount, created_at).',
      security: ['JWT required on all endpoints', 'Idempotency-Key header required on POST /payments'],
    },
    lld: {
      packages: ['com.oms.payment.domain     — Payment, PaymentLedger, PaymentStatus', 'com.oms.payment.service    — PaymentService, MockGateway', 'com.oms.payment.kafka      — PaymentEventProducer, OrderCancelledConsumer', 'com.oms.payment.controller — PaymentController'],
      classes: [['MockGateway', 'Service', 'Simulates payment with configurable delay and failure rate'], ['PaymentService', 'Service', 'Idempotency check, ledger write, Kafka publish'], ['PaymentEventProducer', 'Kafka', 'Publishes PaymentConfirmedEvent and PaymentFailedEvent']],
      schema: 'CREATE TABLE payments (id UUID PK, order_id UUID, amount DECIMAL, status VARCHAR, idempotency_key VARCHAR UNIQUE, created_at TIMESTAMP). CREATE TABLE payment_ledger (id UUID PK, payment_id UUID FK, entry_type VARCHAR, amount DECIMAL, created_at TIMESTAMP).',
      config: [['mock.payment.failure-rate', '0.1', 'Fraction of payments that fail (0.0–1.0)'], ['mock.payment.delay-ms', '1000', 'Simulated processing delay in milliseconds']],
      errors: [['PS-001', '409', 'Duplicate idempotency key with different amount'], ['PS-002', '404', 'Payment not found'], ['PS-003', '422', 'Payment already refunded']],
    },
  },
  {
    name: 'product-service', title: 'Product Service (Catalog + RAG)',
    brd: {
      purpose: 'The Product Service manages the product catalog stored in MongoDB and enables semantic search via pgvector embeddings. It is publicly accessible for browsing without authentication.',
      goals: ['Serve product catalog to authenticated and guest users', 'Enable semantic search for the Product Agent using RAG', 'Generate and store vector embeddings for all products', 'Expose product data to the React UI and agent tools'],
      requirements: [
        { id: 'FR-PRS-01', title: 'Catalog', items: ['CRUD for products (ADMIN role)', 'Public read — no JWT required', 'Filter by category, price range, stock availability'] },
        { id: 'FR-PRS-02', title: 'RAG Search', items: ['Embedding job generates vector for each product on save', 'Similarity search endpoint accepts natural language query', 'Returns top-5 semantically similar products', 'pgvector cosine distance operator <=>'] },
      ],
      nfr: [['Search latency', '< 200ms for vector similarity search'], ['Embedding freshness', 'Re-embed on product update']],
      acceptance: ['Search "wireless headset for gaming" returns relevant products not matching keywords', 'New product embedding appears in pgvector within 5 seconds of save'],
    },
    hld: {
      overview: 'Spring Boot 3.3 + Spring Data MongoDB (catalog) + Spring AI EmbeddingClient (Anthropic) + pgvector via Spring AI VectorStore. Products saved in MongoDB; embeddings in PostgreSQL pgvector.',
      endpoints: [['GET', '/products', 'None', 'List all products (paginated)'], ['GET', '/products/{id}', 'None', 'Get product detail'], ['GET', '/products/search', 'None', 'Semantic search by query string'], ['POST', '/products', 'ADMIN JWT', 'Create product'], ['PUT', '/products/{id}', 'ADMIN JWT', 'Update product'], ['DELETE', '/products/{id}', 'ADMIN JWT', 'Delete product']],
      deps: [['MongoDB', 'Internal', 'Product document store'], ['PostgreSQL + pgvector', 'Internal', 'Embedding vector store'], ['Anthropic Embeddings API', 'External', 'Generate text-embedding-3-small vectors'], ['Kafka', 'Internal', 'ProductUpdatedEvent for embedding refresh']],
      kafka: [['oms.products.updated', 'Produces', 'ProductUpdatedEvent — triggers re-embedding']],
      dataModel: 'MongoDB: products collection {id, name, description, category, price, stockQty, imageUrl}. PostgreSQL: vector_store table (id UUID, content TEXT, metadata JSONB, embedding vector(1536)).',
      security: ['GET endpoints are public (permitAll in Gateway)', 'POST/PUT/DELETE require ADMIN JWT'],
    },
    lld: {
      packages: ['com.oms.product.domain     — Product (MongoDB document)', 'com.oms.product.repository — ProductRepository (MongoRepository), VectorStoreRepository', 'com.oms.product.service    — ProductService, EmbeddingService', 'com.oms.product.controller — ProductController'],
      classes: [['EmbeddingService', 'Service', 'Calls Spring AI EmbeddingClient, writes to VectorStore'], ['ProductService', 'Service', 'CRUD + triggers embedding on save/update'], ['VectorStoreRepository', 'Repository', 'Wraps Spring AI PgVectorStore for similarity search']],
      schema: 'MongoDB collection: products. PostgreSQL (pgvector): CREATE EXTENSION vector; CREATE TABLE vector_store (id UUID, content TEXT, metadata JSONB, embedding vector(1536)); CREATE INDEX ON vector_store USING ivfflat (embedding vector_cosine_ops).',
      config: [['spring.data.mongodb.uri', 'mongodb://mongodb:27017/products', 'MongoDB URI'], ['spring.ai.vectorstore.pgvector.dimensions', '1536', 'Embedding dimensions'], ['spring.ai.anthropic.embedding.model', 'text-embedding-3-small', 'Embedding model']],
      errors: [['PRS-001', '404', 'Product not found'], ['PRS-002', '400', 'Empty search query']],
    },
  },
];

// ═══════════════════════════════════════════════════════════════════════════
// WRITE ALL FILES
// ═══════════════════════════════════════════════════════════════════════════
async function writeDoc(doc, filePath) {
  const buffer = await Packer.toBuffer(doc);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, buffer);
  console.log('  wrote', filePath);
}

async function main() {
  const base = '/home/claude/oms-monorepo';

  console.log('Generating root documents...');
  await writeDoc(buildRootBRD(), `${base}/root-docs/OMS-BRD.docx`);
  await writeDoc(buildRootHLD(), `${base}/root-docs/OMS-HLD.docx`);
  await writeDoc(buildRootLLD(), `${base}/root-docs/OMS-LLD.docx`);

  for (const cfg of modules) {
    console.log(`Generating ${cfg.name} documents...`);
    const { brd, hld, lld } = buildModuleDocs(cfg);
    await writeDoc(brd, `${base}/${cfg.name}/docs/${cfg.name.toUpperCase()}-BRD.docx`);
    await writeDoc(hld, `${base}/${cfg.name}/docs/${cfg.name.toUpperCase()}-HLD.docx`);
    await writeDoc(lld, `${base}/${cfg.name}/docs/${cfg.name.toUpperCase()}-LLD.docx`);
  }

  console.log('All documents generated.');
}

main().catch(console.error);
