-- StreamConverter PostgreSQL initialization script
-- This script sets up the database schema for configuration and metadata storage

CREATE SCHEMA IF NOT EXISTS streamconverter;

-- Configuration table for storing processing pipeline configurations
CREATE TABLE IF NOT EXISTS streamconverter.configurations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    pipeline_config JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Processing history table for tracking execution results
CREATE TABLE IF NOT EXISTS streamconverter.processing_history (
    id SERIAL PRIMARY KEY,
    configuration_id INTEGER REFERENCES streamconverter.configurations(id),
    execution_id VARCHAR(255) NOT NULL,
    input_size BIGINT,
    output_size BIGINT,
    processing_time_ms BIGINT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    metadata JSONB
);

-- Performance metrics table
CREATE TABLE IF NOT EXISTS streamconverter.performance_metrics (
    id SERIAL PRIMARY KEY,
    execution_id VARCHAR(255) NOT NULL,
    command_name VARCHAR(255) NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    memory_used_mb BIGINT,
    input_bytes BIGINT,
    output_bytes BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_configurations_name ON streamconverter.configurations(name);
CREATE INDEX IF NOT EXISTS idx_configurations_active ON streamconverter.configurations(is_active);
CREATE INDEX IF NOT EXISTS idx_processing_history_execution_id ON streamconverter.processing_history(execution_id);
CREATE INDEX IF NOT EXISTS idx_processing_history_status ON streamconverter.processing_history(status);
CREATE INDEX IF NOT EXISTS idx_processing_history_started_at ON streamconverter.processing_history(started_at);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_execution_id ON streamconverter.performance_metrics(execution_id);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON streamconverter.performance_metrics(timestamp);

-- Insert sample configurations
INSERT INTO streamconverter.configurations (name, description, pipeline_config) VALUES
('csv_to_json', 'Convert CSV data to JSON format',
 '{"commands": [{"type": "CsvNavigateCommand", "config": {"columns": ["*"]}}, {"type": "JsonConvertCommand"}]}'),
('api_integration', 'Send data to external API and process response',
 '{"commands": [{"type": "SendHttpCommand", "config": {"url": "http://api.example.com", "method": "POST"}}, {"type": "JsonNavigateCommand", "config": {"path": "$.result"}}]}'),
('xml_validation', 'Validate and transform XML data',
 '{"commands": [{"type": "ValidateCommand", "config": {"schema": "schema.xsd"}}, {"type": "xml.ConvertCommand", "config": {"xslt": "transform.xsl"}}]}')
ON CONFLICT (name) DO NOTHING;

-- Grant permissions to streamconverter user
GRANT USAGE ON SCHEMA streamconverter TO streamconverter;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA streamconverter TO streamconverter;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA streamconverter TO streamconverter;