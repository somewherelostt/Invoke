"use client"

import { useEffect, useRef, ReactNode } from "react"
import { cn } from "@/lib/utils"

interface SectionProps extends React.HTMLAttributes<HTMLElement> {
  children: ReactNode
  variant?: "default" | "alternate" | "royal" | "transparent"
  containerClass?: string
}

export function Section({
  children,
  className,
  id,
  variant = "default",
  containerClass,
  ...props
}: SectionProps) {
  const sectionRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("visible")
          }
        })
      },
      { threshold: 0.1 }
    )

    if (sectionRef.current) {
      observer.observe(sectionRef.current)
    }

    return () => observer.disconnect()
  }, [])

  const variantStyles = {
    default: "bg-background",
    alternate: "bg-muted/40",
    royal: "bg-royal-blue text-white",
    transparent: "bg-transparent",
  }

  return (
    <section
      ref={sectionRef}
      id={id}
      className={cn(
        "section-reveal py-24 px-6 md:px-12 lg:px-20",
        variantStyles[variant],
        className
      )}
      {...props}
    >
      <div className={cn("max-w-7xl mx-auto w-full", containerClass)}>
        {children}
      </div>
    </section>
  )
}
