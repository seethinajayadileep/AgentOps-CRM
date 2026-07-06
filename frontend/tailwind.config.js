/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Dark-mode-first surface palette
        base: {
          DEFAULT: '#09090B',
          alt: '#111827',
        },
        card: {
          DEFAULT: '#18181B',
          hover: '#1F1F23',
        },
        // Repointed "primary" to a purple/violet scale so existing
        // `bg-primary-600` usages stay on-brand without renaming APIs.
        primary: {
          50: '#f5f3ff',
          100: '#ede9fe',
          200: '#ddd6fe',
          300: '#c4b5fd',
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed',
          700: '#6d28d9',
          800: '#5b21b6',
          900: '#4c1d95',
        },
        accent: {
          purple: '#8B5CF6',
          blue: '#3B82F6',
          cyan: '#06B6D4',
        },
        success: '#22C55E',
        warning: '#F59E0B',
      },
      borderRadius: {
        xl: '12px',
        '2xl': '16px',
      },
      boxShadow: {
        glass: '0 8px 32px rgba(0, 0, 0, 0.37)',
        glow: '0 0 24px rgba(139, 92, 246, 0.25)',
      },
      backgroundImage: {
        'accent-gradient': 'linear-gradient(135deg, #8B5CF6 0%, #3B82F6 100%)',
        'accent-gradient-hover': 'linear-gradient(135deg, #7C3AED 0%, #2563EB 100%)',
      },
      transitionDuration: {
        250: '250ms',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 250ms ease-out',
      },
    },
  },
  plugins: [],
}
