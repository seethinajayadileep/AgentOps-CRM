import { describe, expect, it } from 'vitest';
import { formatLeadLocation } from './formatLeadLocation';

const raw = JSON.stringify({
  address: 'Shifa Plaza, near JNTU, Kukatpally, Hyderabad, Telangana 500072, India',
  city: 'Hyderabad',
  state: 'Telangana',
  location: { lat: 17.4938316, lng: 78.3955591 },
});

describe('formatLeadLocation', () => {
  it('prefers the raw street address over a lat/lng dump', () => {
    expect(formatLeadLocation('{lat=17.4938316, lng=78.3955591}', raw)).toBe(
      'Shifa Plaza, near JNTU, Kukatpally, Hyderabad, Telangana 500072, India'
    );
  });

  it('composes city and state when there is no address field', () => {
    expect(
      formatLeadLocation('{lat=17.4, lng=78.3}', JSON.stringify({ city: 'Hyderabad', state: 'Telangana' }))
    ).toBe('Hyderabad, Telangana');
  });

  it('formats a stored coordinate dump when raw data is missing', () => {
    expect(formatLeadLocation('{lat=17.4938316, lng=78.3955591}')).toBe('17.4938° N, 78.3956° E');
  });

  it('keeps an already readable location', () => {
    expect(formatLeadLocation('Hyderabad')).toBe('Hyderabad');
  });

  it('returns a dash when nothing usable is present', () => {
    expect(formatLeadLocation(undefined)).toBe('-');
    expect(formatLeadLocation('   ')).toBe('-');
  });
});
