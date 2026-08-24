import Link from "next/link";
import { Clock, Layers, Star } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Course } from "@/types";

interface CourseCardProps {
  course: Course;
  progress?: number;
  href: string;
}

export function CourseCard({ course, progress, href }: CourseCardProps) {
  return (
    <Link href={href}>
      <Card className="group h-full overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-md">
        <div className="relative flex h-32 items-end bg-gradient-to-br from-brand-ink to-neutral-800 p-4">
          <Badge variant="secondary" className="absolute right-3 top-3 bg-white/15 text-white backdrop-blur">
            {course.level}
          </Badge>
          <p className="font-display text-lg font-bold text-white line-clamp-2">{course.title}</p>
        </div>
        <CardContent className="space-y-3 p-4">
          <p className="line-clamp-2 text-sm text-muted-foreground">{course.description}</p>

          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1"><Clock className="h-3.5 w-3.5" /> {course.durationWeeks}w</span>
            <span className="flex items-center gap-1"><Layers className="h-3.5 w-3.5" /> {course.moduleCount} modules</span>
            {course.rating && (
              <span className="flex items-center gap-1"><Star className="h-3.5 w-3.5 fill-warning text-warning" /> {course.rating.toFixed(1)}</span>
            )}
          </div>

          {typeof progress === "number" && (
            <div className="space-y-1 pt-1">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground">Progress</span>
                <span className="font-medium">{progress}%</span>
              </div>
              <Progress value={progress} className="h-1.5" />
            </div>
          )}
        </CardContent>
      </Card>
    </Link>
  );
}
