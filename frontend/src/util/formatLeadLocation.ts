const COORD_DUMP =
  /^\{\s*lat\s*[=:]\s*(-?\d+(?:\.\d+)?)\s*,\s*lng\s*[=:]\s*(-?\d+(?:\.\d+)?)\s*\}$/i;

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function asText(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const text = value.trim();
  if (!text || text === 'null' || COORD_DUMP.test(text)) return null;
  return text;
}

function formatCoords(lat: unknown, lng: unknown): string | null {
  const latitude = typeof lat === 'number' ? lat : Number(lat);
  const longitude = typeof lng === 'number' ? lng : Number(lng);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null;
  const ns = latitude >= 0 ? 'N' : 'S';
  const ew = longitude >= 0 ? 'E' : 'W';
  return `${Math.abs(latitude).toFixed(4)}° ${ns}, ${Math.abs(longitude).toFixed(4)}° ${ew}`;
}

function composePlace(record: Record<string, unknown>): string | null {
  const parts = ['street', 'neighborhood', 'city', 'state', 'postalCode', 'country', 'countryCode']
    .map((key) => asText(record[key]))
    .filter((part): part is string => Boolean(part));
  const unique = parts.filter((part, index) => parts.indexOf(part) === index);
  return unique.length > 0 ? unique.join(', ') : null;
}

function readableFromRecord(record: Record<string, unknown> | null): string | null {
  if (!record) return null;
  const address = asText(record.address)
    || asText(record.fullAddress)
    || asText(record.formattedAddress)
    || asText(record.streetAddress);
  if (address) return address;
  const composed = composePlace(record);
  if (composed) return composed;
  const nested = asRecord(record.location);
  if (nested) {
    const nestedReadable = readableFromRecord(nested);
    if (nestedReadable) return nestedReadable;
    return formatCoords(nested.lat, nested.lng ?? nested.lon ?? nested.longitude);
  }
  return formatCoords(record.lat, record.lng ?? record.lon ?? record.longitude);
}

function readableFromStored(location?: string | null): string | null {
  if (!location) return null;
  const trimmed = location.trim();
  if (!trimmed) return null;
  const dump = trimmed.match(COORD_DUMP);
  if (dump) return formatCoords(dump[1], dump[2]);
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    try {
      return readableFromRecord(asRecord(JSON.parse(trimmed)));
    } catch {
      return null;
    }
  }
  return trimmed;
}

/**
 * Turns Apify location dumps (`{lat=…, lng=…}`) into an address or readable coordinates.
 */
export function formatLeadLocation(
  location?: string | null,
  rawDataJson?: string | null
): string {
  if (rawDataJson) {
    try {
      const fromRaw = readableFromRecord(asRecord(JSON.parse(rawDataJson)));
      if (fromRaw) return fromRaw;
    } catch {
      // Fall through to the stored location.
    }
  }
  return readableFromStored(location) || '-';
}
