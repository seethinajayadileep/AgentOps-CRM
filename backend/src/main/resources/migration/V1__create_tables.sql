-- AgentOps CRM - Database Schema V1
-- Phase 1: Database Foundation
-- Creates all tables for the CRM system

-- Businesses table
CREATE TABLE businesses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    website_url VARCHAR(500) NOT NULL,
    industry VARCHAR(100),
    description TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    crawl_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Documents table (crawled web pages)
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    url VARCHAR(1000) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    metadata_json JSONB,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Knowledge chunks table (for RAG)
CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    document_id UUID,
    content TEXT NOT NULL,
    source_url VARCHAR(1000),
    vector_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_knowledge_chunks_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
);

-- Conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    customer_phone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversations_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Leads table
CREATE TABLE leads (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    conversation_id UUID,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    requirement_text TEXT,
    budget NUMERIC(15, 2),
    urgency VARCHAR(20),
    timeline VARCHAR(50),
    lead_score NUMERIC(5, 2),
    summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_leads_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL
);

-- Agent logs table
CREATE TABLE agent_logs (
    id UUID PRIMARY KEY,
    business_id UUID,
    conversation_id UUID,
    lead_id UUID,
    agent_name VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    input_json TEXT,
    output_json TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agent_logs_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_logs_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    CONSTRAINT fk_agent_logs_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE SET NULL
);

-- Approvals table
CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    lead_id UUID,
    approval_type VARCHAR(30) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(255),
    review_comment TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_approvals_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_approvals_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE SET NULL
);

-- Voice calls table
CREATE TABLE voice_calls (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    lead_id UUID,
    conversation_id UUID,
    vapi_call_id VARCHAR(255),
    phone_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    transcript TEXT,
    summary TEXT,
    recording_url VARCHAR(1000),
    duration_seconds INTEGER,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_voice_calls_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_voice_calls_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE SET NULL,
    CONSTRAINT fk_voice_calls_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX idx_businesses_website_url ON businesses(website_url);
CREATE INDEX idx_businesses_crawl_status ON businesses(crawl_status);
CREATE INDEX idx_documents_business_id ON documents(business_id);
CREATE INDEX idx_documents_url ON documents(url);
CREATE INDEX idx_knowledge_chunks_business_id ON knowledge_chunks(business_id);
CREATE INDEX idx_knowledge_chunks_document_id ON knowledge_chunks(document_id);
CREATE INDEX idx_conversations_business_id ON conversations(business_id);
CREATE INDEX idx_conversations_customer_email ON conversations(customer_email);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_leads_business_id ON leads(business_id);
CREATE INDEX idx_leads_conversation_id ON leads(conversation_id);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_lead_score ON leads(lead_score);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_agent_logs_business_id ON agent_logs(business_id);
CREATE INDEX idx_agent_logs_conversation_id ON agent_logs(conversation_id);
CREATE INDEX idx_agent_logs_lead_id ON agent_logs(lead_id);
CREATE INDEX idx_agent_logs_agent_name ON agent_logs(agent_name);
CREATE INDEX idx_agent_logs_status ON agent_logs(status);
CREATE INDEX idx_agent_logs_created_at ON agent_logs(created_at);
CREATE INDEX idx_approvals_business_id ON approvals(business_id);
CREATE INDEX idx_approvals_lead_id ON approvals(lead_id);
CREATE INDEX idx_approvals_status ON approvals(status);
CREATE INDEX idx_approvals_created_at ON approvals(created_at);
CREATE INDEX idx_voice_calls_business_id ON voice_calls(business_id);
CREATE INDEX idx_voice_calls_lead_id ON voice_calls(lead_id);
CREATE INDEX idx_voice_calls_conversation_id ON voice_calls(conversation_id);
CREATE INDEX idx_voice_calls_status ON voice_calls(status);
CREATE INDEX idx_voice_calls_started_at ON voice_calls(started_at);