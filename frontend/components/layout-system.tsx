import { ReactNode } from "react"
import { cn } from "@/lib/utils"

export function Container({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn("max-w-5xl mx-auto px-6 md:px-12 w-full", className)}>
      {children}
    </div>
  )
}

interface SectionProps {
  children: ReactNode
  id?: string
  className?: string
  variant?: "default" | "muted"
}

export function Section({ children, id, className, variant = "default" }: SectionProps) {
  return (
    <section 
      id={id} 
      className={cn(
        "py-24", 
        variant === "muted" && "bg-muted/30",
        className
      )}
    >
      <Container>
        {children}
      </Container>
    </section>
  )
}

export function Label({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <span className={cn("text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-4 block", className)}>
      {children}
    </span>
  )
}

export function Heading({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <h2 className={cn("text-3xl md:text-4xl font-semibold tracking-tight text-ink", className)}>
      {children}
    </h2>
  )
}

export function Subtext({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <p className={cn("text-base md:text-lg text-muted-foreground leading-relaxed", className)}>
      {children}
    </p>
  )
}
