import {
  ArrowRight,
  Brain,
  CalendarDays,
  CheckCircle2,
  Code2,
  Github,
  Headphones,
  Mail,
  MessageSquare,
  Mic2,
  Monitor,
  Plane,
  Search,
  ShieldCheck,
  Sparkles,
  Smartphone,
  TrainFront,
  Workflow,
} from "lucide-react"
import { HeroSection } from "@/components/hero-section"
import { FooterSection } from "@/components/footer-section"
import { Section, Label, Heading, Subtext } from "@/components/layout-system"
import { cn } from "@/lib/utils"

const features = [
  {
    icon: Mic2,
    title: "Voice actions across apps",
    body: "Speak naturally and turn commands into messages, notes, snippets, searches, and tool actions.",
  },
  {
    icon: Brain,
    title: "Local intent routing",
    body: "Qwen 3 0.6B runs through Ollama for fast local classification without needing a cloud LLM for core routing.",
  },
  {
    icon: Workflow,
    title: "Composio tool execution",
    body: "Connect Gmail, GitHub, Slack, Calendar, Notion, Todoist, Docs, and web search through one action layer.",
  },
  {
    icon: Sparkles,
    title: "Writing cleanup and snippets",
    body: "Dictate rough thoughts, clean tone, apply style presets, and save reusable phrases or shortcuts.",
  },
]

const pipeline = [
  { label: "Record", body: "Invoke captures your voice from desktop or Android." },
  { label: "Transcribe", body: "Whisper converts speech into text locally." },
  { label: "Classify", body: "Qwen 3 0.6B maps intent to a structured action." },
  { label: "Execute", body: "Composio or local tools complete the task." },
]

const useCases = [
  {
    label: "Crowded commute",
    title: "Review a PR while standing in a packed train.",
    desc: "You do not have a seat, your laptop is closed, and the train is loud. Open the Android mic bubble, use an earbud mic, and say what you want reviewed.",
    command: "Review the auth PR, summarize the risky files, and draft one comment about token refresh.",
    outcome: "Invoke transcribes, classifies the GitHub action, drafts the review note, and asks before posting.",
  },
  {
    icon: Headphones,
    label: "Shared office",
    title: "Whisper a polished reply without disturbing anyone.",
    desc: "You are in an open office or library and need to answer quickly. Speak quietly into a close mic and let Invoke clean the wording.",
    command: "Reply to Sarah that I can join tomorrow, make it warm and professional.",
    outcome: "Invoke drafts the message with your work style preset and keeps private mode local-first.",
  },
  {
    icon: Plane,
    label: "Between gates",
    title: "Turn a passing thought into an organized task before it disappears.",
    desc: "You are walking through an airport with one hand free. Instead of opening Notion, Todoist, and Calendar, capture the whole workflow by voice.",
    command: "Save this as a product idea, create a follow-up task for Friday, and search examples of Android voice bubbles.",
    outcome: "Invoke routes each part to the right connected tool and shows a clear action summary.",
  },
]

const integrations = [
  { icon: Mail, name: "Gmail" },
  { icon: CalendarDays, name: "Calendar" },
  { icon: Github, name: "GitHub" },
  { icon: MessageSquare, name: "Slack" },
  { icon: Code2, name: "Notion" },
  { icon: Search, name: "Web Search" },
]

function ProblemSection() {
  return (
    <Section id="problem" className="relative overflow-hidden bg-black/20">
      <div className="absolute inset-0 bg-[radial-gradient(rgba(255,255,255,0.03)_1px,transparent_1px)] [background-size:32px_32px]" />
      <div className="max-w-2xl space-y-6 relative z-10">
        <Label className="text-cyan-400">The Problem</Label>
        <Heading className="text-white">Typing and switching apps slows you down.</Heading>
        <Subtext className="text-xl text-white/60">
          Most productivity tools still make you open five windows, type a prompt, copy the result, and paste it somewhere else. Invoke understands what you meant and helps finish the action locally.
        </Subtext>
      </div>
    </Section>
  )
}

function SolutionSection() {
  return (
    <Section id="solution" className="bg-primary/5">
      <div className="max-w-3xl space-y-6">
        <Label className="text-cyan-400">The Solution</Label>
        <Heading className="text-white">Voice as a command bar.</Heading>
        <Subtext className="text-white/60">
          Invoke turns natural speech into structured actions. Dictate anywhere, clean up rough thoughts, and trigger workflows across all your connected apps instantly.
        </Subtext>
      </div>
    </Section>
  )
}

