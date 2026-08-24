"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { quizzesApi } from "@/lib/api/quizzes";
import { extractErrorMessage } from "@/lib/api/client";
import { cn } from "@/lib/utils";

export default function StudentQuizAttemptPage() {
  const params = useParams<{ quizId: string }>();
  const router = useRouter();
  const quizFetch = useFetch(() => quizzesApi.get(params.quizId), [params.quizId]);
  const questionsFetch = useFetch(() => quizzesApi.questions(params.quizId), [params.quizId]);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    setSubmitting(true);
    try {
      await quizzesApi.submit(params.quizId, Object.entries(answers).map(([questionId, optionIndex]) => ({ questionId, optionIndex })));
      toast.success("Quiz submitted");
      router.push("/student/quizzes");
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (quizFetch.isLoading || questionsFetch.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-72" />
        {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32 w-full" />)}
      </div>
    );
  }

  if (quizFetch.error || questionsFetch.error || !quizFetch.data) {
    return <ErrorState message={quizFetch.error ?? questionsFetch.error ?? "Quiz not found"} onRetry={quizFetch.refetch} />;
  }

  const quiz = quizFetch.data;
  const questions = questionsFetch.data ?? [];
  const answeredCount = Object.keys(answers).length;

  return (
    <div>
      <PageHeader title={quiz.title} description={`${quiz.courseTitle} · ${quiz.durationMinutes} minutes · ${questions.length} questions`} />

      <div className="space-y-4">
        {questions.map((q, idx) => (
          <Card key={q.id}>
            <CardHeader><CardTitle className="text-base">Q{idx + 1}. {q.question}</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {q.options.map((option, optIdx) => (
                <button
                  key={optIdx}
                  onClick={() => setAnswers((prev) => ({ ...prev, [q.id]: optIdx }))}
                  className={cn(
                    "flex w-full items-center gap-3 rounded-md border px-4 py-2.5 text-left text-sm transition-colors",
                    answers[q.id] === optIdx ? "border-primary bg-primary/5 font-medium" : "hover:bg-muted/50"
                  )}
                >
                  <span
                    className={cn(
                      "flex h-5 w-5 shrink-0 items-center justify-center rounded-full border text-xs",
                      answers[q.id] === optIdx ? "border-primary bg-primary text-primary-foreground" : "border-muted-foreground/40"
                    )}
                  >
                    {String.fromCharCode(65 + optIdx)}
                  </span>
                  {option}
                </button>
              ))}
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="sticky bottom-4 mt-6 flex items-center justify-between rounded-lg border bg-background/95 p-4 shadow-md backdrop-blur">
        <p className="text-sm text-muted-foreground">{answeredCount} of {questions.length} answered</p>
        <Button onClick={handleSubmit} disabled={submitting || answeredCount === 0}>
          {submitting ? "Submitting..." : "Submit quiz"}
        </Button>
      </div>
    </div>
  );
}
