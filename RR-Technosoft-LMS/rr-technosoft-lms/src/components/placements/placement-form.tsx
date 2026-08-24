"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { placementSchema, type PlacementInput } from "@/lib/validations/placement";
import { placementsApi } from "@/lib/api/placements";
import { companiesApi } from "@/lib/api/companies";
import { extractErrorMessage } from "@/lib/api/client";
import { useFetch } from "@/hooks/use-fetch";
import { PlacementDrive } from "@/types";

interface PlacementFormProps {
  placement?: PlacementDrive;
}

function toCsv(values?: string[]) {
  return values && values.length > 0 ? values.join(", ") : "";
}

function fromCsv(value?: string): string[] {
  return (value ?? "")
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}

export function PlacementForm({ placement }: PlacementFormProps) {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const { data: companies } = useFetch(() => companiesApi.list({ page: 0, size: 100, isActive: true }), []);

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = useForm<PlacementInput>({
    resolver: zodResolver(placementSchema),
    defaultValues: placement
      ? {
          companyId: placement.companyId,
          companyName: placement.companyName,
          companyLogoUrl: placement.companyLogoUrl ?? "",
          role: placement.role,
          description: placement.description ?? "",
          eligibility: placement.eligibility ?? "",
          skillsRequired: toCsv(placement.skillsRequired),
          allowedBranches: toCsv(placement.allowedBranches),
          salaryMin: placement.salaryMin,
          salaryMax: placement.salaryMax,
          minCgpa: placement.minCgpa,
          location: placement.location,
          jobType: placement.jobType,
          driveDate: placement.driveDate?.slice(0, 10),
          lastDateToApply: placement.lastDateToApply?.slice(0, 10),
          applicationLink: placement.applicationLink ?? "",
        }
      : { jobType: "FULL_TIME" },
  });

  const companyId = watch("companyId");

  async function onSubmit(values: PlacementInput) {
    setSubmitting(true);
    try {
      const payload = {
        companyId: values.companyId || undefined,
        companyName: values.companyName || undefined,
        companyLogoUrl: values.companyLogoUrl || undefined,
        role: values.role,
        description: values.description,
        eligibility: values.eligibility,
        skillsRequired: fromCsv(values.skillsRequired),
        allowedBranches: fromCsv(values.allowedBranches),
        salaryMin: values.salaryMin,
        salaryMax: values.salaryMax,
        minCgpa: values.minCgpa,
        location: values.location,
        jobType: values.jobType,
        driveDate: values.driveDate || undefined,
        lastDateToApply: values.lastDateToApply,
        applicationLink: values.applicationLink || undefined,
      };

      if (placement) {
        await placementsApi.update(placement.id, payload);
        toast.success("Drive updated");
        router.push(`/admin/placements/${placement.id}`);
      } else {
        const created = await placementsApi.create(payload);
        toast.success("Drive created");
        router.push(`/admin/placements/${created.id}`);
      }
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card>
      <CardContent className="p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Company</Label>
              <Controller
                control={control}
                name="companyId"
                render={({ field }) => (
                  <Select value={field.value ?? ""} onValueChange={field.onChange}>
                    <SelectTrigger><SelectValue placeholder="Select a registered company (optional)" /></SelectTrigger>
                    <SelectContent>
                      {(companies?.content ?? []).map((c) => (
                        <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="companyName">Company name {companyId ? "(auto-filled)" : ""}</Label>
              <Input
                id="companyName"
                placeholder="Only needed if not selecting a registered company above"
                disabled={!!companyId}
                {...register("companyName")}
              />
              {errors.companyName && <p className="text-xs text-destructive">{errors.companyName.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="role">Role</Label>
              <Input id="role" placeholder="Software Engineer" {...register("role")} />
              {errors.role && <p className="text-xs text-destructive">{errors.role.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="description">Job description</Label>
              <Textarea id="description" rows={3} {...register("description")} />
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="eligibility">Eligibility criteria</Label>
              <Textarea id="eligibility" rows={2} placeholder="e.g. B.Tech CSE/IT, no active backlogs" {...register("eligibility")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="skillsRequired">Skills required (comma-separated)</Label>
              <Input id="skillsRequired" placeholder="Java, Spring Boot, SQL" {...register("skillsRequired")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="allowedBranches">Allowed branches (comma-separated)</Label>
              <Input id="allowedBranches" placeholder="CSE, IT, ECE" {...register("allowedBranches")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="location">Location</Label>
              <Input id="location" placeholder="Bengaluru" {...register("location")} />
            </div>

            <div className="space-y-1.5">
              <Label>Job type</Label>
              <Controller
                control={control}
                name="jobType"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FULL_TIME">Full-time</SelectItem>
                      <SelectItem value="INTERNSHIP">Internship</SelectItem>
                      <SelectItem value="PART_TIME">Part-time</SelectItem>
                      <SelectItem value="CONTRACT">Contract</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="salaryMin">Salary min (LPA)</Label>
              <Input id="salaryMin" type="number" step="0.1" min={0} {...register("salaryMin")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="salaryMax">Salary max (LPA)</Label>
              <Input id="salaryMax" type="number" step="0.1" min={0} {...register("salaryMax")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="minCgpa">Minimum CGPA</Label>
              <Input id="minCgpa" type="number" step="0.1" min={0} max={10} {...register("minCgpa")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="driveDate">Drive date</Label>
              <Input id="driveDate" type="date" {...register("driveDate")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="lastDateToApply">Application deadline</Label>
              <Input id="lastDateToApply" type="date" {...register("lastDateToApply")} />
              {errors.lastDateToApply && <p className="text-xs text-destructive">{errors.lastDateToApply.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="applicationLink">External application link (optional)</Label>
              <Input id="applicationLink" placeholder="https://..." {...register("applicationLink")} />
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t pt-5">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {placement ? "Save changes" : "Create drive"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
