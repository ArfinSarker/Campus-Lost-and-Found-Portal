-- =========================================================================
-- CAMPUS LOST AND FOUND: FRESH AUTHENTICATION & PROFILE SYSTEM SETUP
-- =========================================================================

-- 1. CLEANUP PREVIOUS STRUCTURES (Aggressive Reset)
-- Drop all related triggers, functions, and tables
DROP TRIGGER IF EXISTS tr_reports_display_id ON public.reports;
DROP TRIGGER IF EXISTS tr_admin_reports_display_id ON public.admin_reports;
DROP TRIGGER IF EXISTS update_reports_updated_at ON public.reports;
DROP TRIGGER IF EXISTS update_admin_reports_updated_at ON public.admin_reports;

DROP FUNCTION IF EXISTS is_admin() CASCADE;
DROP FUNCTION IF EXISTS generate_display_id() CASCADE;
DROP FUNCTION IF EXISTS increment_counter(text) CASCADE;
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS safe_rename_column(text, text, text) CASCADE;

-- Drop tables (excluding system tables)
DROP TABLE IF EXISTS public.admin_requests CASCADE;
DROP TABLE IF EXISTS public.notifications CASCADE;
DROP TABLE IF EXISTS public.admin_reports CASCADE;
DROP TABLE IF EXISTS public.reports CASCADE;
DROP TABLE IF EXISTS public.profiles CASCADE;
DROP TABLE IF EXISTS public.counters CASCADE;

