-- =========================================================================
-- CAMPUS LOST AND FOUND: MASTER DATABASE SETUP (CONSOLIDATED)
-- Single Source of Truth for the Database Schema, Policies, and Triggers.
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
    password TEXT,
    reset_token TEXT,
    reset_token_expires_at TIMESTAMPTZ,
    reset_token_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

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
    claimed_by_id TEXT REFERENCES public.profiles(university_id) ON DELETE SET NULL,
    hidden_identification_question TEXT,
    additional_location_details TEXT,
    proof_of_ownership_detail TEXT,
    item_handling_status TEXT,
    authority_name TEXT,
    office_room_number TEXT,
    preferred_contact_method TEXT,
    contact_email TEXT,
    contact_phone TEXT,
    contact_name TEXT,
    is_edited BOOLEAN DEFAULT FALSE,
    deleted_by_user BOOLEAN DEFAULT FALSE,
    user_id UUID,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- COMPATIBILITY VIEWS
DROP VIEW IF EXISTS public.lost_reports CASCADE;
CREATE OR REPLACE VIEW public.lost_reports WITH (security_invoker = on) AS
SELECT * FROM public.reports WHERE type = 'lost';

DROP VIEW IF EXISTS public.found_reports CASCADE;
CREATE OR REPLACE VIEW public.found_reports WITH (security_invoker = on) AS
SELECT * FROM public.reports WHERE type = 'found';

-- 6. ADMIN REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.admin_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id TEXT UNIQUE,
    reporter_id TEXT REFERENCES public.profiles(university_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    related_item_id TEXT,
    status TEXT DEFAULT 'Pending',
    admin_note TEXT,
    image_urls TEXT[] DEFAULT '{}',
    image_url TEXT,
    deleted_by_user BOOLEAN DEFAULT FALSE,
    timestamp BIGINT,
    user_id UUID,
    reporter_auth_id TEXT,
    phone TEXT,
    email TEXT,
    reporter_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at_timestamp BIGINT,
    reviewed_by TEXT REFERENCES public.profiles(university_id) ON DELETE SET NULL,
    review_timestamp BIGINT
);

-- 7. NOTIFICATIONS TABLE
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
    additional_details TEXT,
    sender_name TEXT,
    sender_phone TEXT,
    sender_email TEXT,
    sender_image_url TEXT,
    item_name TEXT,
    claimer_id TEXT,
    claimer_name TEXT,
    item_id TEXT,
    claim_type TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. SECURITY & UTILITY FUNCTIONS
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE auth_id = auth.uid() AND (role = 'admin' OR user_type = 'Admin')
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

CREATE OR REPLACE FUNCTION public.fn_populate_notification_user_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NULL AND NEW.recipient_id IS NOT NULL THEN
        NEW.user_id := (SELECT auth_id FROM public.profiles WHERE university_id = NEW.recipient_id LIMIT 1);
    END IF;
    
    -- Map frontend's types to matching query-filter types:
    IF NEW.type = 'lost_claim' THEN
        NEW.type := 'lost_item';
    ELSIF NEW.type = 'found_claim' THEN
        NEW.type := 'found_item';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 9. TRIGGERS
DROP TRIGGER IF EXISTS tr_reports_display_id ON public.reports;
CREATE TRIGGER tr_reports_display_id BEFORE INSERT ON public.reports FOR EACH ROW EXECUTE FUNCTION public.generate_display_id();

DROP TRIGGER IF EXISTS tr_admin_reports_display_id ON public.admin_reports;
CREATE TRIGGER tr_admin_reports_display_id BEFORE INSERT ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION public.generate_display_id();

DROP TRIGGER IF EXISTS tr_notifications_timestamp ON public.notifications;
CREATE TRIGGER tr_notifications_timestamp BEFORE INSERT ON public.notifications FOR EACH ROW EXECUTE FUNCTION public.set_timestamp();

DROP TRIGGER IF EXISTS tr_populate_notification_user_id ON public.notifications;
CREATE TRIGGER tr_populate_notification_user_id BEFORE INSERT ON public.notifications FOR EACH ROW EXECUTE FUNCTION public.fn_populate_notification_user_id();

DROP TRIGGER IF EXISTS update_profiles_timestamp ON public.profiles;
CREATE TRIGGER update_profiles_timestamp BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

DROP TRIGGER IF EXISTS update_reports_timestamp ON public.reports;
CREATE TRIGGER update_reports_timestamp BEFORE UPDATE ON public.reports FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

DROP TRIGGER IF EXISTS update_admin_reports_timestamp ON public.admin_reports;
CREATE TRIGGER update_admin_reports_timestamp BEFORE UPDATE ON public.admin_reports FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();

