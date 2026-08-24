"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { Play, Send } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { practiceApi } from "@/lib/api/practice";
import { extractErrorMessage } from "@/lib/api/client";
import { cn } from "@/lib/utils";

const LANGUAGE_STARTERS: Record<string, string> = {
  javascript: "function solve(input) {\n  // write your solution here\n}\n",
  python: "def solve(input):\n    # write your solution here\n    pass\n",
  java: "class Solution {\n    public void solve() {\n        // write your solution here\n    }\n}\n",
};

export default function PracticeProblemPage() {
  const params = useParams<{ problemId: string }>();
  const { data: problem, isLoading, error, refetch } = useFetch(() => practiceApi.get(params.problemId), [params.problemId]);
  const [language, setLanguage] = useState("javascript");
  const [code, setCode] = useState(LANGUAGE_STARTERS.javascript);
  const [running, setRunning] = useState(false);

  function handleLanguageChange(lang: string) {
    setLanguage(lang);
    setCode(LANGUAGE_STARTERS[lang] ?? "");
  }

  async function handleSubmit() {
    setRunning(true);
    try {
      await practiceApi.submit(params.problemId, { language, code });
      toast.success("Submission received");
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setRunning(false);
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-72" />
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <Skeleton className="h-96 w-full" />
          <Skeleton className="h-96 w-full" />
        </div>
      </div>
    );
  }

  if (error || !problem) {
    return <ErrorState message={error ?? "Problem not found"} onRetry={refetch} />;
  }

  return (
    <div>
      <PageHeader
        title={problem.title}
        description={`${problem.topic} · ${problem.successRate}% success rate`}
        actions={
          <Badge
            variant="outline"
            className={cn(
              problem.difficulty === "EASY" && "text-success border-success/40",
              problem.difficulty === "MEDIUM" && "text-warning border-warning/40",
              problem.difficulty === "HARD" && "text-destructive border-destructive/40"
            )}
          >
            {problem.difficulty}
          </Badge>
        }
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardContent className="prose prose-sm max-w-none p-6 dark:prose-invert">
            <p className="whitespace-pre-line text-sm leading-relaxed">{problem.description}</p>
          </CardContent>
        </Card>

        <Card className="flex flex-col">
          <CardContent className="flex flex-1 flex-col gap-3 p-4">
            <div className="flex items-center justify-between">
              <Select value={language} onValueChange={handleLanguageChange}>
                <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="javascript">JavaScript</SelectItem>
                  <SelectItem value="python">Python</SelectItem>
                  <SelectItem value="java">Java</SelectItem>
                </SelectContent>
              </Select>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" className="gap-2" disabled={running}>
                  <Play className="h-4 w-4" /> Run
                </Button>
                <Button size="sm" className="gap-2" onClick={handleSubmit} disabled={running}>
                  <Send className="h-4 w-4" /> {running ? "Submitting..." : "Submit"}
                </Button>
              </div>
            </div>

            <textarea
              value={code}
              onChange={(e) => setCode(e.target.value)}
              spellCheck={false}
              className="min-h-[320px] flex-1 rounded-md border bg-brand-ink p-4 font-mono text-sm text-white shadow-inner focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
