"use client"

import type { CSSProperties } from "react"
import { ArrowRight, Github, Mic2, Sparkles, CheckCircle2 } from "lucide-react"

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
]

export function HeroSection() {
  return (
    <section className="hero-shell relative min-h-screen flex flex-col pt-6 pb-20">
      <header className="hero-nav relative z-10 mx-auto flex w-full max-w-7xl items-center justify-between px-6 py-4 md:px-12">
        <a href="#" className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-primary shadow-lg">
            <Mic2 className="h-5 w-5" />
          </div>
          <span className="font-mono text-sm uppercase tracking-[0.24em] text-white">Invoke</span>
        </a>
        <nav className="hidden items-center gap-6 text-xs font-bold uppercase tracking-widest text-white/50 lg:flex">
          {navItems.map((item) => (
            <a key={item.href} href={item.href} className="transition-colors hover:text-white">
              {item.label}
            </a>
          ))}
        </nav>
        <a href="https://github.com/somewherelostt/Invoke" target="_blank" className="flex items-center gap-2 rounded-full bg-white px-6 py-2.5 text-xs font-bold uppercase tracking-widest text-primary shadow-xl">
          <Github className="h-4 w-4" />
          View GitHub
        </a>
      </header>

      <div className="relative z-10 mx-auto grid w-full max-w-7xl flex-1 items-center gap-16 px-6 md:px-12 lg:grid-cols-[1fr_0.8fr]">
        <div className="max-w-2xl">
          <div className="hero-kicker mb-10 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-[10px] font-bold uppercase tracking-[0.2em] text-cyan-300">
            <Sparkles className="h-3 w-3" />
            Voice actions for every app
          </div>

          <h1 className="hero-title font-serif text-7xl md:text-8xl lg:text-9xl text-white leading-[0.85] tracking-tight mb-12">
            Speak once. <br />
            Invoke <br />
            <span className="italic text-white/50">handles the rest.</span>
          </h1>

          <p className="hero-copy text-xl md:text-2xl text-white/60 leading-relaxed max-w-xl">
            Turn speech into messages, notes, snippets, searches, and app actions. Use local AI to bridge the gap between intent and execution.
          </p>

          <div className="hero-actions mt-16 flex flex-col gap-6 sm:flex-row sm:items-center">
            <a href="https://github.com/somewherelostt/Invoke" target="_blank" className="inline-flex items-center gap-4 text-xl font-bold text-white group">
              Get Started
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-primary transition-transform group-hover:translate-x-2">
                <ArrowRight className="h-6 w-6" />
              </div>
            </a>
          </div>
        </div>

        <div className="hero-console-entrance hidden lg:block">
          <div className="relative rounded-[2rem] border border-white/10 bg-black/40 p-1 backdrop-blur-2xl shadow-2xl">
            <div className="rounded-[1.8rem] border border-white/5 bg-gradient-to-b from-white/[0.08] to-transparent p-8 md:p-10">
              <div className="mb-10 flex items-center justify-between">
                <div className="flex gap-2">
                  <div className="h-3 w-3 rounded-full bg-red-500/50" />
                  <div className="h-3 w-3 rounded-full bg-amber-500/50" />
                  <div className="h-3 w-3 rounded-full bg-emerald-500/50" />
                </div>
                <span className="text-[10px] font-mono uppercase tracking-widest text-white/30">local endpoint active</span>
              </div>

              <div className="space-y-8">
                <div className="rounded-2xl border border-white/5 bg-white/5 p-8">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-[10px] font-bold uppercase tracking-widest text-white/40">Listening</p>
                      <p className="mt-2 text-2xl font-medium text-white">Natural speech to structured actions</p>
                    </div>
                    <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary shadow-[0_0_50px_rgba(36,91,255,0.4)]">
                      <Mic2 className="h-7 w-7 text-white" />
                    </div>
                  </div>
                  <div className="mt-10 flex items-end gap-1.5 h-16" aria-hidden="true">
                    {[30, 60, 45, 80, 50, 40, 70, 55, 65, 45, 35, 50, 40, 60, 55].map((h, i) => (
                      <div 
                        key={i} 
                        className="flex-1 rounded-full bg-primary/40 animate-wave-breathe" 
                        style={{ height: `${h}%`, animationDelay: `${i * 80}ms` }} 
                      />
                    ))}
                  </div>
                </div>

                <div className="space-y-4">
                  {commandRail.map((item, i) => (
                    <div key={i} className="flex flex-col gap-4 rounded-2xl border border-white/5 bg-white/5 p-6 transition-colors hover:bg-white/10">
                      <p className="text-lg italic text-white/90">"{item.speech}"</p>
                      <div className="flex items-center justify-between border-t border-white/5 pt-4">
                        <span className="font-mono text-[10px] uppercase tracking-widest text-cyan-400/60">{item.tool}</span>
                        <div className="flex items-center gap-2 text-[10px] font-bold uppercase tracking-widest text-white/40">
                          <CheckCircle2 className="h-3 w-3 text-emerald-500" />
                          {item.status}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
