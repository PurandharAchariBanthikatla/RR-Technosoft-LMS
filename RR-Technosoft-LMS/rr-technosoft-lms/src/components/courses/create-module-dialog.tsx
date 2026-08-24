"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2 } from "lucide-react";

interface CreateModuleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (title: string) => Promise<void>;
}

export function CreateModuleDialog({ open, onOpenChange, onCreate }: CreateModuleDialogProps) {
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    if (title.trim().length < 2) return;
    setSubmitting(true);
    try {
      await onCreate(title.trim());
      setTitle("");
      onOpenChange(false);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add module</DialogTitle>
          <DialogDescription>Modules break a course into sections you can add lessons to.</DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label htmlFor="module-title">Module title</Label>
          <Input
            id="module-title"
            placeholder="e.g. Getting started"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={submitting || title.trim().length < 2}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            Add module
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
