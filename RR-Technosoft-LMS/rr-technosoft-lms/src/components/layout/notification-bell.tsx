"use client";

import { useRouter } from "next/navigation";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { useFetch } from "@/hooks/use-fetch";
import { notificationsApi } from "@/lib/api/notifications";
import { formatDate } from "@/lib/utils";
import { cn } from "@/lib/utils";

/** Fills in the previously-dead Bell button in Topbar — see notifications.ts / NotificationController. */
export function NotificationBell() {
  const router = useRouter();
  const countFetch = useFetch(() => notificationsApi.unreadCount(), []);
  const listFetch = useFetch(() => notificationsApi.list({ page: 0, size: 8 }), []);

  async function handleMarkAllRead() {
    await notificationsApi.markAllRead();
    countFetch.refetch();
    listFetch.refetch();
  }

  async function handleItemClick(id: string, alreadyRead: boolean, link?: string) {
    if (!alreadyRead) {
      await notificationsApi.markRead(id);
      countFetch.refetch();
      listFetch.refetch();
    }
    if (link) router.push(link);
  }

  const unread = countFetch.data ?? 0;
  const notifications = listFetch.data?.content ?? [];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Notifications" className="relative">
          <Bell className="h-[1.1rem] w-[1.1rem]" />
          {unread > 0 && (
            <Badge
              variant="destructive"
              className="absolute -right-1 -top-1 h-4 min-w-4 justify-center rounded-full px-1 text-[10px] leading-none"
            >
              {unread > 9 ? "9+" : unread}
            </Badge>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <div className="flex items-center justify-between px-2 py-1.5">
          <DropdownMenuLabel className="p-0 text-sm font-medium">Notifications</DropdownMenuLabel>
          {unread > 0 && (
            <button onClick={handleMarkAllRead} className="text-xs text-muted-foreground hover:text-foreground">
              Mark all read
            </button>
          )}
        </div>
        <DropdownMenuSeparator />
        {notifications.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-muted-foreground">No notifications yet</p>
        ) : (
          <div className="max-h-96 overflow-y-auto">
            {notifications.map((n) => (
              <DropdownMenuItem
                key={n.id}
                className={cn("flex flex-col items-start gap-0.5 whitespace-normal py-2", !n.read && "bg-primary/5")}
                onClick={() => handleItemClick(n.id, n.read, n.link)}
              >
                <div className="flex w-full items-center gap-2">
                  {!n.read && <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />}
                  <p className="text-sm font-medium">{n.title}</p>
                </div>
                {n.body && <p className="text-xs text-muted-foreground">{n.body}</p>}
                <p className="text-[11px] text-muted-foreground">{formatDate(n.createdAt)}</p>
              </DropdownMenuItem>
            ))}
          </div>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
