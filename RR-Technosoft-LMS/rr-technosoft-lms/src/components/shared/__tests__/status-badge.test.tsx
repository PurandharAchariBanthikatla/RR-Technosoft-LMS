import { render, screen } from "@testing-library/react";
import { StatusBadge } from "@/components/shared/status-badge";

describe("StatusBadge", () => {
  it("renders the mapped label for a known status", () => {
    render(<StatusBadge status="ACTIVE" />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("renders a distinct label for a destructive status", () => {
    render(<StatusBadge status="SUSPENDED" />);
    expect(screen.getByText("Suspended")).toBeInTheDocument();
  });

  it("falls back to the raw status string for an unmapped value", () => {
    render(<StatusBadge status="SOME_UNKNOWN_STATUS" />);
    expect(screen.getByText("SOME_UNKNOWN_STATUS")).toBeInTheDocument();
  });

  it("renders finance statuses added for the Finance module", () => {
    render(<StatusBadge status="PARTIALLY_REFUNDED" />);
    expect(screen.getByText("Partially refunded")).toBeInTheDocument();
  });
});
