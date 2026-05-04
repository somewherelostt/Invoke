import { ArrowRight, Github, Mic2 } from "lucide-react"

export function FooterSection() {
  return (
    <footer id="github" className="bg-ink px-6 py-20 text-white md:px-12 lg:px-20">
      <div className="mx-auto max-w-6xl">
        <div className="grid gap-10 border-b border-white/10 pb-16 lg:grid-cols-[1fr_0.75fr] lg:items-end">
          <div>
            <div className="mb-8 flex h-12 w-12 items-center justify-center rounded-xl bg-white text-primary">
              <Mic2 className="h-6 w-6" />
            </div>
            <h2 className="max-w-3xl text-white">Your voice command layer for work.</h2>
            <p className="mt-6 max-w-2xl text-lg text-white/68">
              Invoke is built for people who want voice to do more than dictate. Explore the code, follow development, and try the project from GitHub.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row lg:justify-end">
            <a href="https://github.com/somewherelostt/Invoke" target="_blank" rel="noopener noreferrer" className="cta-primary group">
              <Github className="h-4 w-4" />
              View GitHub
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
            </a>
            <a href="#how-it-works" className="cta-secondary cta-secondary-dark">
              See how it works
            </a>
          </div>
        </div>
        <div className="flex flex-col justify-between gap-4 pt-8 text-sm text-white/40 md:flex-row">
          <p>INVOKE - Voice actions for every app</p>
          <p>Local-first voice routing for desktop, Windows, and Android.</p>
        </div>
      </div>
    </footer>
  )
}
