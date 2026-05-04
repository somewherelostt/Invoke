import type { CSSProperties } from "react"
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
import { SectionWrapper } from "@/components/section-wrapper"

const actionExamples = [
  "Search the web for the latest Kotlin Android updates.",
  "Create a GitHub issue titled login crash.",
  "Draft an email to Maya about the invoice.",
  "Clean this sentence and make it sound professional.",
  "Save my email address as a snippet.",
  "Send a Slack message saying I'll be five minutes late.",
]

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

const integrations = [
  { icon: Mail, name: "Gmail", command: "Draft an email to Alex about the project update." },
  { icon: CalendarDays, name: "Calendar", command: "What meetings do I have tomorrow?" },
  { icon: Github, name: "GitHub", command: "Create an issue for the login button bug." },
  { icon: MessageSquare, name: "Slack", command: "Tell the team I'm joining late." },
  { icon: Code2, name: "Notion", command: "Create a note from this idea." },
  { icon: Search, name: "Web Search", command: "Find the best local AI models for Android." },
]

const realLifeMoments = [
  {
    icon: TrainFront,
    label: "Crowded commute",
    title: "Review a PR while standing in a packed train.",
    scene: "You do not have a seat, your laptop is closed, and the train is loud. Open the Android mic bubble, use an earbud mic, and say what you want reviewed.",
    command: "Review the auth PR, summarize the risky files, and draft one comment about token refresh.",
    outcome: "Invoke transcribes, classifies the GitHub action, drafts the review note, and asks before posting.",
    proof: "Noise-aware capture + confirmation",
  },
  {
    icon: Headphones,
    label: "Shared office",
    title: "Whisper a polished reply without disturbing anyone.",
    scene: "You are in an open office or library and need to answer quickly. Speak quietly into a close mic and let Invoke clean the wording.",
    command: "Reply to Sarah that I can join tomorrow, make it warm and professional.",
    outcome: "Invoke drafts the message with your work style preset and keeps private mode local-first.",
    proof: "Whisper-friendly dictation",
  },
  {
    icon: Plane,
    label: "Between gates",
    title: "Turn a passing thought into an organized task before it disappears.",
    scene: "You are walking through an airport with one hand free. Instead of opening Notion, Todoist, and Calendar, capture the whole workflow by voice.",
    command: "Save this as a product idea, create a follow-up task for Friday, and search examples of Android voice bubbles.",
    outcome: "Invoke routes each part to the right connected tool and shows a clear action summary.",
    proof: "Multi-step routing",
  },
]

function ProblemSection() {
  return (
    <SectionWrapper id="problem" variant="default">
      <div className="grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-end">
        <div>
          <span className="section-kicker">The Problem</span>
          <h2 className="mt-4 max-w-3xl text-ink">Typing, switching apps, and copying text slows everything down.</h2>
        </div>
        <div className="space-y-4 text-lg text-muted-foreground">
          <p>
            Most productivity tools still make you open five windows, type a prompt, copy the result, and paste it somewhere else.
          </p>
          <p>
            Dictation apps stop at text. Invoke goes further: it understands what you meant, picks the right route, and helps finish the action.
          </p>
        </div>
      </div>
    </SectionWrapper>
  )
}

function FeatureSection() {
  return (
    <SectionWrapper id="features" variant="alternate">
      <div className="mb-12 max-w-3xl">
        <span className="section-kicker">What Invoke Does</span>
        <h2 className="mt-4 text-ink">Use your voice like a command bar.</h2>
        <p className="mt-5 text-lg text-muted-foreground">
          Invoke understands natural speech and turns it into structured actions. Dictate anywhere, clean up rough thoughts, save reusable snippets, and trigger workflows across connected apps.
        </p>
      </div>
      <div className="stagger-grid grid gap-4 md:grid-cols-2">
        {features.map((feature) => {
          const Icon = feature.icon
          return (
            <article key={feature.title} className="surface-card group p-6">
              <div className="mb-8 flex h-11 w-11 items-center justify-center rounded-lg bg-primary/10 text-primary transition-transform duration-300 group-hover:-translate-y-1">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="text-xl text-ink">{feature.title}</h3>
              <p className="mt-3 text-muted-foreground">{feature.body}</p>
            </article>
          )
        })}
      </div>
    </SectionWrapper>
  )
}

