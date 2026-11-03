import { randomUUID } from "node:crypto"
import { NextResponse, type NextRequest } from "next/server"
import { createClient as createSupabaseClient } from "@supabase/supabase-js"
import { createClient } from "@/lib/supabase/server"

export const runtime = "nodejs"

const MAX_UPLOAD_BYTES = 15 * 1024 * 1024
const MEDIA_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif", "video/mp4", "video/webm"]
const EXTENSIONS: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/webp": "webp",
  "image/gif": "gif",
  "video/mp4": "mp4",
  "video/webm": "webm",
}

function hasValidSignature(type: string, bytes: Buffer): boolean {
  if (type === "image/jpeg") return bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff
  if (type === "image/png") return bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
  if (type === "image/webp") return bytes.toString("ascii", 0, 4) === "RIFF" && bytes.toString("ascii", 8, 12) === "WEBP"
  if (type === "image/gif") return ["GIF87a", "GIF89a"].includes(bytes.toString("ascii", 0, 6))
  if (type === "video/mp4") return bytes.toString("ascii", 4, 8) === "ftyp"
  if (type === "video/webm") return bytes.subarray(0, 4).equals(Buffer.from([0x1a, 0x45, 0xdf, 0xa3]))
  return false
}

export async function POST(request: NextRequest) {
  if (request.headers.get("origin") !== request.nextUrl.origin) {
    return NextResponse.json({ error: "Request origin could not be verified." }, { status: 403 })
  }

  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) return NextResponse.json({ error: "Sign in required." }, { status: 401 })

  const { data: membership, error: membershipError } = await supabase
    .from("admin_users")
    .select("user_id")
    .eq("user_id", user.id)
    .maybeSingle()

  if (membershipError || !membership) {
    return NextResponse.json({ error: "Administrator access required." }, { status: 403 })
  }

  const formData = await request.formData()
  const file = formData.get("file")
  if (!(file instanceof File)) return NextResponse.json({ error: "Choose a media file to upload." }, { status: 400 })
  if (file.size <= 0 || file.size > MAX_UPLOAD_BYTES) {
    return NextResponse.json({ error: "Files must be smaller than 15 MB." }, { status: 413 })
  }
  if (!MEDIA_TYPES.includes(file.type)) {
    return NextResponse.json({ error: "Use a JPEG, PNG, WebP, GIF, MP4, or WebM file." }, { status: 415 })
  }

  const bytes = Buffer.from(await file.arrayBuffer())
  if (!hasValidSignature(file.type, bytes)) {
    return NextResponse.json({ error: "This file does not match its declared media type." }, { status: 415 })
  }

  const supabaseUrl = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY
  if (!supabaseUrl || !serviceRoleKey) {
    return NextResponse.json({ error: "Media storage is not configured." }, { status: 503 })
  }

  const adminClient = createSupabaseClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  })

  const { data: bucket, error: bucketError } = await adminClient.storage.getBucket("draw-media")
  if (bucketError) {
    const { error: createBucketError } = await adminClient.storage.createBucket("draw-media", {
      public: true,
      fileSizeLimit: MAX_UPLOAD_BYTES,
      allowedMimeTypes: MEDIA_TYPES,
    })
    if (createBucketError && !createBucketError.message.toLowerCase().includes("already exists")) {
      return NextResponse.json({ error: "Media storage could not be initialized." }, { status: 503 })
    }
  } else if (!bucket.public) {
    const { error: updateBucketError } = await adminClient.storage.updateBucket("draw-media", {
      public: true,
      fileSizeLimit: MAX_UPLOAD_BYTES,
      allowedMimeTypes: MEDIA_TYPES,
    })
    if (updateBucketError) return NextResponse.json({ error: "Media storage could not be configured." }, { status: 503 })
  }

  const path = `${user.id}/${randomUUID()}.${EXTENSIONS[file.type]}`
  const { error: uploadError } = await adminClient.storage.from("draw-media").upload(path, bytes, {
    cacheControl: "31536000",
    contentType: file.type,
    upsert: false,
  })

  if (uploadError) return NextResponse.json({ error: "Media upload failed. Please try again." }, { status: 500 })

  const { data } = adminClient.storage.from("draw-media").getPublicUrl(path)
  return NextResponse.json({ url: data.publicUrl })
}
