-- name: select-file
SELECT filename, content FROM file WHERE id = :id

-- name: upsert-file<!
INSERT INTO file (id, filename, content) VALUES (:id, :filename, :content) ON CONFLICT (id) DO UPDATE SET filename = :filename, content = :content RETURNING id
