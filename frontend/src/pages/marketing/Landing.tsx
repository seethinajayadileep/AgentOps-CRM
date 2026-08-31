import { Link } from 'react-router-dom';
import {
  Building2,
  Users,
  MessageSquare,
  Phone,
  CheckCircle,
  FileText,
  LayoutDashboard,
} from 'lucide-react';

const capabilities = [
  { title: 'Lead Finder', body: 'Start industry and location searches, review discovered prospects, and import them into the CRM.' },
  { title: 'Business and contact management', body: 'Keep businesses, websites, crawl status and related contacts in one workspace.' },
  { title: 'AI-assisted conversations', body: 'Answer from crawled business knowledge and keep the conversation history with the lead.' },
  { title: 'Follow-up approvals', body: 'Drafted follow-ups wait for a human decision before anything is treated as approved.' },
  { title: 'Voice calls', body: 'Review call status, outcomes, summaries and recordings when a recording is available.' },
  { title: 'Agent execution logs', body: 'Inspect what each agent did, including sanitized error details when a run fails.' },
  { title: 'Business knowledge', body: 'Crawl a site, build a knowledge base, and ground support answers in that content.' },
  { title: 'Integration health', body: 'See whether OpenAI, Firecrawl, Apify, Vapi and related services are configured.' },
];

const steps = [
  { title: 'Add a business', body: 'Register the company and website you want the agents to work from.' },
  { title: 'Build knowledge', body: 'Crawl the site and build a knowledge base the chat agent can cite.' },
  { title: 'Work the pipeline', body: 'Qualify leads, review conversations, and queue follow-ups or calls.' },
  { title: 'Approve before action', body: 'Humans approve drafts before follow-ups, calls, or outbound searches go out.' },
];

const previewNav = [
  { label: 'Dashboard', icon: LayoutDashboard, active: true },
  { label: 'Businesses', icon: Building2, active: false },
  { label: 'Leads', icon: Users, active: false },
  { label: 'Conversations', icon: MessageSquare, active: false },
];

const previewStats = [
  { label: 'Businesses', icon: Building2 },
  { label: 'Leads', icon: Users },
  { label: 'Conversations', icon: MessageSquare },
  { label: 'Voice calls', icon: Phone },
  { label: 'Approvals', icon: CheckCircle },
  { label: 'Agent logs', icon: FileText },
];

