-- =========================================================================
-- CAMPUS LOST AND FOUND: MASTER DATABASE REPAIR & ALIGNMENT
-- This script fixes "Database error querying schema" and enables login.
-- =========================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. HELPER FUNCTION FOR SAFE RENAMING
CREATE OR REPLACE FUNCTION safe_rename_column(tbl TEXT, old_col TEXT, new_col TEXT)
RETURNS VOID AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = tbl AND column_name = old_col
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = tbl AND column_name = new_col
    ) THEN
        EXECUTE format('ALTER TABLE public.%I RENAME COLUMN %I TO %I', tbl, old_col, new_col);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 3. ALIGN PROFILES TABLE (Matches User.java and UserLoginActivity.java)
SELECT safe_rename_column('profiles', 'userId', 'university_id');
SELECT safe_rename_column('profiles', 'authId', 'auth_id');
SELECT safe_rename_column('profiles', 'fullName', 'full_name');
SELECT safe_rename_column('profiles', 'phoneNumber', 'phone_number');
SELECT safe_rename_column('profiles', 'userType', 'user_type');
SELECT safe_rename_column('profiles', 'requestStatus', 'request_status');
SELECT safe_rename_column('profiles', 'profileImageUrl', 'profile_image_url');
SELECT safe_rename_column('profiles', 'levelTerm', 'level_term');

-- Ensure display_name exists and is nullable
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'display_name') THEN
        ALTER TABLE public.profiles ADD COLUMN display_name TEXT;
    END IF;
    ALTER TABLE public.profiles ALTER COLUMN display_name DROP NOT NULL;
END $$;

-- 4. ALIGN OTHER TABLES (For consistency across the app)
SELECT safe_rename_column('lost_reports', 'displayId', 'display_id');
SELECT safe_rename_column('lost_reports', 'userId', 'reporter_id');
SELECT safe_rename_column('lost_reports', 'name', 'item_name');

SELECT safe_rename_column('found_reports', 'displayId', 'display_id');
SELECT safe_rename_column('found_reports', 'userId', 'reporter_id');
SELECT safe_rename_column('found_reports', 'name', 'item_name');

SELECT safe_rename_column('admin_reports', 'reportId', 'id');
SELECT safe_rename_column('admin_reports', 'displayId', 'display_id');
SELECT safe_rename_column('admin_reports', 'universityId', 'reporter_id');

SELECT safe_rename_column('admin_requests', 'universityId', 'university_id');
SELECT safe_rename_column('admin_requests', 'fullName', 'full_name');

-- 5. ROW LEVEL SECURITY (RLS) FIX
-- Allow public selection of profiles so the app can find email by university_id during login
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Profiles viewable by anyone" ON public.profiles;
DROP POLICY IF EXISTS "Profiles viewable by authenticated" ON public.profiles;
DROP POLICY IF EXISTS "Enable login lookup" ON public.profiles;
DROP POLICY IF EXISTS "Public lookup" ON public.profiles;

CREATE POLICY "Public lookup" ON public.profiles FOR SELECT USING (true);

-- 6. INSERT/REPAIR ADMIN USER
DO $$
DECLARE
  target_auth_id UUID;
BEGIN
  -- Find the auth ID for your email (Assuming you already signed up/created via earlier scripts)
  SELECT id INTO target_auth_id FROM auth.users WHERE email = 'm.shamsularfinsarkernayan@gmail.com';

  INSERT INTO public.profiles (
    university_id, full_name, display_name, email, phone_number, user_type, role, request_status, auth_id
  )
  VALUES (
    '0802410205101019', 'Md. Shamsul Arfin Sarker', 'Md. Shamsul Arfin Sarker',
    'm.shamsularfinsarkernayan@gmail.com', '01819966626', 'Admin', 'admin', 'approved', target_auth_id
  )
  ON CONFLICT (university_id) DO UPDATE SET
    role = 'admin',
    user_type = 'Admin',
    request_status = 'approved',
    display_name = EXCLUDED.display_name,
    auth_id = EXCLUDED.auth_id;
END $$;

-- 7. CLEAN UP
DROP FUNCTION IF EXISTS safe_rename_column(TEXT, TEXT, TEXT);

-- 8. FORCE SCHEMA REFRESH (Solves "Database error querying schema")
NOTIFY pgrst, 'reload schema';
