import { RunDetailView } from "@/components/RunDetailView";

interface RunDetailPageProps {
  params: Promise<{
    runId: string;
  }>;
}

export default async function RunDetailPage({ params }: RunDetailPageProps) {
  const { runId } = await params;
  return <RunDetailView runId={runId} />;
}