-- 2. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 3. PROFILES TABLE (The Primary User Record)
CREATE TABLE public.profiles (
    university_id TEXT PRIMARY KEY,                       -- The key used for login lookup
    auth_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE, -- Link to Supabase Auth
    full_name TEXT NOT NULL,
    display_name TEXT,                                   -- Added for UI flexibility
    email TEXT UNIQUE NOT NULL,                           -- Sync with auth.users
    phone_number TEXT,
    user_type TEXT NOT NULL CHECK (user_type IN ('Student', 'Staff', 'Admin')),
    department TEXT,
    batch TEXT,                                          -- Students
    level_term TEXT,                                     -- Students
    designation TEXT,                                    -- Staff/Admin
    profile_image_url TEXT,
    gender TEXT,
    section TEXT,                                        -- Students
    request_status TEXT DEFAULT 'approved',              -- For registration flow
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexing for fast login lookup
CREATE INDEX idx_profiles_email ON public.profiles(email);
CREATE INDEX idx_profiles_auth_id ON public.profiles(auth_id);

-- 4. ADMIN REQUESTS TABLE (Pending Admin Approvals)
CREATE TABLE public.admin_requests (
    university_id TEXT PRIMARY KEY,
    auth_id UUID UNIQUE,                                 -- Created during signup but waiting for role
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    designation TEXT,
    department TEXT,
    verification_code TEXT,                              -- Admin secret code if used
    profile_image_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'denied')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. REPORTS TABLE
CREATE TABLE public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    type TEXT NOT NULL CHECK (type IN ('lost', 'found')),
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    location TEXT,
    manual_location TEXT,
    date_occurred DATE,
    time_occurred TEXT,
    image_urls TEXT[] DEFAULT '{}',
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'resolved', 'deleted')),
    admin_status TEXT DEFAULT 'Pending',
    claimed_by_id TEXT REFERENCES public.profiles(university_id),
    hidden_identification_question TEXT,
    preferred_contact_method TEXT,
    is_edited BOOLEAN DEFAULT FALSE,
    deleted_by_user BOOLEAN DEFAULT FALSE,
    user_id UUID, -- For direct RLS if needed
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. ADMIN REPORTS (Complaints/Issues)
CREATE TABLE public.admin_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    related_item_id TEXT,
    priority TEXT DEFAULT 'Medium',
    status TEXT DEFAULT 'Pending',
    admin_note TEXT,
    image_urls TEXT[] DEFAULT '{}',
    deleted_by_user BOOLEAN DEFAULT FALSE,
    timestamp BIGINT, -- Compatibility with old app code
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. NOTIFICATIONS
CREATE TABLE public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    sender_id TEXT REFERENCES public.profiles(university_id) ON DELETE SET NULL,
    report_id UUID REFERENCES public.reports(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    user_id UUID, -- Recipient's Auth ID for RLS
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. COUNTERS TABLE (For ID Generation like L1, F1)
CREATE TABLE public.counters (
    counter_name TEXT PRIMARY KEY,
    count BIGINT DEFAULT 0
);

INSERT INTO public.counters (counter_name, count)
VALUES ('lost_items', 0), ('found_items', 0), ('admin_reports', 0)
ON CONFLICT (counter_name) DO NOTHING;

-- 6. FUNCTIONS & TRIGGERS

-- A. Admin check function
CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE auth_id = auth.uid() AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- B. Display ID Generator
CREATE OR REPLACE FUNCTION generate_display_id()
RETURNS TRIGGER AS $$
DECLARE
    prefix TEXT;
    counter_name_var TEXT;
    new_count BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'reports' THEN
        IF NEW.type = 'lost' THEN prefix := 'L'; counter_name_var := 'lost_items';
        ELSE prefix := 'F'; counter_name_var := 'found_items'; END IF;
    ELSIF TG_TABLE_NAME = 'admin_reports' THEN
        prefix := 'R'; counter_name_var := 'admin_reports';
    END IF;

    UPDATE public.counters SET count = count + 1 WHERE counter_name = counter_name_var RETURNING count INTO new_count;
    NEW.display_id := prefix || new_count;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- C. Update timestamp function
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 10. ROW LEVEL SECURITY (RLS)

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Enable public lookup" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = auth_id OR is_admin());
CREATE POLICY "System can insert profiles" ON public.profiles FOR INSERT WITH CHECK (true);

-- Reports Policies
CREATE POLICY "Enable view for all" ON public.reports FOR SELECT USING (true);
CREATE POLICY "Enable insert for authenticated" ON public.reports FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Enable update for owner or admin" ON public.reports FOR UPDATE USING (is_admin() OR auth.uid() IN (SELECT auth_id FROM public.profiles WHERE university_id = reporter_id));

-- Admin Reports Policies
CREATE POLICY "Enable public insert" ON public.admin_reports FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can manage" ON public.admin_reports FOR ALL USING (is_admin());

-- Notifications Policies
CREATE POLICY "Users can view own" ON public.notifications FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "Authenticated can insert" ON public.notifications FOR INSERT TO authenticated WITH CHECK (true);

-- 11. TRIGGERS

-- Display ID Triggers
CREATE TRIGGER tr_reports_display_id BEFORE INSERT ON public.reports FOR EACH ROW EXECUTE FUNCTION generate_display_id();
CREATE TRIGGER tr_admin_reports_display_id BEFORE INSERT ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION generate_display_id();

-- Timestamp Triggers
CREATE TRIGGER update_profiles_timestamp BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION update_timestamp();
CREATE TRIGGER update_reports_timestamp BEFORE UPDATE ON public.reports FOR EACH ROW EXECUTE FUNCTION update_timestamp();
CREATE TRIGGER update_admin_reports_timestamp BEFORE UPDATE ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- 12. INITIAL ADMIN ACCOUNT ACTIVATION
-- Run this block after creating the Auth account in the UI or via script
DO $$
DECLARE
  target_auth_id UUID;
BEGIN
  -- Look for your existing Auth user
  SELECT id INTO target_auth_id FROM auth.users WHERE email = 'm.shamsularfinsarkernayan@gmail.com';

  IF target_auth_id IS NOT NULL THEN
    INSERT INTO public.profiles (
        university_id, auth_id, full_name, display_name, email, phone_number,
        user_type, role, request_status, designation, department
    )
    VALUES (
        '0802410205101019', target_auth_id, 'Md. Shamsul Arfin Sarker', 'Md. Shamsul Arfin Sarker',
        'm.shamsularfinsarkernayan@gmail.com', '01819966626', 'Admin', 'admin', 'approved', 'Main Admin', 'CSE'
    )
    ON CONFLICT (university_id) DO UPDATE SET
        role = 'admin',
        request_status = 'approved',
        auth_id = EXCLUDED.auth_id;
  END IF;
END $$;

-- 9. PERMISSIONS
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 10. REFRESH SCHEMA CACHE
NOTIFY pgrst, 'reload schema';
