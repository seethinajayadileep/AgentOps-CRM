import { Link } from 'react-router-dom';

interface BrandMarkProps {
  to?: string;
  inverted?: boolean;
  showSupport?: boolean;
}

export default function BrandMark({ to = '/', inverted = false, showSupport = false }: BrandMarkProps) {
  const label = inverted ? 'text-hero-muted' : 'text-copy';
  const title = inverted ? 'text-hero-text' : 'text-ink';
  const mark = (
    <span className="inline-flex flex-col">
      <span className={`text-[11px] font-semibold uppercase tracking-classic ${label}`}>AGENTOPS</span>
      <span className={`font-serif text-[22px] leading-none ${title}`}>CRM</span>
      {showSupport && (
        <span className={`mt-2 text-xs font-medium ${label}`}>Agentic revenue operations platform</span>
      )}
    </span>
  );

  if (!to) {
    return <span aria-label="AgentOps CRM">{mark}</span>;
  }

  return (
    <Link to={to} aria-label="AgentOps CRM" className="inline-block">
      {mark}
    </Link>
  );
}
