-- =====================================================================
-- RR TECHNOSOFT LMS — Notifications module wiring (V14)
-- No structural change needed; notifications already matches the JPA
-- model below. Adds the indexes the new query paths need.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id) WHERE is_read = FALSE;
