-- Create the 'healenium' schema that the Healenium backend expects internally
CREATE SCHEMA IF NOT EXISTS healenium;
GRANT ALL ON SCHEMA healenium TO healenium_user;

-- Set search_path so ALL connections to this DB default to the healenium schema
ALTER DATABASE healenium SET search_path TO healenium,public;
