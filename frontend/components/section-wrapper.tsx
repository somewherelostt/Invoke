"use client"

import { useEffect, useRef, ReactNode } from "react"
import { cn } from "@/lib/utils"

interface SectionWrapperProps {
  children: ReactNode
  className?: string
  id?: string
  variant?: "default" | "alternate" | "royal"
}

export function SectionWrapper({ 
  children, 
  className, 
  id,
  variant = "default" 
}: SectionWrapperProps) {
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
    alternate: "bg-off-white",
    royal: "bg-royal-blue text-white"
  }

  return (
    <section
      ref={sectionRef}
      id={id}
      className={cn(
        "section-reveal py-20 md:py-32 px-6 md:px-12 lg:px-20",
        variantStyles[variant],
        className
      )}
    >
      <div className="max-w-6xl mx-auto">
        {children}
      </div>
    </section>
  )
}
