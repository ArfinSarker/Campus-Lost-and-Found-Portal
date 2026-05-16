# Supabase Configuration for Campus Lost and Found

This folder contains the official SQL schema and storage policies for the project.

## Files
- `schema.sql`: Contains all table definitions and the `increment_counter` function for custom IDs (L1, F1, etc.).
- `policies.sql`: Contains the Row Level Security (RLS) policies for the `images` storage bucket.

## How to Setup
1. **Tables & Functions**: Open your Supabase Dashboard -> SQL Editor. Create a "New Query", paste the contents of `schema.sql`, and click **Run**.
2. **Storage Bucket**: Go to Storage. Create a new bucket named `images`. Set it to **Public**.
3. **Storage Policies**: In the SQL Editor, paste the contents of `policies.sql` and click **Run**.
4. **Auth Settings**: Ensure **Email Auth** is enabled in the Authentication providers.
    - **Pro Tip**: To avoid "Email rate limit exceeded" during testing, go to **Authentication** -> **Email Auth** and toggle **Confirm email** to **OFF**. This allows you to create accounts without waiting for verification emails.

## Troubleshooting
- **Email rate limit exceeded**: This is a Supabase safety feature. To fix it:
    1. Go to **Authentication** -> **Settings**.
    2. Under **Rate Limits**, increase the "Max emails per hour" (e.g., set it to 30).
    3. Alternatively, disable email confirmation as mentioned above.

After running these scripts, you can verify:
- The `profiles` table exists in the Table Editor.
- The `increment_counter` function exists in the Database -> Functions section.
- The `images` bucket has 4 active policies in the Storage -> Policies section.
