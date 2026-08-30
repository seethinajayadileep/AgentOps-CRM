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
        snow: '#FFFEFC',
        copy: '#344054',
        slate: '#667085',
        frost: '#DED8CF',
        gold: '#B38B3F',
        success: '#2F6B57',
        warning: '#A5672B',
        error: '#9B3A3A',
        mist: '#F6F3EE',
        silver: '#667085',
        pale: {
          navy: '#E9EEF4',
          gold: '#F4EDDC',
          success: '#E7F0EB',
          warning: '#F4EBE0',
          error: '#F3E6E6',
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
