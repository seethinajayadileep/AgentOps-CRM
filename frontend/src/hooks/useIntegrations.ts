import { useEffect, useState } from 'react';
import { getIntegrations } from '../api/settingsApi';
import type { IntegrationStatus } from '../types/settings';
import { ReadinessStatus } from '../types/settings';

function isReady(integration?: IntegrationStatus): boolean {
  if (!integration) return false;
  if (!integration.configured) return false;
  return (
    integration.status !== ReadinessStatus.DISABLED &&
    integration.status !== ReadinessStatus.NOT_CONFIGURED &&
    integration.status !== ReadinessStatus.ERROR &&
    integration.status !== ReadinessStatus.UNKNOWN
  );
}

export function useIntegrations() {
  const [byName, setByName] = useState<Record<string, IntegrationStatus>>({});
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    getIntegrations()
      .then((response) => {
        const next: Record<string, IntegrationStatus> = {};
        for (const item of response.integrations || []) {
          next[item.name] = item;
        }
        setByName(next);
      })
      .catch(() => {
        setByName({});
      })
      .finally(() => setLoaded(true));
  }, []);

  const lookup = (name: string) => {
    const key = Object.keys(byName).find((k) => k.toLowerCase() === name.toLowerCase());
    return key ? byName[key] : undefined;
  };

  return {
    loaded,
    ready: (name: string) => !loaded || isReady(lookup(name)),
    message: (name: string) => lookup(name)?.message,
  };
}