function RealLifeSection() {
  return (
    <SectionWrapper id="real-life" variant="default">
      <div className="mb-12 grid gap-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-end">
        <div>
          <span className="section-kicker">Real Life</span>
          <h2 className="mt-4 text-ink">Built for the moments where typing falls apart.</h2>
        </div>
        <p className="text-lg text-muted-foreground">
          Invoke is not only for quiet desks. It is for commutes, shared spaces, airport gates, and rushed work moments where voice is the fastest input you still control.
        </p>
      </div>

      <div className="scenario-grid grid gap-5 lg:grid-cols-3">
        {realLifeMoments.map((moment, index) => {
          const Icon = moment.icon
          return (
            <article key={moment.title} className="scenario-card" style={{ "--scenario-delay": `${index * 90}ms` } as CSSProperties}>
              <div className="scenario-media">
                <div className="scenario-orbit" />
                <div className="scenario-icon">
                  <Icon className="h-6 w-6" />
                </div>
                <div className="scenario-proof">{moment.proof}</div>
              </div>
              <div className="p-6">
                <span className="font-mono text-xs uppercase tracking-[0.16em] text-primary">{moment.label}</span>
                <h3 className="mt-4 text-2xl text-ink">{moment.title}</h3>
                <p className="mt-4 text-sm text-muted-foreground">{moment.scene}</p>
                <div className="mt-6 rounded-xl bg-muted p-4 text-sm text-ink">"{moment.command}"</div>
                <div className="mt-4 flex items-start gap-3 text-sm text-muted-foreground">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                  <span>{moment.outcome}</span>
                </div>
              </div>
            </article>
          )
        })}
      </div>
    </SectionWrapper>
  )
}

function HowItWorksSection() {
  return (
    <SectionWrapper id="how-it-works" variant="default">
      <div className="grid gap-10 lg:grid-cols-[0.85fr_1.15fr] lg:items-center">
        <div>
          <span className="section-kicker">How It Works</span>
          <h2 className="mt-4 text-ink">Speak naturally. Invoke routes your words into actions.</h2>
          <p className="mt-5 text-lg text-muted-foreground">
            The core architecture is simple: voice input, speech-to-text, local intent classification, tool execution, then a result or completed action.
          </p>
        </div>
        <div className="pipeline-panel action-pipeline">
          {pipeline.map((step, index) => (
            <div key={step.label} className="pipeline-step">
              <div className="pipeline-index">{String(index + 1).padStart(2, "0")}</div>
              <div>
                <h3 className="text-lg text-ink">{step.label}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{step.body}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </SectionWrapper>
  )
}

function LocalAiSection() {
  return (
    <SectionWrapper id="local-ai" variant="royal">
      <div className="grid gap-8 lg:grid-cols-[1fr_0.9fr] lg:items-center">
        <div>
          <span className="section-kicker section-kicker-light">Local AI</span>
          <h2 className="mt-4 text-white">Powered by Qwen 3 0.6B through Ollama.</h2>
          <p className="mt-5 text-lg text-white/72">
            Invoke does not use Qwen as a big chatbot. It uses the model for one focused job: understanding what the user wants and converting speech into structured actions.
          </p>
          <div className="mt-8 grid gap-3 sm:grid-cols-2">
            {["Small enough to run locally", "Fast for intent classification", "No cloud LLM required for routing", "Upgradeable for larger models"].map((item) => (
              <div key={item} className="flex items-center gap-3 text-sm text-white/84">
                <CheckCircle2 className="h-4 w-4 text-cyan-200" />
                {item}
              </div>
            ))}
          </div>
        </div>
        <div className="code-card">
          <div className="mb-5 flex items-center justify-between border-b border-white/10 pb-4">
            <span className="font-mono text-xs uppercase tracking-[0.18em] text-cyan-200">Intent payload</span>
            <span className="rounded-full bg-emerald-300/12 px-3 py-1 text-xs text-emerald-100">local</span>
          </div>
          <pre>{`User:
"Search the web for OpenAI official website"

Qwen 3 0.6B:
{
  tool: "COMPOSIO_SEARCH_WEB",
  parameters: {
    query: "OpenAI official website"
  }
}

Invoke:
execute(action)`}</pre>
        </div>
      </div>
    </SectionWrapper>
  )
}

function IntegrationsSection() {
  return (
    <SectionWrapper id="integrations" variant="alternate">
      <div className="mb-12 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-2xl">
          <span className="section-kicker">Integrations</span>
          <h2 className="mt-4 text-ink">One voice layer for the tools you already use.</h2>
        </div>
        <p className="max-w-md text-muted-foreground">
          Composio lets Invoke connect spoken intent to real app actions, from developer workflows to everyday messages.
        </p>
      </div>
      <div className="stagger-grid grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {integrations.map((integration) => {
          const Icon = integration.icon
          return (
            <article key={integration.name} className="surface-card p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Icon className="h-5 w-5" />
                </div>
                <h3 className="text-lg text-ink">{integration.name}</h3>
              </div>
              <p className="mt-5 rounded-lg bg-muted p-4 text-sm text-muted-foreground">"{integration.command}"</p>
            </article>
          )
        })}
      </div>
    </SectionWrapper>
  )
}

