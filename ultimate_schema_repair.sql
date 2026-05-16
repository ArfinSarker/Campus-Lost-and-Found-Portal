-- =========================================================================
-- CAMPUS LOST AND FOUND: ULTIMATE SCHEMA REPAIR & ALIGNMENT
-- This script aligns the schema without dropping data.
-- =========================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. COUNTERS TABLE
CREATE TABLE IF NOT EXISTS public.counters (
    counter_name TEXT PRIMARY KEY,
    count BIGINT DEFAULT 0
);

INSERT INTO public.counters (counter_name, count)
VALUES ('lost_items', 0), ('found_items', 0), ('admin_reports', 0)
ON CONFLICT (counter_name) DO NOTHING;

-- 3. PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    university_id TEXT PRIMARY KEY,
    auth_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    display_name TEXT,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    user_type TEXT NOT NULL CHECK (user_type IN ('Student', 'Staff', 'Admin')),
    department TEXT,
    batch TEXT,
    level_term TEXT,
    designation TEXT,
    profile_image_url TEXT,
    gender TEXT,
    section TEXT,
    request_status TEXT DEFAULT 'approved',
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure profiles columns exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'role') THEN
        ALTER TABLE public.profiles ADD COLUMN role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin'));
    END IF;
END $$;

-- 4. ADMIN REQUESTS TABLE
CREATE TABLE IF NOT EXISTS public.admin_requests (
    university_id TEXT PRIMARY KEY,
    auth_id UUID UNIQUE,
    full_name TEXT NOT NULL,
    display_name TEXT,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    designation TEXT,
    department TEXT,
    verification_code TEXT,
    profile_image_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'denied')),
    password TEXT,
    user_type TEXT DEFAULT 'Admin',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure admin_requests columns exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_requests' AND column_name = 'password') THEN
        ALTER TABLE public.admin_requests ADD COLUMN password TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_requests' AND column_name = 'user_type') THEN
        ALTER TABLE public.admin_requests ADD COLUMN user_type TEXT DEFAULT 'Admin';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_requests' AND column_name = 'display_name') THEN
        ALTER TABLE public.admin_requests ADD COLUMN display_name TEXT;
    END IF;
END $$;

-- 5. REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    type TEXT NOT NULL CHECK (type IN ('lost', 'found')),
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    location TEXT,
    manual_location TEXT,
    date_occurred TEXT,
    time_occurred TEXT,
    image_urls TEXT[] DEFAULT '{}',
    image_url TEXT,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'resolved', 'deleted')),
    admin_status TEXT DEFAULT 'Pending',
    claimed_by_id TEXT REFERENCES public.profiles(university_id),
    hidden_identification_question TEXT,
    additional_location_details TEXT,
    proof_of_ownership_detail TEXT,
    item_handling_status TEXT,
    authority_name TEXT,
    office_room_number TEXT,
    preferred_contact_method TEXT,
    is_edited BOOLEAN DEFAULT FALSE,
    deleted_by_user BOOLEAN DEFAULT FALSE,
    user_id UUID,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure reports columns exist
DO $$
BEGIN
    -- Fix date_occurred type if it's still DATE
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'date_occurred' AND data_type = 'date') THEN
        -- Must drop dependent views first
        DROP VIEW IF EXISTS public.lost_reports CASCADE;
        DROP VIEW IF EXISTS public.found_reports CASCADE;

        ALTER TABLE public.reports ALTER COLUMN date_occurred TYPE TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'additional_location_details') THEN
        ALTER TABLE public.reports ADD COLUMN additional_location_details TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'proof_of_ownership_detail') THEN
        ALTER TABLE public.reports ADD COLUMN proof_of_ownership_detail TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'item_handling_status') THEN
        ALTER TABLE public.reports ADD COLUMN item_handling_status TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'authority_name') THEN
        ALTER TABLE public.reports ADD COLUMN authority_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'office_room_number') THEN
        ALTER TABLE public.reports ADD COLUMN office_room_number TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'image_url') THEN
        ALTER TABLE public.reports ADD COLUMN image_url TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'user_id') THEN
        ALTER TABLE public.reports ADD COLUMN user_id UUID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reports' AND column_name = 'timestamp') THEN
        ALTER TABLE public.reports ADD COLUMN timestamp BIGINT;
    END IF;
END $$;

-- 5.1 COMPATIBILITY VIEWS
CREATE OR REPLACE VIEW public.lost_reports AS
SELECT * FROM public.reports WHERE type = 'lost';

CREATE OR REPLACE VIEW public.found_reports AS
SELECT * FROM public.reports WHERE type = 'found';

-- 6. ADMIN REPORTS
CREATE TABLE IF NOT EXISTS public.admin_reports (
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
    image_url TEXT,
    deleted_by_user BOOLEAN DEFAULT FALSE,
    timestamp BIGINT,
    user_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure admin_reports columns exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'image_url') THEN
        ALTER TABLE public.admin_reports ADD COLUMN image_url TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'user_id') THEN
        ALTER TABLE public.admin_reports ADD COLUMN user_id UUID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'timestamp') THEN
        ALTER TABLE public.admin_reports ADD COLUMN timestamp BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'reporter_auth_id') THEN
        ALTER TABLE public.admin_reports ADD COLUMN reporter_auth_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'phone') THEN
        ALTER TABLE public.admin_reports ADD COLUMN phone TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admin_reports' AND column_name = 'reporter_name') THEN
        ALTER TABLE public.admin_reports ADD COLUMN reporter_name TEXT;
    END IF;
END $$;

