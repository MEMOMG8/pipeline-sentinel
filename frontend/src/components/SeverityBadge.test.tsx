import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SeverityBadge } from "@/components/SeverityBadge";

describe("SeverityBadge", () => {
  it("renders HIGH severity distinctly", () => {
    render(<SeverityBadge severity="HIGH" />);

    expect(screen.getByText("HIGH")).toHaveClass("text-red-700");
  });

  it("renders MEDIUM severity distinctly", () => {
    render(<SeverityBadge severity="MEDIUM" />);

    expect(screen.getByText("MEDIUM")).toHaveClass("text-amber-700");
  });

  it("renders no severity when max severity is null", () => {
    render(<SeverityBadge severity={null} />);

    expect(screen.getByText("None")).toBeInTheDocument();
  });
});
