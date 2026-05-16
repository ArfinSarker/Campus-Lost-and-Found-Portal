-- Ensure counters table exists
CREATE TABLE IF NOT EXISTS public.counters (
    "counter_name" TEXT PRIMARY KEY,
    "count" BIGINT DEFAULT 0
);

-- Ensure increment_counter function exists
CREATE OR REPLACE FUNCTION increment_counter(p_counter_name TEXT)
RETURNS BIGINT AS $$
DECLARE
    new_count BIGINT;
BEGIN
    INSERT INTO public.counters (counter_name, count)
    VALUES (p_counter_name, 1)
    ON CONFLICT (counter_name)
    DO UPDATE SET count = public.counters.count + 1
    RETURNING count INTO new_count;

    RETURN new_count;
END;
$$ LANGUAGE plpgsql;