-- 7. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    sender_id TEXT REFERENCES public.profiles(university_id) ON DELETE SET NULL,
    report_id UUID REFERENCES public.reports(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    user_id UUID,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure notifications columns exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'user_id') THEN
        ALTER TABLE public.notifications ADD COLUMN user_id UUID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'timestamp') THEN
        ALTER TABLE public.notifications ADD COLUMN timestamp BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'additional_details') THEN
        ALTER TABLE public.notifications ADD COLUMN additional_details TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'sender_name') THEN
        ALTER TABLE public.notifications ADD COLUMN sender_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'sender_phone') THEN
        ALTER TABLE public.notifications ADD COLUMN sender_phone TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'sender_email') THEN
        ALTER TABLE public.notifications ADD COLUMN sender_email TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'sender_image_url') THEN
        ALTER TABLE public.notifications ADD COLUMN sender_image_url TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'item_name') THEN
        ALTER TABLE public.notifications ADD COLUMN item_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'claimer_id') THEN
        ALTER TABLE public.notifications ADD COLUMN claimer_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'claimer_name') THEN
        ALTER TABLE public.notifications ADD COLUMN claimer_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'item_id') THEN
        ALTER TABLE public.notifications ADD COLUMN item_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'claim_type') THEN
        ALTER TABLE public.notifications ADD COLUMN claim_type TEXT;
    END IF;
END $$;

-- 8. SECURITY & UTILITY FUNCTIONS
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE auth_id = auth.uid() AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION public.increment_counter(p_counter_name TEXT)
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
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.generate_display_id()
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
    NEW.timestamp := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.timestamp := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 9. TRIGGERS (Drop first to avoid duplication errors)
DROP TRIGGER IF EXISTS tr_reports_display_id ON public.reports;
CREATE TRIGGER tr_reports_display_id BEFORE INSERT ON public.reports FOR EACH ROW EXECUTE FUNCTION public.generate_display_id();

DROP TRIGGER IF EXISTS tr_admin_reports_display_id ON public.admin_reports;
CREATE TRIGGER tr_admin_reports_display_id BEFORE INSERT ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION public.generate_display_id();

DROP TRIGGER IF EXISTS tr_notifications_timestamp ON public.notifications;
CREATE TRIGGER tr_notifications_timestamp BEFORE INSERT ON public.notifications FOR EACH ROW EXECUTE FUNCTION public.set_timestamp();

DROP TRIGGER IF EXISTS update_profiles_timestamp ON public.profiles;
CREATE TRIGGER update_profiles_timestamp BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

DROP TRIGGER IF EXISTS update_reports_timestamp ON public.reports;
CREATE TRIGGER update_reports_timestamp BEFORE UPDATE ON public.reports FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

DROP TRIGGER IF EXISTS update_admin_reports_timestamp ON public.admin_reports;
CREATE TRIGGER update_admin_reports_timestamp BEFORE UPDATE ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

-- 10. ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- 11. POLICIES (Drop first to update)
DO $$
BEGIN
    -- Profiles
    DROP POLICY IF EXISTS "Profiles selection" ON public.profiles;
    CREATE POLICY "Profiles selection" ON public.profiles FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Profiles update own" ON public.profiles;
    CREATE POLICY "Profiles update own" ON public.profiles FOR UPDATE USING (auth.uid() = auth_id OR public.is_admin());
    DROP POLICY IF EXISTS "Profiles insert system" ON public.profiles;
    CREATE POLICY "Profiles insert system" ON public.profiles FOR INSERT WITH CHECK (true);

    -- Reports
    DROP POLICY IF EXISTS "Reports view" ON public.reports;
    CREATE POLICY "Reports view" ON public.reports FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Reports insert" ON public.reports;
    CREATE POLICY "Reports insert" ON public.reports FOR INSERT TO authenticated WITH CHECK (true);
    DROP POLICY IF EXISTS "Reports update" ON public.reports;
    CREATE POLICY "Reports update" ON public.reports FOR UPDATE USING (auth.uid() = user_id OR public.is_admin());

    -- Admin Requests
    DROP POLICY IF EXISTS "Admin requests selection" ON public.admin_requests;
    CREATE POLICY "Admin requests selection" ON public.admin_requests FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Admin requests insert" ON public.admin_requests;
    CREATE POLICY "Admin requests insert" ON public.admin_requests FOR INSERT WITH CHECK (true);
    DROP POLICY IF EXISTS "Admin requests manage" ON public.admin_requests;
    CREATE POLICY "Admin requests manage" ON public.admin_requests FOR ALL USING (public.is_admin());
    DROP POLICY IF EXISTS "Admin requests delete" ON public.admin_requests;
    CREATE POLICY "Admin requests delete" ON public.admin_requests FOR DELETE USING (public.is_admin());

    -- Admin Reports
    DROP POLICY IF EXISTS "Admin reports manage" ON public.admin_reports;
    CREATE POLICY "Admin reports manage" ON public.admin_reports FOR ALL USING (public.is_admin());
    DROP POLICY IF EXISTS "Admin reports insert" ON public.admin_reports;
    CREATE POLICY "Admin reports insert" ON public.admin_reports FOR INSERT WITH CHECK (true);

    -- Notifications
    DROP POLICY IF EXISTS "Notifications view own" ON public.notifications;
    CREATE POLICY "Notifications view own" ON public.notifications FOR SELECT USING (user_id = auth.uid());
    DROP POLICY IF EXISTS "Notifications insert" ON public.notifications;
    CREATE POLICY "Notifications insert" ON public.notifications FOR INSERT TO authenticated WITH CHECK (true);
END $$;

-- 12. PERMISSIONS
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 13. DATA MIGRATION
UPDATE public.reports SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;
UPDATE public.admin_reports SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;
UPDATE public.notifications SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;

-- 14. FORCE API REFRESH
NOTIFY pgrst, 'reload schema';
