"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ShieldAlert, Loader2, Plus, Trash2, Pencil, Database } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { masterDataApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import {
  masterDataCategorySchema, type MasterDataCategoryInput,
  masterDataItemSchema, type MasterDataItemInput,
} from "@/lib/validations/administration";
import { MasterDataItem } from "@/types";

export default function MasterDataPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data: categories, isLoading, error, refetch } = useFetch(() => masterDataApi.listCategories(), []);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [categoryDialogOpen, setCategoryDialogOpen] = useState(false);
  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<MasterDataItem | null>(null);
  const [deleteItemTarget, setDeleteItemTarget] = useState<MasterDataItem | null>(null);

  useEffect(() => {
    if (!selectedCategoryId && categories && categories.length > 0) {
      setSelectedCategoryId(categories[0].id);
    }
  }, [categories, selectedCategoryId]);

  const selectedCategory = categories?.find((c) => c.id === selectedCategoryId) ?? null;

  const {
    data: items,
    isLoading: itemsLoading,
    refetch: refetchItems,
  } = useFetch(
    () => (selectedCategoryId ? masterDataApi.listItems(selectedCategoryId) : Promise.resolve([])),
    [selectedCategoryId]
  );

  const categoryForm = useForm<MasterDataCategoryInput>({ resolver: zodResolver(masterDataCategorySchema) });
  const itemForm = useForm<MasterDataItemInput>({ resolver: zodResolver(masterDataItemSchema) });

  async function onCreateCategory(values: MasterDataCategoryInput) {
    try {
      const created = await masterDataApi.createCategory(values);
      toast.success(`Category "${created.name}" created`);
      setCategoryDialogOpen(false);
      categoryForm.reset();
      refetch();
      setSelectedCategoryId(created.id);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  function openCreateItem() {
    setEditingItem(null);
    itemForm.reset({ code: "", label: "", description: "", sortOrder: 0 });
    setItemDialogOpen(true);
  }

  function openEditItem(item: MasterDataItem) {
    setEditingItem(item);
    itemForm.reset({ code: item.code, label: item.label, description: item.description ?? "", sortOrder: item.sortOrder });
    setItemDialogOpen(true);
  }

  async function onSubmitItem(values: MasterDataItemInput) {
    if (!selectedCategoryId) return;
    try {
      if (editingItem) {
        await masterDataApi.updateItem(editingItem.id, {
          label: values.label,
          description: values.description,
          sortOrder: values.sortOrder,
        });
        toast.success("Item updated");
      } else {
        await masterDataApi.createItem(selectedCategoryId, values);
        toast.success("Item added");
      }
      setItemDialogOpen(false);
      refetchItems();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function toggleItemActive(item: MasterDataItem, isActive: boolean) {
    try {
      await masterDataApi.updateItem(item.id, { label: item.label, isActive });
      refetchItems();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function handleDeleteItem() {
    if (!deleteItemTarget) return;
    try {
      await masterDataApi.removeItem(deleteItemTarget.id);
      toast.success("Item removed");
      setDeleteItemTarget(null);
      refetchItems();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can manage master data." />;
  }

  return (
    <div>
      <PageHeader
        title="Master Data"
        description="Lookup lists used across the platform — departments, designations, skill tags, and more."
        actions={
          <Button onClick={() => setCategoryDialogOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" /> New Category
          </Button>
        }
      />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-96 w-full" />}

      {categories && !error && (
        categories.length === 0 ? (
          <EmptyState icon={Database} title="No categories yet" description="Create your first master data category to get started." />
        ) : (
          <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
            <Card className="h-fit">
              <CardContent className="p-2">
                {categories.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setSelectedCategoryId(c.id)}
                    className={`flex w-full flex-col rounded-md px-3 py-2 text-left text-sm transition-colors ${
                      c.id === selectedCategoryId ? "bg-red-600 text-white" : "hover:bg-muted"
                    }`}
                  >
                    <span className="font-medium">{c.name}</span>
                    <span className={c.id === selectedCategoryId ? "text-xs text-white/80" : "text-xs text-muted-foreground"}>
                      {c.code}
                    </span>
                  </button>
                ))}
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                {selectedCategory && (
                  <div className="mb-4 flex items-center justify-between">
                    <div>
                      <h3 className="font-display text-base font-semibold">{selectedCategory.name}</h3>
                      {selectedCategory.description && (
                        <p className="text-sm text-muted-foreground">{selectedCategory.description}</p>
                      )}
                    </div>
                    <Button size="sm" variant="outline" onClick={openCreateItem} className="gap-2">
                      <Plus className="h-4 w-4" /> Add Item
                    </Button>
                  </div>
                )}

                {itemsLoading && <Skeleton className="h-40 w-full" />}

                {items && !itemsLoading && items.length === 0 && (
                  <EmptyState icon={Database} title="No items yet" description="Add items to this category." />
                )}

                {items && !itemsLoading && items.length > 0 && (
                  <div className="divide-y">
                    {items.map((item) => (
                      <div key={item.id} className="flex items-center justify-between gap-4 py-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-medium">{item.label}</span>
                            <Badge variant="outline" className="text-[10px]">{item.code}</Badge>
                          </div>
                          {item.description && <p className="text-xs text-muted-foreground">{item.description}</p>}
                        </div>
                        <div className="flex shrink-0 items-center gap-3">
                          <Switch checked={item.isActive} onCheckedChange={(v) => toggleItemActive(item, v)} />
                          <Button size="icon" variant="ghost" onClick={() => openEditItem(item)}>
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button size="icon" variant="ghost" onClick={() => setDeleteItemTarget(item)}>
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )
      )}

      <Dialog open={categoryDialogOpen} onOpenChange={setCategoryDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New Master Data Category</DialogTitle>
            <DialogDescription>Categories group related lookup items, e.g. Departments or Skill Tags.</DialogDescription>
          </DialogHeader>
          <form onSubmit={categoryForm.handleSubmit(onCreateCategory)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="code">Code</Label>
              <Input id="code" placeholder="e.g. VENDOR_TYPE" {...categoryForm.register("code")} />
              {categoryForm.formState.errors.code && (
                <p className="text-xs text-destructive">{categoryForm.formState.errors.code.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="name">Name</Label>
              <Input id="name" {...categoryForm.register("name")} />
              {categoryForm.formState.errors.name && (
                <p className="text-xs text-destructive">{categoryForm.formState.errors.name.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="description">Description</Label>
              <Input id="description" {...categoryForm.register("description")} />
            </div>
            <DialogFooter>
              <Button type="submit" disabled={categoryForm.formState.isSubmitting} className="gap-2">
                {categoryForm.formState.isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Create
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={itemDialogOpen} onOpenChange={setItemDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingItem ? "Edit Item" : "Add Item"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={itemForm.handleSubmit(onSubmitItem)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="item-code">Code</Label>
              <Input id="item-code" disabled={!!editingItem} {...itemForm.register("code")} />
              {itemForm.formState.errors.code && (
                <p className="text-xs text-destructive">{itemForm.formState.errors.code.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="item-label">Label</Label>
              <Input id="item-label" {...itemForm.register("label")} />
              {itemForm.formState.errors.label && (
                <p className="text-xs text-destructive">{itemForm.formState.errors.label.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="item-description">Description</Label>
              <Input id="item-description" {...itemForm.register("description")} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="item-sort">Sort Order</Label>
              <Input id="item-sort" type="number" {...itemForm.register("sortOrder")} />
            </div>
            <DialogFooter>
              <Button type="submit" disabled={itemForm.formState.isSubmitting} className="gap-2">
                {itemForm.formState.isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {editingItem ? "Save" : "Add"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteItemTarget}
        onOpenChange={(open) => !open && setDeleteItemTarget(null)}
        title="Remove item?"
        description={`"${deleteItemTarget?.label}" will be permanently removed.`}
        confirmLabel="Remove"
        onConfirm={handleDeleteItem}
      />
    </div>
  );
}
