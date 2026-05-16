-- Function to sync profile changes to auth.users
-- This ensures that direct updates to email/phone in the app are reflected in the login system.
CREATE OR REPLACE FUNCTION public.sync_profile_to_auth()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE auth.users
  SET email = NEW.email,
      phone = NEW.phone_number,
      email_confirmed_at = CASE WHEN OLD.email <> NEW.email THEN NOW() ELSE email_confirmed_at END,
      updated_at = NOW()
  WHERE id = NEW.auth_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to execute the sync function on email or phone change
DROP TRIGGER IF EXISTS tr_sync_profile_to_auth ON public.profiles;
CREATE TRIGGER tr_sync_profile_to_auth
AFTER UPDATE OF email, phone_number ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.sync_profile_to_auth();
