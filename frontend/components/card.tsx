import { ReactNode } from "react"
import { cn } from "@/lib/utils"

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  variant?: "primary" | "secondary" | "dark"
}

export function Card({ children, className, variant = "primary", ...props }: CardProps) {
  return (
    <article
      className={cn(
        "rounded-2xl p-6 md:p-8",
        "transition-all duration-200 hover:-translate-y-1 hover:shadow-lg",
        variant === "primary" &&
        "border border-border/80 bg-white/80 shadow-sm backdrop-blur-sm hover:border-primary/30",
        variant === "secondary" &&
        "border border-border/40 bg-muted/40 shadow-sm hover:bg-muted/60 hover:border-border/80",
        variant === "dark" &&
        "border border-white/10 bg-white/5 shadow-sm backdrop-blur-md hover:border-white/20 hover:bg-white/10 text-white",
        className
      )}
      {...props}
    >
      {children}
    </article>
  )
}