function FeaturesSection() {
  return (
    <Section id="features">
      <div className="space-y-20">
        <div className="max-w-2xl space-y-4">
          <Label className="text-cyan-400">Features</Label>
          <Heading className="text-white">Built for speed and <span className="text-primary-light italic">absolute privacy.</span></Heading>
        </div>
        <div className="grid md:grid-cols-2 gap-x-20 gap-y-16">
          {features.map((feature, idx) => (
            <div key={feature.title} className="group space-y-6">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/5 ring-1 ring-white/10 group-hover:bg-primary/20 transition-all duration-500">
                <feature.icon className="h-6 w-6 text-white" />
              </div>
              <div className="space-y-3">
                <h3 className="text-2xl font-bold text-white tracking-tight">{feature.title}</h3>
                <p className="text-base text-white/40 leading-relaxed max-w-sm">{feature.body}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Section>
  )
}

function UseCaseSection() {
  return (
    <Section id="real-life" className="bg-black/20">
      <div className="space-y-16">
        <div className="max-w-2xl space-y-4">
          <Label className="text-cyan-400">Use Cases</Label>
          <Heading className="text-white">Everywhere typing falls apart.</Heading>
        </div>
        <div className="space-y-12 max-w-5xl">
          {useCases.map((moment) => (
            <div key={moment.title} className="grid gap-12 lg:grid-cols-2 lg:items-center py-12 border-t border-white/5 first:border-0">
              <div className="space-y-6">
                <span className="font-mono text-[10px] font-bold uppercase tracking-widest text-primary-light">{moment.label}</span>
                <h3 className="text-3xl font-bold text-white tracking-tight">{moment.title}</h3>
                <p className="text-white/50 leading-relaxed">{moment.desc}</p>
                <a href="#" className="inline-flex items-center gap-2 text-sm font-semibold text-primary-light hover:translate-x-1 transition-transform">
                  See how it works <ArrowRight className="h-4 w-4" />
                </a>
              </div>
              
              <div className="relative group/card">
                <div className="absolute -inset-2 bg-primary/20 blur-xl opacity-0 group-hover/card:opacity-100 transition-opacity duration-500" />
                <div className="relative rounded-2xl bg-white/5 p-8 shadow-sm ring-1 ring-white/10 transition-all duration-300 group-hover/card:bg-white/[0.08] group-hover/card:-translate-y-1">
                  <div className="flex items-center gap-3 border-b border-white/5 pb-4 mb-6">
                    <div className="h-2 w-2 rounded-full bg-primary animate-pulse" />
                    <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/40">Command</p>
                  </div>
                  <p className="text-lg font-medium text-white italic leading-snug">"{moment.command}"</p>
                  <div className="mt-6 flex items-start gap-4 rounded-xl bg-white/5 p-5">
                    <ArrowRight className="mt-1 h-4 w-4 shrink-0 text-primary-light" />
                    <p className="text-sm leading-relaxed text-white/60">{moment.outcome}</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Section>
  )
}

function IntegrationsSection() {
  return (
    <Section id="integrations">
      <div className="space-y-16">
        <div className="max-w-2xl space-y-4">
          <Label className="text-cyan-400">Integrations</Label>
          <Heading className="text-white">One layer for all your tools.</Heading>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-8">
          {integrations.map((item) => (
            <div key={item.name} className="flex items-center gap-4 group cursor-default">
              <div className="flex h-8 w-8 items-center justify-center rounded bg-white/5 text-white/40 group-hover:text-white transition-colors">
                <item.icon className="h-4 w-4" />
              </div>
              <span className="text-sm font-medium text-white/40 group-hover:text-white transition-colors">{item.name}</span>
            </div>
          ))}
        </div>
      </div>
    </Section>
  )
}

function CallToAction() {
  return (
    <Section className="pb-32 border-t border-white/5">
      <div className="max-w-2xl space-y-8">
        <Heading className="text-4xl md:text-5xl lg:text-6xl font-serif text-white tracking-tight">Private by design.</Heading>
        <Subtext className="text-xl text-white/50">
          Invoke supports local AI through Ollama, so core intent classification can run on your own machine.
        </Subtext>
        <div className="flex flex-col sm:flex-row gap-4 pt-4">
          <a href="https://github.com/somewherelostt/Invoke" target="_blank" className="px-8 py-4 bg-white text-primary rounded-full font-bold hover:bg-white/90 transition-colors text-center">
            Get Started
          </a>
        </div>
      </div>
    </Section>
  )
}

export default function Home() {
  return (
    <main className="min-h-screen bg-[#08111F] text-white selection:bg-primary/30">
      <HeroSection />
      <ProblemSection />
      <SolutionSection />
      <FeaturesSection />
      <UseCaseSection />
      <IntegrationsSection />
      <CallToAction />
      <FooterSection />
    </main>
  )
}
