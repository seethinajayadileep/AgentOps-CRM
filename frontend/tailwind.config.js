/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ink: '#182230',
        navy: '#243B53',
        page: '#F6F3EE',
        snow: '#FFFDFA',
        copy: '#526071',
        slate: '#526071',
        frost: '#DDD8CF',
        gold: '#B99245',
        success: '#347A64',
        warning: '#956B21',
        error: '#A84D55',
        mist: '#F6F3EE',
        silver: '#667085',
        pale: {
          navy: '#E9EEF4',
          gold: '#F4EDDC',
          success: '#E7F0EB',
          warning: '#F4EBE0',
          error: '#F3E6E6',
        },
        market: {
          bg: '#F6F3EE',
          surface: '#FFFDFA',
          secondary: '#F0EDE7',
          text: '#182230',
          muted: '#526071',
          accent: '#4F6FAE',
          teal: '#427F72',
          ok: '#347A64',
          danger: '#A84D55',
        },
        hero: {
          bg: '#182230',
          text: '#F8FAFC',
          muted: '#94A3B8',
        },
        primary: {
          50: '#E8EEF3',
          100: '#D5DEE7',
          200: '#B0BECB',
          300: '#8296A8',
          400: '#4E6A82',
          500: '#243B53',
          600: '#243B53',
          700: '#1C2F43',
          800: '#182230',
          900: '#101821',
        },
        accent: {
          purple: '#243B53',
          blue: '#243B53',
          cyan: '#2F6B57',
        },
      },
      fontFamily: {
        serif: ['"Playfair Display"', 'Georgia', 'Times New Roman', 'serif'],
        sans: ['"Source Sans 3"', 'system-ui', 'sans-serif'],
      },
      maxWidth: {
        content: '1200px',
      },
      borderRadius: {
        xl: '4px',
        '2xl': '6px',
      },
      boxShadow: {
        glass: '0 1px 2px rgba(24, 34, 48, 0.04)',
        glow: 'none',
        classic: '0 1px 2px rgba(24, 34, 48, 0.05), 0 1px 1px rgba(24, 34, 48, 0.03)',
        sidebar: '4px 0 16px rgba(24, 34, 48, 0.04)',
      },
      spacing: {
        13: '3.25rem',
      },
      letterSpacing: {
        classic: '0.12em',
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
        'fade-in': 'fade-in 200ms ease-out',
      },
    },
  },
  plugins: [],
}
