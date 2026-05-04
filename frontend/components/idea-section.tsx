"use client"

import { ReactNode } from "react"
import { SectionWrapper } from "./section-wrapper"
import { cn } from "@/lib/utils"
import { 
  Users, 
  TrendingUp, 
  Globe, 
  Shield, 
  Layers, 
  Droplets, 
  Palette,
  LucideIcon
} from "lucide-react"

// Icon mapping for each idea
const iconMap: Record<number, LucideIcon> = {
  1: Users,
  2: TrendingUp,
  3: Globe,
  4: Shield,
  5: Layers,
  6: Droplets,
  7: Palette,
}

interface IdeaSectionProps {
  number: number
  title: string
  children: ReactNode
  variant?: "default" | "alternate"
}

export function IdeaSection({ 
  number, 
  title, 
  children,
  variant = "default"
}: IdeaSectionProps) {
  const Icon = iconMap[number] || Users
  const paddedNumber = String(number).padStart(2, '0')

  return (
    <SectionWrapper 
      id={`idea-${number}`} 
      variant={variant}
    >
      <div className="idea-card rounded-2xl p-8 md:p-12">
        <div className="flex flex-col gap-6">
          {/* Header */}
          <div className="flex items-start gap-4">
            <div className="flex items-center justify-center w-14 h-14 rounded-xl bg-royal-blue/10 text-royal-blue shrink-0">
              <Icon className="w-7 h-7" />
            </div>
            <div className="flex-1">
              <span className="idea-number inline-block mb-3">
                {paddedNumber}
              </span>
              <h2 className={cn(
                "text-midnight",
                variant === "alternate" && "text-midnight"
              )}>
                {title}
              </h2>
            </div>
          </div>
          
          {/* Content */}
          <div className="space-y-6">
            {children}
          </div>
        </div>
      </div>
    </SectionWrapper>
  )
}

// Sub-component for structured content within idea sections
interface IdeaFieldProps {
  label: string
  children: ReactNode
  highlight?: boolean
}

export function IdeaField({ label, children, highlight }: IdeaFieldProps) {
  return (
    <div className={cn(
      "p-4 rounded-lg",
      highlight ? "bg-royal-blue/5 border border-royal-blue/20" : "bg-secondary/50"
    )}>
      <h4 className="text-sm font-semibold text-royal-blue uppercase tracking-wider mb-2 font-sans">
        {label}
      </h4>
      <div className="text-foreground/90 leading-relaxed">
        {children}
      </div>
    </div>
  )
}
