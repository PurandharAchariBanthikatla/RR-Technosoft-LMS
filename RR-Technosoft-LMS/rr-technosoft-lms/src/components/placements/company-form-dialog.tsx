"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { companySchema, type CompanyInput } from "@/lib/validations/company";
import { companiesApi } from "@/lib/api/companies";
import { extractErrorMessage } from "@/lib/api/client";
import { Company } from "@/types";

interface CompanyFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  company?: Company;
  onSaved: () => void;
}

export function CompanyFormDialog({ open, onOpenChange, company, onSaved }: CompanyFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CompanyInput>({ resolver: zodResolver(companySchema) });

  useEffect(() => {
    if (open) {
      reset(
        company
          ? {
              name: company.name,
              logoUrl: company.logoUrl ?? "",
              website: company.website ?? "",
              industry: company.industry ?? "",
              description: company.description ?? "",
              contactPersonName: company.contactPersonName ?? "",
              contactEmail: company.contactEmail ?? "",
              contactPhone: company.contactPhone ?? "",
              address: company.address ?? "",
            }
          : { name: "" }
      );
    }
  }, [open, company, reset]);

  async function onSubmit(values: CompanyInput) {
    try {
      if (company) {
        await companiesApi.update(company.id, values);
        toast.success("Company updated");
      } else {
        await companiesApi.create(values);
        toast.success("Company added");
      }
      onSaved();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{company ? "Edit company" : "Add a company"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="name">Company name</Label>
              <Input id="name" placeholder="Acme Corp" {...register("name")} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="industry">Industry</Label>
              <Input id="industry" placeholder="Software" {...register("industry")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="website">Website</Label>
              <Input id="website" placeholder="https://acme.com" {...register("website")} />
              {errors.website && <p className="text-xs text-destructive">{errors.website.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="logoUrl">Logo URL</Label>
              <Input id="logoUrl" placeholder="https://..." {...register("logoUrl")} />
              {errors.logoUrl && <p className="text-xs text-destructive">{errors.logoUrl.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" rows={3} {...register("description")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="contactPersonName">Contact person</Label>
              <Input id="contactPersonName" {...register("contactPersonName")} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="contactPhone">Contact phone</Label>
              <Input id="contactPhone" {...register("contactPhone")} />
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="contactEmail">Contact email</Label>
              <Input id="contactEmail" placeholder="hr@acme.com" {...register("contactEmail")} />
              {errors.contactEmail && <p className="text-xs text-destructive">{errors.contactEmail.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="address">Address</Label>
              <Textarea id="address" rows={2} {...register("address")} />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {company ? "Save changes" : "Add company"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
