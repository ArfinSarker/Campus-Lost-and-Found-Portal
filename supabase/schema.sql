-- =========================================================
-- CAMPUS LOST AND FOUND: DATABASE SCHEMA & FUNCTIONS
-- =========================================================

-- 1. PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    "userId" TEXT PRIMARY KEY,
    "authId" TEXT UNIQUE,
    "name" TEXT,
    "fullName" TEXT,
    "universityId" TEXT UNIQUE,
    "email" TEXT UNIQUE,
    "phone" TEXT,
    "phoneNumber" TEXT,
    "department" TEXT,
    "batch" TEXT,
    "levelTerm" TEXT,
    "section" TEXT,
    "profileImageUrl" TEXT,
    "profileImageUrls" TEXT[],
    "gender" TEXT,
    "userType" TEXT,
    "designation" TEXT,
    "role" TEXT DEFAULT 'user',
    "requestStatus" TEXT DEFAULT 'approved',
    "registeredAt" BIGINT,
    "created_at" TEXT,
    "isAdmin" BOOLEAN DEFAULT FALSE
);

-- 2. FOUND REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.found_reports (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "displayId" TEXT,
    "name" TEXT,
    "category" TEXT,
    "description" TEXT,
    "location" TEXT,
    "manualLocation" TEXT,
    "date" TEXT,
    "time" TEXT,
    "additionalLocationDetails" TEXT,
    "imageUrl" TEXT,
    "imageUrls" TEXT[],
    "status" TEXT DEFAULT 'found',
    "userId" TEXT REFERENCES public.profiles("userId"),
    "userName" TEXT,
    "userEmail" TEXT,
    "userPhone" TEXT,
    "userUniversityId" TEXT,
    "userDepartment" TEXT,
    "contactName" TEXT,
    "contactPhone" TEXT,
    "secondUserId" TEXT,
    "secondUserName" TEXT,
    "itemHandlingStatus" TEXT,
    "authorityName" TEXT,
    "officeRoomNumber" TEXT,
    "hiddenIdentificationQuestion" TEXT,
    "verificationMethod" TEXT,
    "isBlurred" BOOLEAN DEFAULT FALSE,
    "preferredContactMethod" TEXT,
    "adminStatus" TEXT DEFAULT 'Pending',
    "claimedByUserId" TEXT,
    "timestamp" BIGINT,
    "isEdited" BOOLEAN DEFAULT FALSE,
    "deleted_by_user" BOOLEAN DEFAULT FALSE
);

-- 3. LOST REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.lost_reports (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "displayId" TEXT,
    "name" TEXT,
    "category" TEXT,
    "description" TEXT,
    "location" TEXT,
    "manualLocation" TEXT,
    "date" TEXT,
    "time" TEXT,
    "additionalLocationDetails" TEXT,
    "imageUrl" TEXT,
    "imageUrls" TEXT[],
    "status" TEXT DEFAULT 'lost',
    "userId" TEXT REFERENCES public.profiles("userId"),
    "userName" TEXT,
    "userEmail" TEXT,
    "userPhone" TEXT,
    "userUniversityId" TEXT,
    "userDepartment" TEXT,
    "contactName" TEXT,
    "contactPhone" TEXT,
    "proofOfOwnershipUrl" TEXT,
    "proofOfOwnershipUrls" TEXT[],
    "proofOfOwnershipDetail" TEXT,
    "confidentialIdentificationDetail" TEXT,
    "preferredContactMethod" TEXT,
    "adminStatus" TEXT DEFAULT 'Pending',
    "timestamp" BIGINT,
    "isEdited" BOOLEAN DEFAULT FALSE,
    "deleted_by_user" BOOLEAN DEFAULT FALSE
);

-- 4. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS public.notifications (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "recipientId" TEXT,
    "senderId" TEXT,
    "senderName" TEXT,
    "senderPhone" TEXT,
    "senderEmail" TEXT,
    "itemId" TEXT,
    "itemName" TEXT,
    "message" TEXT,
    "timestamp" BIGINT,
    "read" BOOLEAN DEFAULT FALSE,
    "type" TEXT,
    "additionalDetails" TEXT
);

-- 5. ADMIN REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.admin_reports (
    "reportId" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "displayId" TEXT,
    "title" TEXT,
    "category" TEXT,
    "description" TEXT,
    "relatedId" TEXT,
    "reporterName" TEXT,
    "universityId" TEXT,
    "reporterAuthId" TEXT,
    "phone" TEXT,
    "imageUrl" TEXT,
    "imageUrls" TEXT[],
    "priority" TEXT,
    "status" TEXT DEFAULT 'Pending',
    "admin_note" TEXT,
    "timestamp" BIGINT,
    "updated_at" BIGINT,
    "deleted_by_user" BOOLEAN DEFAULT FALSE
);

-- 6. ADMIN REQUESTS TABLE
CREATE TABLE IF NOT EXISTS public.admin_requests (
    "universityId" TEXT PRIMARY KEY,
    "authId" TEXT,
    "fullName" TEXT,
    "email" TEXT,
    "phoneNumber" TEXT,
    "designation" TEXT,
    "verificationCode" TEXT,
    "password" TEXT,
    "userType" TEXT DEFAULT 'Admin',
    "requestStatus" TEXT DEFAULT 'pending',
    "profileImageUrl" TEXT,
    "timestamp" BIGINT,
    "created_at" TEXT
);

-- 7. COUNTERS TABLE (FOR ID GENERATION)
CREATE TABLE IF NOT EXISTS public.counters (
    "counter_name" TEXT PRIMARY KEY,
    "count" BIGINT DEFAULT 0
);

-- 8. INCREMENT COUNTER RPC FUNCTION
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
