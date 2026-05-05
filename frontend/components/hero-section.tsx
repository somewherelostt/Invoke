"use client"

import type { CSSProperties } from "react"
import { useEffect, useRef } from "react"
import { ArrowDown, ArrowRight, CheckCircle2, Github, Mic2, Sparkles, Workflow } from "lucide-react"

const navItems = [
  { label: "Problem", href: "#problem" },
  { label: "Features", href: "#features" },
  { label: "Real life", href: "#real-life" },
  { label: "How it works", href: "#how-it-works" },
  { label: "Local AI", href: "#local-ai" },
  { label: "Integrations", href: "#integrations" },
  { label: "Privacy", href: "#privacy" },
]

const commandRail = [
  {
    speech: "Clean this sentence and make it sound professional.",
    tool: "TEXT_CLEANUP",
    status: "ready",
  },
  {
    speech: "Create a GitHub issue for the login bug.",
    tool: "GITHUB_CREATE_ISSUE",
    status: "queued",
  },
  {
    speech: "Search the web for Android privacy changes.",
    tool: "COMPOSIO_SEARCH_WEB",
    status: "done",
  },
]

export function HeroSection() {
  const navRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const nav = navRef.current
    if (!nav) return
    const onScroll = () => {
      nav.classList.toggle("hero-nav--scrolled", window.scrollY > 24)
    }
    window.addEventListener("scroll", onScroll, { passive: true })
    return () => window.removeEventListener("scroll", onScroll)
  }, [])

  return (
    <section className="hero-shell relative min-h-screen overflow-hidden">
      {/* HERO MOTION STORYBOARD
       *   0ms static shell visible, no blocked CTA
       *  80ms brand/nav settles down
       * 160ms voice-action badge fades up
       * 260ms headline rises in
       * 420ms body copy and CTAs follow
       * 520ms console slides in from right
       * 700ms command cards stagger inside the console
       */}
      <header ref={navRef} className="hero-nav fixed top-0 left-0 z-50 w-full">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between px-6 py-5 md:px-10 lg:px-12">
          <a href="#" className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-primary shadow-sm">
              <Mic2 className="h-5 w-5" />
            </span>
            <span className="font-mono text-sm uppercase tracking-[0.24em] text-white">Invoke</span>
          </a>
          <nav className="hidden items-center gap-5 text-sm text-white/70 lg:flex">
            {navItems.map((item) => (
              <a key={item.href} href={item.href} className="transition-colors hover:text-white">
                {item.label}
              </a>
            ))}
          </nav>
          <a href="https://github.com/somewherelostt/Invoke" target="_blank" rel="noopener noreferrer" className="hidden items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-medium text-primary transition-transform hover:-translate-y-0.5 md:inline-flex">
            <Github className="h-4 w-4" />
            View GitHub
          </a>
        </div>
      </header>

      <div className="relative z-10 mx-auto grid min-h-screen w-full max-w-7xl gap-12 px-6 pb-16 pt-28 md:px-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:px-12">
        <div>
          <div className="hero-kicker mb-8 inline-flex items-center gap-2 rounded-full border border-white/12 bg-white/8 px-4 py-2 text-sm text-white/84 backdrop-blur">
            <Sparkles className="h-4 w-4 text-cyan-200" />
            Voice actions for every app
          </div>

          <h1 className="hero-title max-w-4xl text-white">
            Speak once.
            <br />
            <span className="text-white/72">Invoke handles the rest.</span>
          </h1>

          <p className="hero-copy mt-8 max-w-2xl text-xl leading-relaxed text-white/78 md:text-2xl">
            Turn speech into messages, notes, snippets, searches, and app actions. Use local AI through Ollama, connect tools through Composio, and keep control of your data with privacy-first setup.
          </p>

          <div className="hero-actions mt-10 flex flex-col gap-3 sm:flex-row">
            <a href="https://github.com/somewherelostt/Invoke" target="_blank" rel="noopener noreferrer" className="cta-primary group">
              View GitHub
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
            </a>
            <a href="#how-it-works" className="cta-secondary">
              See how it works
            </a>
          </div>
        </div>

        <div className="hero-console hero-console-entrance">
          <div className="console-topbar">
            <div className="flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full bg-red-300" />
              <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
              <span className="h-2.5 w-2.5 rounded-full bg-emerald-300" />
            </div>
            <span>local endpoint active</span>
          </div>

          <div className="voice-card">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm text-white/54">Listening</p>
                <p className="mt-1 text-xl text-white">Natural speech to structured actions</p>
              </div>
              <div className="mic-pulse">
                <Mic2 className="h-6 w-6" />
              </div>
            </div>
            <div className="waveform" aria-hidden="true">
              {Array.from({ length: 18 }).map((_, index) => (
                <span
                  key={index}
                  style={
                    {
                      "--bar": `${18 + ((index * 13) % 44)}px`,
                      "--wave-delay": `${index * 54}ms`,
                    } as CSSProperties
                  }
                />
              ))}
            </div>
          </div>

          <div className="space-y-3">
            {commandRail.map((item, index) => (
              <div
                key={item.tool}
                className="command-card"
                style={{ "--card-delay": `${760 + index * 120}ms` } as CSSProperties}
              >
                <p>"{item.speech}"</p>
                <div className="mt-4 flex items-center justify-between gap-3">
                  <span className="font-mono text-xs text-cyan-200">{item.tool}</span>
                  <span className="flex items-center gap-1.5 rounded-full bg-white/8 px-2.5 py-1 text-xs text-white/70">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-200" />
                    {item.status}
                  </span>
                </div>
              </div>
            ))}
          </div>

          <div className="route-strip">
            <Workflow className="h-4 w-4 text-cyan-200" />
            <span>Voice input &gt; Whisper &gt; Qwen 3 0.6B &gt; Composio action</span>
          </div>
        </div>
      </div>

      <a href="#problem" className="absolute bottom-7 left-1/2 z-10 -translate-x-1/2 animate-bounce text-white/45">
        <ArrowDown className="h-6 w-6" />
      </a>
    </section>
  )
}
