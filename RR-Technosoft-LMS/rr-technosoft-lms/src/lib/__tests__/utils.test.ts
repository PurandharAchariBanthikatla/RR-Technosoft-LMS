import { cn, formatDate, formatTime, initials, truncate, formatCurrency } from "@/lib/utils";

describe("cn", () => {
  it("merges class names and resolves Tailwind conflicts (last wins)", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });

  it("drops falsy values", () => {
    expect(cn("a", false, undefined, null, "b")).toBe("a b");
  });
});

describe("formatDate", () => {
  it("formats an ISO string as DD MMM YYYY", () => {
    expect(formatDate("2026-03-15T10:00:00Z")).toMatch(/15 Mar 2026/);
  });

  it("accepts a Date object directly", () => {
    expect(formatDate(new Date("2026-01-01T00:00:00Z"))).toMatch(/01 Jan 2026|31 Dec 2025/);
  });
});

describe("formatTime", () => {
  it("formats a time in hour:minute form", () => {
    expect(formatTime("2026-03-15T10:30:00Z")).toMatch(/\d{1,2}:\d{2}/);
  });
});

describe("initials", () => {
  it("takes the first letter of the first two words", () => {
    expect(initials("Priya Sharma")).toBe("PS");
  });

  it("handles a single-word name", () => {
    expect(initials("Madonna")).toBe("M");
  });

  it("ignores extra whitespace between words", () => {
    expect(initials("  Kiran   Kumar  ")).toBe("KK");
  });

  it("only uses the first two words for a longer name", () => {
    expect(initials("Anil Kumar Reddy")).toBe("AK");
  });
});

describe("truncate", () => {
  it("leaves short text untouched", () => {
    expect(truncate("hello", 80)).toBe("hello");
  });

  it("truncates long text and appends an ellipsis", () => {
    const long = "a".repeat(100);
    const result = truncate(long, 10);
    expect(result.length).toBe(11); // 10 chars + ellipsis
    expect(result.endsWith("…")).toBe(true);
  });
});

describe("formatCurrency", () => {
  it("formats an amount as INR by default", () => {
    expect(formatCurrency(1500)).toContain("1,500");
  });

  it("formats with no fractional digits", () => {
    expect(formatCurrency(999.99)).not.toContain(".");
  });
});