function PlatformsSection() {
  return (
    <SectionWrapper id="platforms" variant="default">
      <div className="mb-12 max-w-2xl">
        <span className="section-kicker">Desktop + Android</span>
        <h2 className="mt-4 text-ink">One assistant across computer and phone.</h2>
      </div>
      <div className="stagger-grid grid gap-5 lg:grid-cols-2">
        <article className="platform-card">
          <Monitor className="h-7 w-7 text-primary" />
          <h3>Windows desktop app</h3>
          <p>Tauri desktop app with global shortcuts, local Whisper transcription, Ollama settings, Qwen 3 0.6B model support, and Composio actions.</p>
        </article>
        <article className="platform-card">
          <Smartphone className="h-7 w-7 text-primary" />
          <h3>Android voice bubble</h3>
          <p>Kotlin Android app concept with floating mic access, permissions onboarding, privacy mode, dictionary, styles, snippets, and advanced setup when needed.</p>
        </article>
      </div>
    </SectionWrapper>
  )
}

function PrivacySection() {
  return (
    <SectionWrapper id="privacy" variant="alternate">
      <div className="privacy-band">
        <div>
          <ShieldCheck className="mb-8 h-10 w-10 text-primary" />
          <span className="section-kicker">Privacy</span>
          <h2 className="mt-4 text-ink">Private by design.</h2>
        </div>
        <p className="max-w-2xl text-lg text-muted-foreground">
          Invoke supports local AI through Ollama, so core intent classification can run on your own machine. Privacy mode keeps data stored on your device. Cloud sync and app integrations are optional and controlled by the user.
        </p>
      </div>
    </SectionWrapper>
  )
}

function ExamplesSection() {
  return (
    <SectionWrapper id="examples" variant="default">
      <div className="mb-10 max-w-2xl">
        <span className="section-kicker">Examples</span>
        <h2 className="mt-4 text-ink">Real commands, real outcomes.</h2>
      </div>
      <div className="command-list command-list-animated">
        {actionExamples.map((command, index) => (
          <div key={command} className="command-row">
            <span>{String(index + 1).padStart(2, "0")}</span>
            <p>"{command}"</p>
            <ArrowRight className="h-4 w-4" />
          </div>
        ))}
      </div>
    </SectionWrapper>
  )
}

export default function Home() {
  return (
    <main className="min-h-screen">
      <HeroSection />
      <ProblemSection />
      <FeatureSection />
      <RealLifeSection />
      <HowItWorksSection />
      <LocalAiSection />
      <IntegrationsSection />
      <PlatformsSection />
      <PrivacySection />
      <ExamplesSection />
      <FooterSection />
    </main>
  )
}
