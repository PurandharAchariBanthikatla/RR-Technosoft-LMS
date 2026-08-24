"use client";

import { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Upload } from "lucide-react";
import { toast } from "sonner";

import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { learningResourceSchema, type LearningResourceInputForm } from "@/lib/validations/learning-resource";
import { learningResourcesApi } from "@/lib/api/learning-resources";
import { extractErrorMessage } from "@/lib/api/client";
import { LearningResource } from "@/types";

interface ResourceFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  resource?: LearningResource;
  onSaved: () => void;
}

export function ResourceFormDialog({ open, onOpenChange, resource, onSaved }: ResourceFormDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const {
    register, handleSubmit, control, reset, watch,
    formState: { errors, isSubmitting },
  } = useForm<LearningResourceInputForm>({ resolver: zodResolver(learningResourceSchema) });

  const resourceType = watch("resourceType");

  useEffect(() => {
    if (open) {
      setFile(null);
      reset(
        resource
          ? {
              title: resource.title,
              description: resource.description ?? "",
              resourceType: resource.resourceType,
              category: resource.category ?? "",
              externalUrl: resource.externalUrl ?? "",
            }
          : { resourceType: "DOCUMENT" }
      );
    }
  }, [open, resource, reset]);

  async function onSubmit(values: LearningResourceInputForm) {
    try {
      const payload = {
        title: values.title,
        description: values.description,
        resourceType: values.resourceType,
        category: values.category,
        externalUrl: values.externalUrl || undefined,
      };

      let saved: LearningResource;
      if (resource) {
        saved = await learningResourcesApi.update(resource.id, payload);
        toast.success("Resource updated");
      } else {
        saved = await learningResourcesApi.create(payload);
        toast.success("Resource created");
      }

      if (file) {
        setUploading(true);
        await learningResourcesApi.uploadFile(saved.id, file);
        setUploading(false);
      }
      onSaved();
    } catch (err) {
      toast.error(extractErrorMessage(err));
      setUploading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{resource ? "Edit resource" : "Add a learning resource"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="title">Title</Label>
            <Input id="title" placeholder="Spring Boot Cheat Sheet" {...register("title")} />
            {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Description</Label>
            <Textarea id="description" rows={3} {...register("description")} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Type</Label>
              <Controller
                control={control}
                name="resourceType"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {["DOCUMENT", "PDF", "PRESENTATION", "SPREADSHEET", "LINK", "ARCHIVE", "OTHER"].map((t) => (
                        <SelectItem key={t} value={t}>{t}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="category">Category</Label>
              <Input id="category" placeholder="Java" {...register("category")} />
            </div>
          </div>

          {resourceType === "LINK" ? (
            <div className="space-y-1.5">
              <Label htmlFor="externalUrl">External URL</Label>
              <Input id="externalUrl" placeholder="https://..." {...register("externalUrl")} />
              {errors.externalUrl && <p className="text-xs text-destructive">{errors.externalUrl.message}</p>}
            </div>
          ) : (
            <div className="space-y-1.5">
              <Label htmlFor="file">File {resource?.fileUrl ? "(replace existing)" : ""}</Label>
              <Input id="file" type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              <p className="text-xs text-muted-foreground">Upload now, or add the file later from the resource&apos;s row.</p>
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting || uploading}>
              {(isSubmitting || uploading) && <Loader2 className="h-4 w-4 animate-spin" />}
              {uploading ? "Uploading..." : <><Upload className="h-4 w-4" /> {resource ? "Save changes" : "Create resource"}</>}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
