"use client"

import { useState } from "react"
import Link from "next/link"
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUpRight } from "lucide-react"
import type { SiteContent } from "@/lib/types"

type Slide = {
  id: string
  title: string
  copy: string
  mediaUrl: string | null
}

function toSlide(item: SiteContent): Slide {
  const lines = item.content_text.split("\n").map((line) => line.trim()).filter(Boolean)
  return {
    id: item.id,
    title: lines[0] ?? "Draws with clear terms",
    copy: lines.slice(1).join(" ") || "Review the ticket price, ticket limit, and draw details before you enter.",
    mediaUrl: item.media_url,
  }
}

function youtubeEmbed(url: string): string | null {
  try {
    const parsed = new URL(url)
    const videoId = parsed.hostname.includes("youtu.be")
      ? parsed.pathname.slice(1)
      : parsed.searchParams.get("v")
    return videoId && /^[A-Za-z0-9_-]{11}$/.test(videoId)
      ? `https://www.youtube-nocookie.com/embed/${videoId}`
      : null
  } catch {
    return null
  }
}

function isVideo(url: string): boolean {
  return /\.(mp4|webm)(?:[?#].*)?$/i.test(url)
}

export function HeroCarousel({ items }: { items: SiteContent[] }) {
  const authoredSlides = items.filter((item) => item.section_name === "hero_banner" || item.section_name === "videos")
  const slides: Slide[] = authoredSlides.length > 0
    ? authoredSlides.map(toSlide)
    : [{
        id: "welcome",
        title: "Clear terms. Every draw.",
        copy: "Review ticket limits before you join. Completed draws stay in a public archive.",
        mediaUrl: "/draw-hero.png",
      }]
  const [activeIndex, setActiveIndex] = useState(0)
  const activeSlide = slides[activeIndex % slides.length]
  const embedUrl = activeSlide.mediaUrl ? youtubeEmbed(activeSlide.mediaUrl) : null

  function move(direction: number) {
    setActiveIndex((current) => (current + direction + slides.length) % slides.length)
  }

  return (
    <section className="hero-panel" aria-roledescription="carousel" aria-label="Promotions">
      <div className="hero-media" aria-hidden={!activeSlide.mediaUrl}>
        {embedUrl ? (
          <iframe
            src={embedUrl}
            title={activeSlide.title}
            allow="accelerometer; encrypted-media; gyroscope; picture-in-picture; web-share"
            referrerPolicy="strict-origin-when-cross-origin"
            allowFullScreen
          />
        ) : activeSlide.mediaUrl && isVideo(activeSlide.mediaUrl) ? (
          <video src={activeSlide.mediaUrl} controls playsInline aria-label={activeSlide.title} />
        ) : (
          <img
            src={activeSlide.mediaUrl || "/draw-hero.png"}
            alt={activeSlide.mediaUrl ? activeSlide.title : "Emerald draw ticket and gold raffle ball on an ivory surface"}
          />
        )}
      </div>
      <div className="hero-content">
        <p className="mb-4 flex items-center gap-2 text-[0.68rem] font-extrabold tracking-[0.18em] text-[#ead08b]">
          <span className="h-px w-7 bg-[#d9b565]" aria-hidden="true" />
          LUCKY DRAW · PAKISTAN
        </p>
        <h1 className="hero-title">{activeSlide.title}</h1>
        <p className="hero-copy mt-4">{activeSlide.copy}</p>
        <div className="hero-actions mt-5 flex flex-wrap items-center gap-2">
          <Link href="/draws" className="inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-[#e0c273] px-4 text-sm font-extrabold text-[#153d2d] transition hover:bg-[#ecd895]">
            Explore active draws <ArrowUpRight size={16} aria-hidden="true" />
          </Link>
          <a href="#guidelines" className="inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-white/25 px-3 text-sm font-semibold text-white transition hover:bg-white/10">
            How it works <ArrowDown size={14} aria-hidden="true" />
          </a>
        </div>
        <div className="mt-6 flex items-center justify-between gap-4">
          <div className="hero-dots" aria-label={`Slide ${activeIndex + 1} of ${slides.length}`}>
            {slides.map((slide, index) => (
              <button
                key={slide.id}
                type="button"
                className="hero-dot"
                aria-label={`Show promotion ${index + 1}`}
                aria-current={index === activeIndex}
                onClick={() => setActiveIndex(index)}
              />
            ))}
          </div>
          {slides.length > 1 && (
            <div className="flex gap-2">
              <button type="button" className="grid size-9 place-items-center rounded-full border border-white/25 text-white transition hover:bg-white/10" onClick={() => move(-1)} aria-label="Previous promotion">
                <ArrowLeft size={16} aria-hidden="true" />
              </button>
              <button type="button" className="grid size-9 place-items-center rounded-full border border-white/25 text-white transition hover:bg-white/10" onClick={() => move(1)} aria-label="Next promotion">
                <ArrowRight size={16} aria-hidden="true" />
              </button>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
