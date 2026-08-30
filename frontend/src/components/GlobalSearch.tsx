import { useEffect, useId, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { searchCrm, type SearchHit } from '../api/searchApi';

interface GroupedHits {
  label: string;
  items: SearchHit[];
}

export default function GlobalSearch() {
  const navigate = useNavigate();
  const listId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [groups, setGroups] = useState<GroupedHits[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);

  const flat = groups.flatMap((group) => group.items);

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      setGroups([]);
      setError(null);
      setLoading(false);
      return;
    }

    const handle = window.setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await searchCrm(trimmed);
        const next: GroupedHits[] = [
          { label: 'Businesses', items: result.businesses },
          { label: 'Leads', items: result.leads },
          { label: 'Conversations', items: result.conversations },
        ].filter((group) => group.items.length > 0);
        setGroups(next);
        setActiveIndex(0);
        setOpen(true);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Search failed';
        setError(message);
        setGroups([]);
        setOpen(true);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => window.clearTimeout(handle);
  }, [query]);

  const goTo = (hit: SearchHit) => {
    setOpen(false);
    setQuery('');
    navigate(hit.href);
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setOpen(false);
      return;
    }
    if (event.key === 'ArrowDown' && flat.length > 0) {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((index) => (index + 1) % flat.length);
      return;
    }
    if (event.key === 'ArrowUp' && flat.length > 0) {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((index) => (index - 1 + flat.length) % flat.length);
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      if (flat[activeIndex]) {
        goTo(flat[activeIndex]);
      } else if (query.trim().length >= 2 && !loading) {
        setOpen(true);
      }
    }
  };

  return (
    <div className="relative hidden md:block">
      <label htmlFor="global-search" className="sr-only">
        Search the CRM
      </label>
      <Search
        size={16}
        className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-copy"
        aria-hidden="true"
      />
      <input
        ref={inputRef}
        id="global-search"
        type="search"
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={open && flat[activeIndex] ? `${listId}-opt-${activeIndex}` : undefined}
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setOpen(true);
        }}
        onFocus={() => query.trim().length >= 2 && setOpen(true)}
        onBlur={() => window.setTimeout(() => setOpen(false), 150)}
        onKeyDown={onKeyDown}
        placeholder="Search businesses, leads, calls…"
        className="input-dark w-72 pl-10 pr-4"
        autoComplete="off"
      />
      {open && query.trim().length >= 2 && (
        <div
          id={listId}
          role="listbox"
          aria-label="Search results"
          className="absolute z-50 mt-1 max-h-80 w-full overflow-y-auto border border-frost bg-snow shadow-classic"
        >
          {loading && <p className="px-3 py-2 text-sm text-copy">Searching…</p>}
          {!loading && error && <p className="px-3 py-2 text-sm text-error">{error}</p>}
          {!loading && !error && flat.length === 0 && (
            <p className="px-3 py-2 text-sm text-copy">No matches for “{query.trim()}”.</p>
          )}
          {!loading &&
            groups.map((group) => (
              <div key={group.label}>
                <p className="px-3 pt-2 text-[11px] font-semibold uppercase tracking-classic text-slate">
                  {group.label}
                </p>
                {group.items.map((hit) => {
                  const index = flat.indexOf(hit);
                  return (
                    <button
                      key={`${group.label}-${hit.id}`}
                      id={`${listId}-opt-${index}`}
                      type="button"
                      role="option"
                      aria-selected={index === activeIndex}
                      className={`block w-full px-3 py-2 text-left text-sm ${
                        index === activeIndex ? 'bg-pale-navy text-navy' : 'text-ink hover:bg-page'
                      }`}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => goTo(hit)}
                    >
                      <span className="font-medium">{hit.title}</span>
                      {hit.subtitle && (
                        <span className="mt-0.5 block text-xs text-copy">{hit.subtitle}</span>
                      )}
                    </button>
                  );
                })}
              </div>
            ))}
        </div>
      )}
    </div>
  );
}
