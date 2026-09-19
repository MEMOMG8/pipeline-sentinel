import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { UploadCard } from "@/components/UploadCard";
import { submitValidationRun } from "@/lib/api";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    submitValidationRun: vi.fn(),
  };
});

const mockedSubmitValidationRun = vi.mocked(submitValidationRun);

describe("UploadCard", () => {
  beforeEach(() => {
    mockedSubmitValidationRun.mockReset();
  });

  it("shows an API error state when upload fails", async () => {
    mockedSubmitValidationRun.mockRejectedValue(new Error("Uploaded file must have a .csv extension"));
    render(<UploadCard />);

    const file = new File(["not,csv"], "events.txt", { type: "text/plain" });
    fireEvent.change(screen.getByLabelText("CSV file"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Submit validation run" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Uploaded file must have a .csv extension");
    });
  });
});
