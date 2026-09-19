import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RunDetailPage from "@/app/runs/[runId]/page";

vi.mock("@/components/RunDetailView", () => ({
  RunDetailView: ({ runId }: { runId: string }) => <div>detail run id: {runId}</div>,
}));

describe("RunDetailPage", () => {
  it("awaits the dynamic route params before rendering the detail view", async () => {
    const page = await RunDetailPage({
      params: Promise.resolve({
        runId: "5329e083-0972-403c-9414-9b49c02a0b25",
      }),
    });

    render(page);

    expect(screen.getByText("detail run id: 5329e083-0972-403c-9414-9b49c02a0b25")).toBeInTheDocument();
  });
});