-- 10. FORGOT PASSWORD ANONYMOUS SECURITY TRIGGER
CREATE OR REPLACE FUNCTION public.check_anon_profile_update()
RETURNS TRIGGER AS $$
BEGIN
    IF auth.role() = 'anon' THEN
        -- Block changing sensitive fields without auth
        IF NEW.university_id IS DISTINCT FROM OLD.university_id OR
           NEW.email IS DISTINCT FROM OLD.email OR
           NEW.role IS DISTINCT FROM OLD.role OR
           NEW.password IS DISTINCT FROM OLD.password OR
           NEW.auth_id IS DISTINCT FROM OLD.auth_id OR
           NEW.full_name IS DISTINCT FROM OLD.full_name OR
           NEW.user_type IS DISTINCT FROM OLD.user_type THEN
            RAISE EXCEPTION 'Anonymous users can only update password reset tokens.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_check_anon_profile_update ON public.profiles;
CREATE TRIGGER tr_check_anon_profile_update
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.check_anon_profile_update();

-- 10b. SECURE ADMIN USER DELETION RPC FUNCTION (SECURITY DEFINER)
-- Completely avoids circular triggers and cascade locks by initiating deletions from auth.users first.
CREATE OR REPLACE FUNCTION public.delete_user_by_admin(target_university_id TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, storage
AS $$
DECLARE
    target_auth_id UUID;
BEGIN
    -- 1. Ensure the executing user is an authenticated admin
    IF NOT public.is_admin() THEN
        RAISE EXCEPTION 'Access Denied: Only administrators can delete users.';
    END IF;

    -- 2. Retrieve the auth_id of the target user
    SELECT auth_id INTO target_auth_id 
    FROM public.profiles 
    WHERE university_id = target_university_id;

    -- 3. Perform manual safe cleanup of related user data and associations
    
    -- Clean up storage objects ownership to prevent objects_owner_fkey blocks
    -- Since Supabase restricts direct DELETE on storage.objects, we run a safe UPDATE to sever the foreign key reference
    IF target_auth_id IS NOT NULL AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'storage' AND table_name = 'objects') THEN
        UPDATE storage.objects SET owner = NULL WHERE owner = target_auth_id;
    END IF;

    -- Clean up admin requests
    DELETE FROM public.admin_requests 
    WHERE university_id = target_university_id OR (target_auth_id IS NOT NULL AND auth_id = target_auth_id);

    -- Delete notifications sent by this user
    DELETE FROM public.notifications 
    WHERE sender_id = target_university_id;

    -- Reset status of reports claimed by this user back to active
    UPDATE public.reports 
    SET status = 'active', claimed_by_id = NULL 
    WHERE claimed_by_id = target_university_id;

    -- 4. Delete the corresponding auth.users and profiles records safely
    IF target_auth_id IS NOT NULL THEN
        -- Deleting from auth.users automatically cascade-deletes the profiles row due to the foreign key constraint
        DELETE FROM auth.users WHERE id = target_auth_id;
    ELSE
        -- Fallback: If they do not have an auth account (e.g. mock user), delete from profiles directly
        DELETE FROM public.profiles WHERE university_id = target_university_id;
    END IF;
END;
$$;

-- Drop the old tr_on_profile_delete trigger and delete_auth_user trigger function to avoid circular cascade loops
DROP TRIGGER IF EXISTS tr_on_profile_delete ON public.profiles;
DROP TRIGGER IF EXISTS tr_on_profile_delete ON profiles;
DROP FUNCTION IF EXISTS public.delete_auth_user();

-- 11. ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- 11b. SAFE FOREIGN KEY MIGRATION
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tc.constraint_name 
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
          AND tc.table_schema = kcu.table_schema
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND tc.table_name = 'reports'
          AND kcu.column_name = 'claimed_by_id'
    ) LOOP
        EXECUTE 'ALTER TABLE public.reports DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;
END;
$$;

ALTER TABLE public.reports 
ADD CONSTRAINT reports_claimed_by_id_fkey 
FOREIGN KEY (claimed_by_id) 
REFERENCES public.profiles(university_id) 
ON DELETE SET NULL;



