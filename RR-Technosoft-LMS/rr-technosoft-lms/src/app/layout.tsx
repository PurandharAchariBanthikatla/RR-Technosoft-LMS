import type { Metadata } from "next";
// Self-hosted via @fontsource-variable (bundled at npm-install time, zero network
// calls during `next build`). Previously used next/font/google, which fetches
// Inter/Space Grotesk/JetBrains Mono from fonts.googleapis.com at build time —
// that call would hang/retry-with-backoff and eventually fail on any host,
// CI runner, or Docker build with restricted/no internet egress. See globals.css
// for the --font-sans/--font-display/--font-mono variable mapping.
import "@fontsource-variable/inter";
import "@fontsource-variable/space-grotesk";
import "@fontsource-variable/jetbrains-mono";
import "./globals.css";
import { ThemeProvider } from "@/components/layout/theme-provider";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "RR TECHNOSOFT | Learning Management System",
  description:
    "RR TECHNOSOFT LMS — courses, live classes, assignments, quizzes, attendance, certificates and placements, all in one platform.",
  icons: { icon: "/favicon.ico" },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="font-sans">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem disableTransitionOnChange>
          {children}
          <Toaster richColors position="top-right" closeButton />
        </ThemeProvider>
      </body>
    </html>
  );
}
