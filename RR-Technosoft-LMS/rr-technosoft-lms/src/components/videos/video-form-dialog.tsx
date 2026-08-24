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
import { videoResourceSchema, type VideoResourceInputForm } from "@/lib/validations/video";
import { videosApi } from "@/lib/api/videos";
import { extractErrorMessage } from "@/lib/api/client";
import { VideoResource } from "@/types";

interface VideoFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  video?: VideoResource;
  onSaved: () => void;
}

export function VideoFormDialog({ open, onOpenChange, video, onSaved }: VideoFormDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const {
    register, handleSubmit, control, reset, watch,
    formState: { errors, isSubmitting },
  } = useForm<VideoResourceInputForm>({ resolver: zodResolver(videoResourceSchema) });

  const source = watch("source");

  useEffect(() => {
    if (open) {
      setFile(null);
      reset(
        video
          ? {
              title: video.title,
              description: video.description ?? "",
              category: video.category ?? "",
              source: video.source,
              videoUrl: video.videoUrl ?? "",
              thumbnailUrl: video.thumbnailUrl ?? "",
              durationSeconds: video.durationSeconds,
            }
          : { source: "UPLOAD" }
      );
    }
  }, [open, video, reset]);

  async function onSubmit(values: VideoResourceInputForm) {
    try {
      const payload = {
        title: values.title,
        description: values.description,
        category: values.category,
        source: values.source,
        videoUrl: values.videoUrl || undefined,
        thumbnailUrl: values.thumbnailUrl || undefined,
        durationSeconds: values.durationSeconds,
      };

      let saved: VideoResource;
      if (video) {
        saved = await videosApi.update(video.id, payload);
        toast.success("Video updated");
      } else {
        if (values.source === "UPLOAD" && !file) {
          toast.error("Choose a video file to upload, or switch to a YouTube/External link");
          return;
        }
        saved = await videosApi.create(payload);
        toast.success("Video added");
      }

      if (file) {
        setUploading(true);
        await videosApi.uploadFile(saved.id, file);
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
          <DialogTitle>{video ? "Edit video" : "Add a video"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="title">Title</Label>
            <Input id="title" placeholder="Intro to REST APIs" {...register("title")} />
            {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Description</Label>
            <Textarea id="description" rows={3} {...register("description")} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Source</Label>
              <Controller
                control={control}
                name="source"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="UPLOAD">Upload file</SelectItem>
                      <SelectItem value="YOUTUBE">YouTube</SelectItem>
                      <SelectItem value="EXTERNAL">External link</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="category">Category</Label>
              <Input id="category" placeholder="Web Development" {...register("category")} />
            </div>
          </div>

          {source === "UPLOAD" ? (
            <div className="space-y-1.5">
              <Label htmlFor="file">Video file {video?.videoUrl ? "(replace existing)" : ""}</Label>
              <Input id="file" type="file" accept="video/*" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
            </div>
          ) : (
            <div className="space-y-1.5">
              <Label htmlFor="videoUrl">Video URL</Label>
              <Input id="videoUrl" placeholder="https://youtube.com/watch?v=..." {...register("videoUrl")} />
              {errors.videoUrl && <p className="text-xs text-destructive">{errors.videoUrl.message}</p>}
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="thumbnailUrl">Thumbnail URL (optional)</Label>
            <Input id="thumbnailUrl" placeholder="https://..." {...register("thumbnailUrl")} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="durationSeconds">Duration (seconds)</Label>
            <Input id="durationSeconds" type="number" min={0} {...register("durationSeconds")} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting || uploading}>
              {(isSubmitting || uploading) && <Loader2 className="h-4 w-4 animate-spin" />}
              {uploading ? "Uploading..." : <><Upload className="h-4 w-4" /> {video ? "Save changes" : "Add video"}</>}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
