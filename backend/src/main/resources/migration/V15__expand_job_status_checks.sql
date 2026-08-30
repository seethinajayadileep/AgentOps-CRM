-- Hibernate ddl-auto historically created CHECK constraints for STRING enums.
-- Those constraints are not updated when new enum values are added.

ALTER TABLE public.businesses DROP CONSTRAINT IF EXISTS businesses_crawl_status_check;
ALTER TABLE public.businesses ADD CONSTRAINT businesses_crawl_status_check
    CHECK (crawl_status IN (
        'NOT_STARTED',
        'QUEUED',
        'CRAWLING',
        'IN_PROGRESS',
        'COMPLETED',
        'FAILED'
    ));

ALTER TABLE public.knowledge_base_jobs DROP CONSTRAINT IF EXISTS knowledge_base_jobs_status_check;
ALTER TABLE public.knowledge_base_jobs ADD CONSTRAINT knowledge_base_jobs_status_check
    CHECK (status IN (
        'QUEUED',
        'CRAWLING',
        'CHUNKING',
        'EMBEDDING',
        'INDEXING',
        'COMPLETED',
        'PARTIAL',
        'FAILED'
    ));
