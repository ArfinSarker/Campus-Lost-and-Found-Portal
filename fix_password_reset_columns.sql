-- =========================================================================
-- CAMPUS LOST AND FOUND: PASSIVE SECURITY FOR PASSWORD RESET
-- Ensures columns exist and RLS is wide open for testing.
-- =========================================================================

BEGIN;

-- 1. Ensure columns exist
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS reset_token TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMPTZ;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS reset_token_used BOOLEAN DEFAULT FALSE;

-- 2. Wide open policy for selection (Rule out RLS issues)
DROP POLICY IF EXISTS "Allow anonymous reset token check" ON public.profiles;
DROP POLICY IF EXISTS "Profiles selection" ON public.profiles;
CREATE POLICY "Profiles selection" ON public.profiles FOR SELECT USING (true);

-- 3. Ensure permissions are granted to the anon role
GRANT USAGE ON SCHEMA public TO anon;
GRANT SELECT, UPDATE ON public.profiles TO anon;

COMMIT;

NOTIFY pgrst, 'reload schema';
