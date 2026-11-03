"use client"

import { useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Pencil, Plus, Trash2, UploadCloud } from "lucide-react"
import { uploadAdminMedia } from "@/lib/admin-media"
import { createClient } from "@/lib/supabase/client"
import type { SiteContent } from "@/lib/types"

const sections: SiteContent["section_name"][] = ["hero_banner", "guidelines", "trust", "videos"]
const sectionLabels: Record<SiteContent["section_name"], string> = {
  hero_banner: "Hero banner",
  guidelines: "Guidelines",
  trust: "Trust text",
  videos: "Video",
}

type ContentForm = { id: string; section: SiteContent["section_name"]; text: string; mediaUrl: string; order: string }
const emptyForm = (): ContentForm => ({ id: "", section: "hero_banner", text: "", mediaUrl: "", order: "0" })

export function ContentManager({ content }: { content: SiteContent[] }) {
  const [form, setForm] = useState<ContentForm>(emptyForm)
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [notice, setNotice] = useState("")
  const router = useRouter()
  const supabase = createClient()

  function editItem(item: SiteContent) {
    setForm({ id: item.id, section: item.section_name, text: item.content_text, mediaUrl: item.media_url ?? "", order: String(item.display_order) })
    setEditing(true)
    setNotice("")
  }

  function resetForm() {
    setForm(emptyForm())
    setEditing(false)
    setNotice("")
  }

  async function upload(file: File | undefined) {
    if (!file) return
    setUploading(true)
    setNotice("")
    try {
      const url = await uploadAdminMedia(file)
      setForm((current) => ({ ...current, mediaUrl: url }))
      setNotice("Media uploaded. Save this content item to publish it.")
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Upload failed.")
    } finally {
      setUploading(false)
    }
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setNotice("")
    const { error } = await supabase.rpc("admin_save_site_content", {
      p_content_id: form.id || null,
      p_section_name: form.section,
      p_content_text: form.text.trim(),
      p_media_url: form.mediaUrl.trim() || null,
      p_display_order: Number(form.order),
    })
    setBusy(false)

    if (error) {
      setNotice("Content could not be saved. Check the values and try again.")
      return
    }

    setNotice(editing ? "Content updated." : "Content published.")
    resetForm()
    router.refresh()
  }

  async function remove(item: SiteContent) {
    if (!window.confirm(`Delete this ${sectionLabels[item.section_name].toLowerCase()} item?`)) return
    setNotice("")
    const { error } = await supabase.rpc("admin_delete_site_content", { p_content_id: item.id })
    if (error) {
      setNotice("Content could not be deleted. Please try again.")
      return
    }
    if (form.id === item.id) resetForm()
    setNotice("Content deleted.")
    router.refresh()
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
      <section className="surface-card p-5 sm:p-6">
        <div className="flex items-start justify-between gap-3">
          <div><p className="section-eyebrow">Media & content</p><h2 className="mt-1 text-xl font-bold text-ink-strong">{editing ? "Edit content" : "Add content"}</h2></div>
          {editing && <button type="button" className="secondary-button min-h-9 px-3" onClick={resetForm}><Plus size={15} aria-hidden="true" /> New</button>}
        </div>
        <p className="mt-2 text-sm leading-6 text-muted">Hero and video copy uses the first line as its headline; remaining lines appear as supporting text.</p>
        <form className="mt-5 space-y-4" onSubmit={save}>
          <div>
            <label className="field-label" htmlFor="content-section">Section</label>
            <select className="field-control" id="content-section" value={form.section} onChange={(event) => setForm({ ...form, section: event.target.value as ContentForm["section"] })}>
              {sections.map((section) => <option key={section} value={section}>{sectionLabels[section]}</option>)}
            </select>
          </div>
          <div>
            <label className="field-label" htmlFor="content-text">Text</label>
            <textarea className="field-control min-h-36" id="content-text" value={form.text} onChange={(event) => setForm({ ...form, text: event.target.value })} maxLength={10000} placeholder="Headline\nSupporting copy" />
          </div>
          <div>
            <label className="field-label" htmlFor="content-media-url">Media URL</label>
            <input className="field-control" id="content-media-url" type="url" value={form.mediaUrl} onChange={(event) => setForm({ ...form, mediaUrl: event.target.value })} placeholder="https://…" />
          </div>
          <div>
            <label className="field-label" htmlFor="content-media-file">Upload image or video</label>
            <div className="flex flex-wrap items-center gap-3">
              <input className="field-control min-w-0 flex-1" id="content-media-file" type="file" accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm" onChange={(event) => { void upload(event.target.files?.[0]); event.currentTarget.value = "" }} />
              <span className="inline-flex items-center gap-1.5 text-xs text-muted"><UploadCloud size={15} aria-hidden="true" />{uploading ? "Uploading…" : "15 MB max"}</span>
            </div>
          </div>
          <div>
            <label className="field-label" htmlFor="content-order">Display order</label>
            <input className="field-control" id="content-order" type="number" value={form.order} onChange={(event) => setForm({ ...form, order: event.target.value })} required />
          </div>
          {notice && <p className="rounded-xl bg-green-pale px-4 py-3 text-sm text-green" role="status">{notice}</p>}
          <div className="flex flex-wrap gap-3">
            <button type="submit" className="primary-button" disabled={busy || uploading}>{busy ? "Saving…" : editing ? "Save changes" : "Publish content"}</button>
            {editing && <button type="button" className="secondary-button" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
      </section>

      <section aria-labelledby="content-list-heading">
        <div className="mb-4 flex items-end justify-between gap-3"><div><p className="section-eyebrow">Live on the site</p><h2 id="content-list-heading" className="mt-1 text-lg font-bold text-ink-strong">Content items ({content.length})</h2></div></div>
        {content.length ? (
          <div className="space-y-3">
            {content.map((item) => (
              <article key={item.id} className="surface-card flex flex-wrap items-start justify-between gap-4 p-4 sm:p-5">
                <div className="min-w-0 flex-1">
                  <span className="status-pill">{sectionLabels[item.section_name]}</span>
                  <p className="mt-3 whitespace-pre-line text-sm font-semibold leading-5 text-ink-strong">{item.content_text || "No text set"}</p>
                  {item.media_url && <a href={item.media_url} target="_blank" rel="noreferrer" className="mt-2 block truncate text-xs font-semibold text-green hover:underline">{item.media_url}</a>}
                  <p className="mt-2 text-xs text-muted">Display order {item.display_order}</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button type="button" className="secondary-button min-h-9 px-3" onClick={() => editItem(item)} aria-label={`Edit ${sectionLabels[item.section_name]}`}><Pencil size={15} aria-hidden="true" /> Edit</button>
                  <button type="button" className="danger-button min-h-9 px-3" onClick={() => void remove(item)} aria-label={`Delete ${sectionLabels[item.section_name]}`}><Trash2 size={15} aria-hidden="true" /></button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="surface-card p-6 text-sm text-muted">No site content has been added yet.</div>
        )}
      </section>
    </div>
  )
}
