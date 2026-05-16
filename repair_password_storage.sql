-- =========================================================================
-- CAMPUS LOST AND FOUND: REPAIR PASSWORD STORAGE (FIXED VERSION)
-- This script ensures the 'password' column is fully operational.
-- =========================================================================

BEGIN;

-- 1. Ensure the password column exists in profiles
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='profiles' AND column_name='password') THEN
        ALTER TABLE public.profiles ADD COLUMN password TEXT;
        RAISE NOTICE 'Added missing password column to profiles.';
    ELSE
        RAISE NOTICE 'password column already exists in profiles.';
    END IF;
END $$;

-- 2. Ensure the password column exists in admin_requests
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_requests' AND column_name='password') THEN
        ALTER TABLE public.admin_requests ADD COLUMN password TEXT;
        RAISE NOTICE 'Added missing password column to admin_requests.';
    ELSE
        RAISE NOTICE 'password column already exists in admin_requests.';
    END IF;
END $$;

-- 3. Check for and disable any security triggers that might clear passwords
DO $$
DECLARE
    trig_record RECORD;
BEGIN
    FOR trig_record IN
        SELECT trigger_name, event_object_table
        FROM information_schema.triggers
        WHERE event_object_table IN ('profiles', 'admin_requests')
        AND trigger_name ILIKE '%clear_password%'
    LOOP
        EXECUTE 'DROP TRIGGER IF EXISTS ' || trig_record.trigger_name || ' ON ' || trig_record.event_object_table;
        RAISE NOTICE 'Dropped interfering trigger: % on table %', trig_record.trigger_name, trig_record.event_object_table;
    END LOOP;
END $$;

COMMIT;

-- 4. CRITICAL: Force PostgREST to reload the schema cache.
NOTIFY pgrst, 'reload schema';

-- Notice moved inside a DO block to prevent syntax error
DO $$
BEGIN
    RAISE NOTICE 'Password storage repair completed. Please test registration now.';
END $$;
