import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Inbox } from "lucide-react";
import { EmptyState } from "@/components/shared/empty-state";

describe("EmptyState", () => {
  it("renders the title and description", () => {
    render(<EmptyState icon={Inbox} title="No records found" description="Try adjusting your filters." />);
    expect(screen.getByText("No records found")).toBeInTheDocument();
    expect(screen.getByText("Try adjusting your filters.")).toBeInTheDocument();
  });

  it("omits the description when none is given", () => {
    render(<EmptyState icon={Inbox} title="No records found" />);
    expect(screen.queryByText("Try adjusting your filters.")).not.toBeInTheDocument();
  });

  it("does not render an action button when no handler is provided", () => {
    render(<EmptyState icon={Inbox} title="No records found" />);
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("invokes onAction when the action button is clicked", async () => {
    const user = userEvent.setup();
    const onAction = jest.fn();
    render(<EmptyState icon={Inbox} title="No records found" actionLabel="Create one" onAction={onAction} />);

    await user.click(screen.getByRole("button", { name: "Create one" }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });
});