-- 12. CLEAN AND APPLY POLICIES
DO $$
BEGIN
    -- Profiles
    DROP POLICY IF EXISTS "Public lookup" ON public.profiles;
    DROP POLICY IF EXISTS "Profiles selection" ON public.profiles;
    CREATE POLICY "Profiles selection" ON public.profiles FOR SELECT USING (true);
    
    DROP POLICY IF EXISTS "Profiles update own" ON public.profiles;
    DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
    CREATE POLICY "Profiles update own" ON public.profiles FOR UPDATE USING (auth.uid() = auth_id OR public.is_admin());
    
    DROP POLICY IF EXISTS "Profiles insert system" ON public.profiles;
    DROP POLICY IF EXISTS "System can insert profiles" ON public.profiles;
    DROP POLICY IF EXISTS "Enable insert for registration" ON public.profiles;
    CREATE POLICY "Profiles insert system" ON public.profiles FOR INSERT TO authenticated WITH CHECK (auth.uid() = auth_id);
    
    DROP POLICY IF EXISTS "Profiles delete own" ON public.profiles;
    CREATE POLICY "Profiles delete own" ON public.profiles FOR DELETE USING (auth.uid() = auth_id OR public.is_admin());
    
    DROP POLICY IF EXISTS "Allow anonymous update for reset tokens" ON public.profiles;
    CREATE POLICY "Allow anonymous update for reset tokens" ON public.profiles FOR UPDATE USING (auth.uid() IS NULL);

    -- Reports
    DROP POLICY IF EXISTS "Reports view" ON public.reports;
    CREATE POLICY "Reports view" ON public.reports FOR SELECT USING (true);
    
    DROP POLICY IF EXISTS "Reports insert" ON public.reports;
    CREATE POLICY "Reports insert" ON public.reports FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
    
    DROP POLICY IF EXISTS "Reports update" ON public.reports;
    CREATE POLICY "Reports update" ON public.reports FOR UPDATE USING (auth.uid() = user_id OR public.is_admin());
    
    DROP POLICY IF EXISTS "Reports delete" ON public.reports;
    CREATE POLICY "Reports delete" ON public.reports FOR DELETE USING (auth.uid() = user_id OR public.is_admin());

    -- Admin Requests
    DROP POLICY IF EXISTS "Admin requests selection" ON public.admin_requests;
    CREATE POLICY "Admin requests selection" ON public.admin_requests FOR SELECT USING (true);
    
    DROP POLICY IF EXISTS "Admin requests insert" ON public.admin_requests;
    CREATE POLICY "Admin requests insert" ON public.admin_requests FOR INSERT TO authenticated WITH CHECK (auth.uid() = auth_id);
    
    DROP POLICY IF EXISTS "Admin requests manage" ON public.admin_requests;
    CREATE POLICY "Admin requests manage" ON public.admin_requests FOR ALL USING (public.is_admin());
    
    DROP POLICY IF EXISTS "Admin requests delete" ON public.admin_requests;
    CREATE POLICY "Admin requests delete" ON public.admin_requests FOR DELETE USING (public.is_admin());

    -- Admin Reports
    DROP POLICY IF EXISTS "Admin reports manage" ON public.admin_reports;
    CREATE POLICY "Admin reports manage" ON public.admin_reports FOR ALL USING (public.is_admin());
    
    DROP POLICY IF EXISTS "Admin reports insert" ON public.admin_reports;
    CREATE POLICY "Admin reports insert" ON public.admin_reports FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

    -- Notifications
    DROP POLICY IF EXISTS "Notifications view own" ON public.notifications;
    CREATE POLICY "Notifications view own" ON public.notifications FOR SELECT USING (
        user_id = auth.uid() OR 
        sender_id IN (SELECT university_id FROM public.profiles WHERE auth_id = auth.uid()) OR
        public.is_admin()
    );
    
    DROP POLICY IF EXISTS "Notifications insert" ON public.notifications;
    CREATE POLICY "Notifications insert" ON public.notifications FOR INSERT TO authenticated WITH CHECK (true);
    
    DROP POLICY IF EXISTS "Notifications update own" ON public.notifications;
    CREATE POLICY "Notifications update own" ON public.notifications FOR UPDATE USING (auth.uid() = user_id OR public.is_admin());
    
    DROP POLICY IF EXISTS "Notifications delete own" ON public.notifications;
    CREATE POLICY "Notifications delete own" ON public.notifications FOR DELETE USING (auth.uid() = user_id OR public.is_admin());
END $$;

-- 13. PERMISSIONS
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 14. DATA MIGRATION (Ensure timestamps are populated)
UPDATE public.reports SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;
UPDATE public.admin_reports SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;
UPDATE public.notifications SET timestamp = (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT WHERE timestamp IS NULL;
UPDATE public.notifications SET type = 'lost_item' WHERE type = 'lost_claim';
UPDATE public.notifications SET type = 'found_item' WHERE type = 'found_claim';

-- 15. STORAGE BUCKET & POLICIES (From policies.sql)
INSERT INTO storage.buckets (id, name, public) 
VALUES ('images', 'images', true) 
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    DROP POLICY IF EXISTS "Public View Access" ON storage.objects;
    CREATE POLICY "Public View Access" ON storage.objects FOR SELECT USING (bucket_id = 'images');
    
    DROP POLICY IF EXISTS "Authenticated Upload Access" ON storage.objects;
    CREATE POLICY "Authenticated Upload Access" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'images');
    
    DROP POLICY IF EXISTS "User Update Own" ON storage.objects;
    CREATE POLICY "User Update Own" ON storage.objects FOR UPDATE TO authenticated USING (bucket_id = 'images');
    
    DROP POLICY IF EXISTS "User Delete Own" ON storage.objects;
    CREATE POLICY "User Delete Own" ON storage.objects FOR DELETE TO authenticated USING (bucket_id = 'images');
END $$;

-- 16. FORCE API REFRESH
NOTIFY pgrst, 'reload schema';
