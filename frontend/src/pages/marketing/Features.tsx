export default function Features() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
      <h1 className="font-serif text-4xl text-ink">Features</h1>
      <p className="mt-4 max-w-2xl text-copy">
        AgentOps CRM covers the operator loop already implemented in the product: discover, manage,
        converse, approve and audit.
      </p>
      <ul className="mt-8 space-y-4 text-ink">
        <li>Lead Finder for industry and location searches, with import into Leads.</li>
        <li>Business records, website crawl jobs and knowledge-base builds.</li>
        <li>Conversations with RAG answers when the knowledge base supports them.</li>
        <li>Follow-up drafts that stay in Approvals until a person decides.</li>
        <li>Voice call history, filters and recording playback when a file is available.</li>
        <li>Agent logs with sanitized error messages and execution detail.</li>
        <li>Settings for integration health and voice configuration.</li>
      </ul>
    </div>
  );
}
