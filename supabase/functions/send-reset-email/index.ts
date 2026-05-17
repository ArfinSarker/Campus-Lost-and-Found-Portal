// Use Deno.serve for better compatibility and performance
const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY')
const SUPABASE_URL = Deno.env.get('SUPABASE_URL')

Deno.serve(async (req) => {
  const url = new URL(req.url);
  
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { 
        headers: { 
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, GET',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey',
        } 
    })
  }

  // Handle Redirect (GET)
  if (req.method === 'GET') {
    const token = url.searchParams.get('token');
    if (!token) {
      return new Response('Missing token', { status: 400 });
    }

    const intentLink = `intent://reset-password?token=${token}#Intent;scheme=campus-lost-found;package=com.sas.lostandfound;S.token=${token};S.reset_token=${token};action=android.intent.action.VIEW;category=android.intent.category.BROWSABLE;end`;
    const appLink = `campus-lost-found://reset-password?token=${token}`;

    const html = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="0;url=${intentLink}">
    <title>Opening Campus Lost & Found</title>
    <style>
      body { font-family: -apple-system, system-ui, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background: #f3f4f6; text-align: center; }
      .card { background: white; padding: 2rem; border-radius: 1rem; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); max-width: 400px; width: 90%; }
      .btn { display: inline-block; background: #2AABEE; color: white; padding: 0.75rem 1.5rem; border-radius: 0.5rem; text-decoration: none; font-weight: bold; margin-top: 1.5rem; }
      .loader { border: 3px solid #e5e7eb; border-top-color: #2AABEE; border-radius: 50%; width: 2rem; height: 2rem; animation: spin 1s linear infinite; margin: 0 auto 1rem; }
      @keyframes spin { to { transform: rotate(360deg); } }
    </style>
  </head>
  <body>
    <div class="card">
      <div class="loader"></div>
      <h2 style="margin: 0 0 0.5rem; color: #111827;">Opening App...</h2>
      <p style="color: #4b5563;">Redirecting you to the password reset screen.</p>
      <a href="${intentLink}" class="btn">Open App Manually</a>
      <p style="margin-top: 1.5rem; font-size: 0.8rem; color: #9ca3af;">Link: ${appLink}</p>
    </div>
    <script>
      setTimeout(() => { window.location.href = "${intentLink}"; }, 500);
    </script>
  </body>
</html>`;

    return new Response(null, {
      status: 302,
      headers: {
        "Location": intentLink,
        "Access-Control-Allow-Origin": "*",
        "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
        "Pragma": "no-cache",
        "Expires": "0",
      },
    });
  }

  // Handle POST requests
  try {
    const body = await req.json()
    const { action, email, token, password, university_id } = body

    // 1. Action: SEND EMAIL
    if (!action || action === 'send-email') {
      const bridgeUrl = `${SUPABASE_URL}/functions/v1/send-reset-email?token=${token}`;
      const appLink = `campus-lost-found://reset-password?token=${token}`;

      console.log(`Sending reset email to ${email}`);

      const res = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${RESEND_API_KEY}`,
        },
        body: JSON.stringify({
          from: 'Lost&Found <onboarding@resend.dev>',
          to: [email],
          subject: 'Reset Your Password - Campus Lost&Found',
          html: `
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 8px;">
              <div style="text-align: center; margin-bottom: 20px;">
                <h2 style="color: #2AABEE; margin-bottom: 5px;">Reset Your Password</h2>
                <p style="color: #6B7280; font-size: 14px;">Campus Lost & Found</p>
              </div>
              <p>Hello,</p>
              <p>We received a request to reset your password for your Lost&amp;Found Portal account. Click the button below to set a new password:</p>
              <div style="text-align: center; margin: 35px 0;">
                <a href="${bridgeUrl}" style="padding: 16px 32px; background-color: #2AABEE; color: white; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">Open Reset Screen</a>
              </div>
              <p style="font-size: 14px; color: #6B7280; text-align: center;">If the button above doesn't work, <a href="${bridgeUrl}" style="color: #2AABEE;">click here to reset via browser</a>.</p>
            </div>
          `,
        }),
      })

      const data = await res.json()
      return new Response(JSON.stringify(data), {
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
        status: 200,
      })
    }

    // 2. Action: UPDATE PASSWORD (Admin API)
    if (action === 'update-password') {
      console.log(`Updating password for user with token ${token}`);
      
      const SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
      if (!SERVICE_ROLE_KEY) {
        throw new Error('Missing SUPABASE_SERVICE_ROLE_KEY')
      }

      // Initialize Supabase Client with Service Role
      const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY)

      // A. Find the user in profiles by token
      const { data: profile, error: profileError } = await supabase
        .from('profiles')
        .select('auth_id, university_id, reset_token_expires_at, reset_token_used')
        .eq('reset_token', token)
        .single()

      if (profileError || !profile) {
        return new Response(JSON.stringify({ error: 'Invalid or expired token' }), { status: 400, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' } })
      }

      if (profile.reset_token_used) {
        return new Response(JSON.stringify({ error: 'Token already used' }), { status: 400, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' } })
      }

      // B. Update the password in auth.users
      const { error: authError } = await supabase.auth.admin.updateUserById(
        profile.auth_id,
        { password: password }
      )

      if (authError) {
        console.error('Auth update error:', authError)
        return new Response(JSON.stringify({ error: 'Failed to update auth password: ' + authError.message }), { status: 500, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' } })
      }

      // C. Update the profiles table
      const { error: updateError } = await supabase
        .from('profiles')
        .update({ 
          password: password, 
          reset_token: null, 
          reset_token_used: true 
        })
        .eq('university_id', profile.university_id)

      if (updateError) {
        console.error('Profile update error:', updateError)
        return new Response(JSON.stringify({ error: 'Failed to update profile password: ' + updateError.message }), { status: 500, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' } })
      }

      return new Response(JSON.stringify({ message: 'Password updated successfully' }), {
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
        status: 200,
      })
    }

    return new Response(JSON.stringify({ error: 'Invalid action' }), { status: 400, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' } })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      status: 400,
    })
  }
})

// Mock createClient if not imported (Supabase Edge Functions usually have it available via import)
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'