export default function Landing() {
  return (
    <div>
      <section className="hero-band">
        <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 sm:py-16">
          <p className="text-xs font-semibold uppercase tracking-classic text-market-teal">AgentOps CRM</p>
          <h1 className="mt-3 max-w-3xl font-serif text-3xl leading-tight sm:mt-4 sm:text-5xl">
            A CRM that doesn’t just track work—it moves it forward.
          </h1>
          <p className="mt-4 max-w-2xl text-base text-hero-muted sm:mt-5 sm:text-lg">
            Discover leads, manage conversations and coordinate AI-assisted follow-ups, calls and approvals
            from one accountable workspace.
          </p>
          <div className="mt-6 flex flex-wrap items-center gap-3 sm:mt-8">
            <Link to="/signup" className="market-btn-primary">
              Get Started
            </Link>
            <Link to="/login" className="market-btn-secondary">
              Login
            </Link>
            <Link to="/login" className="inline-flex min-h-11 items-center px-2 text-sm font-medium text-hero-muted hover:text-hero-text">
              Explore the product
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="preview-heading">
        <div className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-16">
          <h2 id="preview-heading" className="font-serif text-3xl text-ink">
            The workspace
          </h2>
          <p className="mt-3 max-w-2xl text-copy">
            After you sign in, AgentOps CRM opens on this same warm operational dashboard: compact totals,
            thin borders, and a blue active-navigation indicator.
          </p>
          <div className="workspace-preview mt-8">
            <div className="flex min-h-[280px]">
              <aside className="hidden w-44 shrink-0 border-r border-[var(--border)] bg-[var(--sidebar)] sm:block">
                <div className="border-b border-[var(--border)] px-4 py-4">
                  <p className="text-[10px] font-semibold uppercase tracking-classic text-copy">AGENTOPS</p>
                  <p className="font-serif text-xl leading-none text-ink">CRM</p>
                </div>
                <nav aria-label="Preview navigation" className="px-2 py-3">
                  {previewNav.map((item) => {
                    const Icon = item.icon;
                    return (
                      <p
                        key={item.label}
                        className={`flex min-h-10 items-center gap-2 border-l-[3px] px-3 text-sm ${
                          item.active
                            ? 'border-[var(--primary)] bg-[var(--primary-soft)] font-semibold text-ink'
                            : 'border-transparent text-copy'
                        }`}
                      >
                        <Icon size={16} strokeWidth={1.75} aria-hidden="true" />
                        {item.label}
                      </p>
                    );
                  })}
                </nav>
              </aside>
              <div className="min-w-0 flex-1 p-4 sm:p-5">
                <p className="text-[10px] font-semibold uppercase tracking-classic text-copy">Overview</p>
                <p className="mt-1 font-serif text-2xl text-ink">Dashboard</p>
                <div className="mt-4 grid grid-cols-2 gap-2 lg:grid-cols-3">
                  {previewStats.map((stat) => {
                    const Icon = stat.icon;
                    return (
                      <article
                        key={stat.label}
                        className="border border-[var(--border)] bg-[var(--surface)] px-3 py-3 shadow-[var(--shadow-card)]"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <p className="text-[10px] font-semibold uppercase tracking-classic text-copy">
                            {stat.label}
                          </p>
                          <Icon size={14} strokeWidth={1.75} className="text-copy" aria-hidden="true" />
                        </div>
                        <p className="mt-2 font-serif text-xl tabular text-ink">—</p>
                      </article>
                    );
                  })}
                </div>
                <p className="mt-3 text-xs text-[var(--text-muted)]">
                  Live totals appear after you sign in. No sample metrics are shown here.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="capabilities-heading">
        <h2 id="capabilities-heading" className="font-serif text-3xl text-ink">
          Core capabilities
        </h2>
        <p className="mt-3 max-w-2xl text-copy">
          Every capability below is implemented in the current workspace—not a roadmap slide.
        </p>
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          {capabilities.map((item) => (
            <article key={item.title} className="border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-card)]">
              <h3 className="text-lg font-semibold text-ink">{item.title}</h3>
              <p className="mt-2 max-w-prose text-sm text-copy">{item.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="compare-heading">
        <h2 id="compare-heading" className="font-serif text-3xl text-ink">
          Traditional CRM versus AgentOps CRM
        </h2>
        <div className="mt-8 overflow-x-auto border border-[var(--border)] bg-[var(--surface)] px-5 shadow-[var(--shadow-card)]">
          <table className="min-w-full text-left text-sm">
            <thead>
              <tr className="text-copy">
                <th scope="col" className="py-3 pr-6 font-medium">
                  Work
                </th>
                <th scope="col" className="py-3 pr-6 font-medium">
                  Traditional CRM
                </th>
                <th scope="col" className="py-3 font-medium">
                  AgentOps CRM
                </th>
              </tr>
            </thead>
            <tbody className="text-ink">
              <tr className="border-t border-[var(--border)]">
                <td className="py-3 pr-6">Lead capture</td>
                <td className="py-3 pr-6 text-copy">Manual entry and imports</td>
                <td className="py-3">Search, review and import from Lead Finder</td>
              </tr>
              <tr className="border-t border-[var(--border)]">
                <td className="py-3 pr-6">Follow-up</td>
                <td className="py-3 pr-6 text-copy">Left to the operator’s memory</td>
                <td className="py-3">Drafted messages wait in Approvals</td>
              </tr>
              <tr className="border-t border-[var(--border)]">
                <td className="py-3 pr-6">Support answers</td>
                <td className="py-3 pr-6 text-copy">Generic inbox notes</td>
                <td className="py-3">Answers grounded in crawled business knowledge</td>
              </tr>
              <tr className="border-t border-[var(--border)]">
                <td className="py-3 pr-6">Audit</td>
                <td className="py-3 pr-6 text-copy">Scattered activity feeds</td>
                <td className="py-3">Agent execution logs for each run</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="how-heading">
        <h2 id="how-heading" className="font-serif text-3xl text-ink">
          How it works
        </h2>
        <ol className="mt-8 grid gap-4 sm:grid-cols-2">
          {steps.map((step, index) => (
            <li key={step.title} className="border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-card)]">
              <p className="text-xs font-semibold uppercase tracking-classic text-market-teal">Step {index + 1}</p>
              <h3 className="mt-2 text-lg font-semibold text-ink">{step.title}</h3>
              <p className="mt-2 max-w-prose text-sm text-copy">{step.body}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="control-heading">
        <h2 id="control-heading" className="font-serif text-3xl text-ink">
          Human approval and control
        </h2>
        <p className="mt-4 max-w-2xl text-copy">
          Agents draft and recommend. Approvals, voice calls and paid lead searches remain operator
          decisions you take from the workspace.
        </p>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="screens-heading">
        <h2 id="screens-heading" className="font-serif text-3xl text-ink">
          Product screens
        </h2>
        <div className="mt-8 grid gap-4 md:grid-cols-3">
          {[
            { title: 'Dashboard', body: 'Live totals and recent activity from the current workspace.' },
            { title: 'Conversations', body: 'Filter by status, search, and open a thread with AI answers.' },
            { title: 'Settings', body: 'Integration readiness and voice configuration without hidden keys.' },
          ].map((screen) => (
            <article key={screen.title} className="border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-card)]">
              <h3 className="text-lg font-semibold text-ink">{screen.title}</h3>
              <p className="mt-2 max-w-prose text-sm text-copy">{screen.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-14 sm:px-6 sm:pb-16" aria-labelledby="pricing-heading">
        <h2 id="pricing-heading" className="font-serif text-3xl text-ink">
          Pricing
        </h2>
        <p className="mt-4 max-w-2xl text-copy">
          There is no self-serve billing in the current product. Sign in or create an account to use
          AgentOps CRM.
        </p>
        <Link to="/pricing" className="mt-6 inline-flex min-h-11 items-center text-sm text-market-accent">
          View pricing details
        </Link>
      </section>

      <section className="border-t border-[var(--border)]">
        <div className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-16">
          <h2 className="font-serif text-3xl text-ink">Open the workspace</h2>
          <p className="mt-4 max-w-2xl text-copy">
            Create an account or use the published sample credentials on the login page.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/signup" className="market-btn-primary">
              Get Started
            </Link>
            <Link to="/login" className="market-btn-secondary">
              Login
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
