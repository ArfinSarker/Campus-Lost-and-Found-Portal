-- =========================================================================
-- CAMPUS LOST AND FOUND: FIX MISSING PASSWORD COLUMN
-- This script safely adds the 'password' column to profiles and admin_requests
-- =========================================================================

BEGIN;

-- 1. Add password column to public.profiles if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='profiles' AND column_name='password') THEN
        ALTER TABLE public.profiles ADD COLUMN password TEXT;
        RAISE NOTICE 'Added password column to profiles table.';
    ELSE
        RAISE NOTICE 'password column already exists in profiles table.';
    END IF;
END $$;

-- 2. Add password column to public.admin_requests if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='admin_requests' AND column_name='password') THEN
        ALTER TABLE public.admin_requests ADD COLUMN password TEXT;
        RAISE NOTICE 'Added password column to admin_requests table.';
    ELSE
        RAISE NOTICE 'password column already exists in admin_requests table.';
    END IF;
END $$;

COMMIT;

-- Reload PostgREST schema cache to ensure changes take effect immediately
NOTIFY pgrst, 'reload schema';
