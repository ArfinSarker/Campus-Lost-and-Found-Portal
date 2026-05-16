-- =========================================================
-- CAMPUS LOST AND FOUND: STORAGE POLICIES (BUCKET: images)
-- =========================================================

-- 1. SELECT: Public Access (View)
-- Allows anyone to view the images
CREATE POLICY "Public View Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'images');

-- 2. INSERT: Authenticated Access (Upload)
-- Allows any logged-in user to upload images
CREATE POLICY "Authenticated Upload Access"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'images');

-- 3. UPDATE: User Own Access
-- Allows users to update images they have uploaded
CREATE POLICY "User Update Own"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'images');

-- 4. DELETE: User Own Access
-- Allows users to delete images they have uploaded
CREATE POLICY "User Delete Own"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'images');
