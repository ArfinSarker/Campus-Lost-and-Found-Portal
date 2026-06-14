import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const FIREBASE_SERVICE_ACCOUNT_ENV = Deno.env.get('FIREBASE_SERVICE_ACCOUNT');

Deno.serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey',
      }
    })
  }

  try {
    const body = await req.json();
    console.log("Webhook payload received:", JSON.stringify(body));

    const notification = body.record;
    if (!notification) {
      return new Response(JSON.stringify({ error: "Missing record object" }), {
        status: 400,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    const recipientId = notification.recipient_id;
    if (!recipientId) {
      console.log("No recipient_id on notification, skipping.");
      return new Response(JSON.stringify({ message: "No recipient_id, skipped." }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // Initialize Supabase Client with Service Role key
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    // Fetch the recipient's FCM token from profiles
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('fcm_token')
      .eq('university_id', recipientId)
      .single();

    if (profileError || !profile) {
      console.error("Error fetching recipient profile:", profileError);
      return new Response(JSON.stringify({ error: "Profile search failed" }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    const fcmToken = profile.fcm_token;
    if (!fcmToken) {
      console.log(`No FCM token registered for recipient ${recipientId}, skipping push notification.`);
      return new Response(JSON.stringify({ message: "No FCM token registered, skipped." }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // Verify service account exists
    if (!FIREBASE_SERVICE_ACCOUNT_ENV) {
      throw new Error("Missing FIREBASE_SERVICE_ACCOUNT environment variable.");
    }

    const serviceAccount = JSON.parse(FIREBASE_SERVICE_ACCOUNT_ENV);
    const projectId = serviceAccount.project_id;
    if (!projectId) {
      throw new Error("Missing project_id in service account JSON.");
    }

    // Get OAuth2 access token
    const accessToken = await getAccessToken(serviceAccount);

    // Derive notification title
    const getNotificationTitle = (type: string): string => {
      if (!type) return "Lost & Found Update";
      switch (type) {
        case "lost_item":
          return "New Lost Item Report";
        case "found_item":
          return "New Found Item Report";
        case "lost_claim":
        case "found_claim":
        case "item_claimed":
          return "Item Claim Request";
        case "item_return":
          return "Item Returned";
        case "admin_report":
        case "admin_report_new":
          return "Admin Report Update";
        case "admin_request":
          return "Admin Request Update";
        default:
          return "Campus Lost & Found Update";
      }
    };

    const title = getNotificationTitle(notification.type);
    const bodyText = notification.message || "You have a new update.";

    // Build the FCM message payload
    const fcmPayload = {
      message: {
        token: fcmToken,
        android: {
          priority: "HIGH"
        },
        notification: {
          title: title,
          body: bodyText
        },
        data: {
          from_notification: "true",
          notification_id: String(notification.id || ""),
          notification_type: String(notification.type || ""),
          item_id: String(notification.item_id || notification.report_id || ""),
          sender_id: String(notification.sender_id || ""),
          claimer_id: String(notification.claimer_id || ""),
          sender_name: String(notification.sender_name || ""),
          sender_phone: String(notification.sender_phone || ""),
          sender_email: String(notification.sender_email || ""),
          item_name: String(notification.item_name || ""),
          additional_details: String(notification.additional_details || ""),
          message: bodyText
        }
      }
    };

    console.log(`Sending FCM notification to token ${fcmToken}...`);
    const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
    const fcmResponse = await fetch(fcmUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`
      },
      body: JSON.stringify(fcmPayload)
    });

    const fcmResult = await fcmResponse.json();
    console.log("FCM send result:", JSON.stringify(fcmResult));

    if (!fcmResponse.ok) {
      console.error("FCM API error response:", fcmResult);
      return new Response(JSON.stringify({ error: "FCM API error", details: fcmResult }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    return new Response(JSON.stringify({ message: "Notification sent successfully", result: fcmResult }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
    });

  } catch (error) {
    console.error("Function error:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
    });
  }
});

// Helper function to sign JWT and request Google OAuth2 access token
async function getAccessToken(serviceAccount: any): Promise<string> {
  const privateKeyPem = serviceAccount.private_key;
  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = privateKeyPem
    .substring(privateKeyPem.indexOf(pemHeader) + pemHeader.length, privateKeyPem.indexOf(pemFooter))
    .replace(/\s+/g, "");
  const binaryDerString = atob(pemContents);
  const binaryDer = new Uint8Array(binaryDerString.length);
  for (let i = 0; i < binaryDerString.length; i++) {
    binaryDer[i] = binaryDerString.charCodeAt(i);
  }

  const key = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["sign"]
  );

  const header = {
    alg: "RS256",
    typ: "JWT",
  };

  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };

  const textEncoder = new TextEncoder();
  const encodedHeader = btoa(JSON.stringify(header))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
  const encodedPayload = btoa(JSON.stringify(payload))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  const stringToSign = `${encodedHeader}.${encodedPayload}`;
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    textEncoder.encode(stringToSign)
  );

  const encodedSignature = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  const jwt = `${stringToSign}.${encodedSignature}`;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  const data = await response.json();
  if (data.error) {
    throw new Error(`Google Auth error: ${data.error_description || data.error}`);
  }
  return data.access_token;
}
