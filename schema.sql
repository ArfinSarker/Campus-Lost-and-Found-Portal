-- SQL Schema for Campus Lost and Found
-- Designed for Supabase (PostgreSQL)

-- Necessary extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Custom Types
DO $$ BEGIN
    CREATE TYPE report_type AS ENUM ('lost', 'found');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE report_status AS ENUM ('active', 'resolved', 'deleted');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE priority_level AS ENUM ('Low', 'Medium', 'High');
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 2. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    university_id TEXT PRIMARY KEY,
    auth_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    user_type TEXT CHECK (user_type IN ('Student', 'Staff', 'Admin')),
    department TEXT,
    batch TEXT,
    level_term TEXT,
    designation TEXT,
    profile_image_url TEXT,
    gender TEXT,
    section TEXT,
    request_status TEXT DEFAULT 'approved',
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 3. Reports Table
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    type report_type NOT NULL,
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    location TEXT,
    manual_location TEXT,
    additional_location_details TEXT,
    date_occurred DATE,
    time_occurred TEXT,
    image_urls TEXT[] DEFAULT '{}',
    status report_status DEFAULT 'active',
    admin_status TEXT DEFAULT 'Pending',
    claimed_by_id TEXT REFERENCES public.profiles(university_id),
    proof_of_ownership_detail TEXT,
    hidden_identification_question TEXT,
    item_handling_status TEXT,
    authority_name TEXT,
    office_room_number TEXT,
    preferred_contact_method TEXT,
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

-- 4. Admin Reports (Complaints)
CREATE TABLE IF NOT EXISTS public.admin_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    related_item_id UUID REFERENCES public.reports(id) ON DELETE SET NULL,
    priority priority_level DEFAULT 'Medium',
    status TEXT DEFAULT 'Pending',
    admin_note TEXT,
    image_urls TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.admin_reports ENABLE ROW LEVEL SECURITY;

-- 5. Admin Requests
CREATE TABLE IF NOT EXISTS public.admin_requests (
    university_id TEXT PRIMARY KEY,
    auth_id UUID UNIQUE,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    designation TEXT,
    department TEXT,
    verification_code TEXT,
    profile_image_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'denied')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.admin_requests ENABLE ROW LEVEL SECURITY;

-- 6. Notifications
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    sender_id TEXT REFERENCES public.profiles(university_id) ON DELETE SET NULL,
    report_id UUID REFERENCES public.reports(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    additional_details TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- 7. Counters
CREATE TABLE IF NOT EXISTS public.counters (
    counter_name TEXT PRIMARY KEY,
    count BIGINT DEFAULT 0
);

INSERT INTO public.counters (counter_name, count) VALUES ('lost_items', 0), ('found_items', 0), ('admin_reports', 0) ON CONFLICT (counter_name) DO NOTHING;

-- 8. Functions & Triggers

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

    IF new_count IS NULL THEN
        INSERT INTO public.counters(counter_name, count) VALUES(counter_name_var, 1) RETURNING count INTO new_count;
    END IF;

    NEW.display_id := prefix || new_count;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_reports_display_id ON public.reports;
CREATE TRIGGER tr_reports_display_id BEFORE INSERT ON public.reports FOR EACH ROW WHEN (NEW.display_id IS NULL) EXECUTE FUNCTION generate_display_id();

DROP TRIGGER IF EXISTS tr_admin_reports_display_id ON public.admin_reports;
CREATE TRIGGER tr_admin_reports_display_id BEFORE INSERT ON public.admin_reports FOR EACH ROW WHEN (NEW.display_id IS NULL) EXECUTE FUNCTION generate_display_id();

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_reports_updated_at ON public.reports;
CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON public.reports FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_admin_reports_updated_at ON public.admin_reports;
CREATE TRIGGER update_admin_reports_updated_at BEFORE UPDATE ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 9. RPC Functions

-- RPC for manual ID generation from the app
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

-- 10. RLS Policies

CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE auth_id = auth.uid() AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Cleanup existing policies to avoid "already exists" errors
DO $$
BEGIN
    -- Profiles
    DROP POLICY IF EXISTS "Profiles viewable by authenticated" ON public.profiles;
    DROP POLICY IF EXISTS "Owner or admin can update profile" ON public.profiles;

    -- Reports
    DROP POLICY IF EXISTS "Reports viewable by authenticated" ON public.reports;
    DROP POLICY IF EXISTS "Users can insert reports" ON public.reports;
    DROP POLICY IF EXISTS "Owner or admin can update reports" ON public.reports;
    DROP POLICY IF EXISTS "Owner or admin can delete reports" ON public.reports;

    -- Admin Requests
    DROP POLICY IF EXISTS "Admins can view requests" ON public.admin_requests;
    DROP POLICY IF EXISTS "Authenticated can insert requests" ON public.admin_requests;
    DROP POLICY IF EXISTS "Admins can delete requests" ON public.admin_requests;

    -- Notifications
    DROP POLICY IF EXISTS "Users can see own notifications" ON public.notifications;
    DROP POLICY IF EXISTS "Users can insert notifications" ON public.notifications;
    DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
END $$;

-- Profiles
CREATE POLICY "Profiles viewable by authenticated" ON public.profiles FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Users can insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = auth_id);
CREATE POLICY "Owner or admin can update profile" ON public.profiles FOR UPDATE USING (auth.uid() = auth_id OR is_admin());

-- Reports
CREATE POLICY "Reports viewable by authenticated" ON public.reports FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Users can insert reports" ON public.reports FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Owner or admin can update reports" ON public.reports FOR UPDATE USING (
    is_admin() OR auth.uid() IN (SELECT auth_id FROM public.profiles WHERE university_id = reporter_id)
);
CREATE POLICY "Owner or admin can delete reports" ON public.reports FOR DELETE USING (
    is_admin() OR auth.uid() IN (SELECT auth_id FROM public.profiles WHERE university_id = reporter_id)
);

-- Admin Requests
CREATE POLICY "Enable insert for all users" ON public.admin_requests FOR INSERT WITH CHECK (true);
CREATE POLICY "Enable select for all users" ON public.admin_requests FOR SELECT USING (true);
CREATE POLICY "Admins can delete requests" ON public.admin_requests FOR DELETE USING (is_admin());

-- Notifications
CREATE POLICY "Users can see own notifications" ON public.notifications FOR SELECT USING (
    auth.uid() IN (SELECT auth_id FROM public.profiles WHERE university_id = recipient_id)
);
CREATE POLICY "Users can insert notifications" ON public.notifications FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Users can update own notifications" ON public.notifications FOR UPDATE USING (
    auth.uid() IN (SELECT auth_id FROM public.profiles WHERE university_id = recipient_id)
);
