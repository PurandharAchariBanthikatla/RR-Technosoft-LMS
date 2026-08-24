import Link from "next/link";
import { GraduationCap, ShieldCheck, Sparkles, Users } from "lucide-react";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-brand-ink p-10 text-white lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{
            backgroundImage:
              "radial-gradient(circle at 20% 20%, rgba(227,30,36,0.35), transparent 40%), radial-gradient(circle at 80% 70%, rgba(227,30,36,0.25), transparent 45%)",
          }}
        />
        <Link href="/" className="relative z-10 flex items-center gap-2">
          <span className="flex h-10 w-10 items-center justify-center rounded-md bg-primary">
            <GraduationCap className="h-5 w-5" />
          </span>
          <span className="font-display text-lg font-bold tracking-wide">RR TECHNOSOFT</span>
        </Link>

        <div className="relative z-10 max-w-md">
          <h2 className="font-display text-3xl font-bold leading-tight">
            Learn. Build. Get Placed.
          </h2>
          <p className="mt-3 text-sm leading-relaxed text-white/70">
            One platform for live classes, hands-on projects, assignments, quizzes and a practice
            portal — built to take you from your first line of code to your first offer letter.
          </p>

          <div className="mt-8 space-y-4">
            <div className="flex items-start gap-3">
              <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-white/10">
                <Users className="h-4 w-4" />
              </span>
              <p className="text-sm text-white/80">
                Cohort-based learning with mentors tracking your progress every day.
              </p>
            </div>
            <div className="flex items-start gap-3">
              <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-white/10">
                <Sparkles className="h-4 w-4" />
              </span>
              <p className="text-sm text-white/80">
                A dedicated practice portal to sharpen your coding skills daily.
              </p>
            </div>
            <div className="flex items-start gap-3">
              <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-white/10">
                <ShieldCheck className="h-4 w-4" />
              </span>
              <p className="text-sm text-white/80">
                Verified certificates and direct access to placement drives.
              </p>
            </div>
          </div>
        </div>

        <p className="relative z-10 text-xs text-white/40">
          © {new Date().getFullYear()} RR TECHNOSOFT. All rights reserved.
        </p>
      </div>

      {/* Form panel */}
      <div className="flex items-center justify-center bg-background px-6 py-12">
        <div className="w-full max-w-sm">{children}</div>
      </div>
    </div>
  );
}
