export default function HowItWorks() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
      <h1 className="font-serif text-4xl text-ink">How it works</h1>
      <ol className="mt-8 space-y-6">
        <li>
          <h2 className="text-xl font-semibold">1. Add a business</h2>
          <p className="mt-2 text-copy">Create the company record and website the agents will use.</p>
        </li>
        <li>
          <h2 className="text-xl font-semibold">2. Build knowledge</h2>
          <p className="mt-2 text-copy">Crawl pages and build embeddings so support chat can stay on-source.</p>
        </li>
        <li>
          <h2 className="text-xl font-semibold">3. Work leads and conversations</h2>
          <p className="mt-2 text-copy">Qualify leads, review threads, and queue follow-ups or voice calls.</p>
        </li>
        <li>
          <h2 className="text-xl font-semibold">4. Approve before anything leaves the workspace</h2>
          <p className="mt-2 text-copy">
            Approvals, paid searches and live calls stay human-gated. Approve a draft or start a
            search when you are ready.
          </p>
        </li>
      </ol>
    </div>
  );
}
