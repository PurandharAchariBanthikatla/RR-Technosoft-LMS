import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DataTable, type Column } from "@/components/shared/data-table";

interface Row {
  id: string;
  name: string;
}

const columns: Column<Row>[] = [
  { key: "name", header: "Name", render: (r) => r.name },
];

describe("DataTable", () => {
  it("shows skeleton rows while loading", () => {
    const { container } = render(
      <DataTable columns={columns} data={[]} isLoading rowKey={(r) => r.id} />
    );
    expect(container.querySelectorAll(".animate-pulse").length).toBeGreaterThan(0);
    // no table should render while loading
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("shows the empty state when there is no data", () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        rowKey={(r) => r.id}
        emptyTitle="Nothing here"
        emptyDescription="Add a record to get started."
      />
    );
    expect(screen.getByText("Nothing here")).toBeInTheDocument();
    expect(screen.getByText("Add a record to get started.")).toBeInTheDocument();
  });

  it("renders one row per data item with the column header and cell", () => {
    const data: Row[] = [{ id: "1", name: "Alpha" }, { id: "2", name: "Beta" }];
    render(<DataTable columns={columns} data={data} rowKey={(r) => r.id} />);

    expect(screen.getByText("Name")).toBeInTheDocument();
    expect(screen.getByText("Alpha")).toBeInTheDocument();
    expect(screen.getByText("Beta")).toBeInTheDocument();
    expect(screen.getAllByRole("row")).toHaveLength(3); // header + 2 rows
  });

  it("invokes onRowClick with the clicked row", async () => {
    const user = userEvent.setup();
    const onRowClick = jest.fn();
    const data: Row[] = [{ id: "1", name: "Alpha" }];
    render(<DataTable columns={columns} data={data} rowKey={(r) => r.id} onRowClick={onRowClick} />);

    await user.click(screen.getByText("Alpha"));

    expect(onRowClick).toHaveBeenCalledWith(data[0]);
  });
});
