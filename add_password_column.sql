-- Safely add password column to profiles table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'password') THEN
        ALTER TABLE public.profiles ADD COLUMN password TEXT;
    END IF;
END $$;
