import { ReactNode } from "react"
import { cn } from "@/lib/utils"

interface FeatureGridProps extends React.HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  columns?: 2 | 3 | 4
}

export function FeatureGrid({ children, className, columns = 2, ...props }: FeatureGridProps) {
  return (
    <div
      className={cn(
        "stagger-grid grid gap-6",
        columns === 2 && "md:grid-cols-2",
        columns === 3 && "md:grid-cols-2 lg:grid-cols-3",
        columns === 4 && "md:grid-cols-2 lg:grid-cols-4",
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}
