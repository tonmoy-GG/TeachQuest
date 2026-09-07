export default function ResourceTypeIcon({ type }) {
  const commonProps = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: '1.7',
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': 'true',
  }

  switch (type) {
    case 'recording':
      return (
        <svg {...commonProps}>
          <rect x="3" y="5" width="14" height="14" rx="2.2" />
          <path d="M17 9.2L21 7v10l-4-2.2" />
          <circle cx="9.5" cy="12" r="3" />
        </svg>
      )
    case 'notes':
      return (
        <svg {...commonProps}>
          <path d="M7 4.5h7l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
          <path d="M14 4.5V9h4" />
          <path d="M8 12h7M8 15h7" />
        </svg>
      )
    case 'quiz':
      return (
        <svg {...commonProps}>
          <path d="M7 4.5h7l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
          <path d="M14 4.5V9h4" />
          <path d="M8.5 13l1.4 1.4 3.1-3.4" />
        </svg>
      )
    case 'exam':
      return (
        <svg {...commonProps}>
          <rect x="4" y="4.5" width="16" height="15" rx="2.2" />
          <path d="M8 8h8M8 12h8M8 16h5" />
          <path d="M17.5 17.5 19 19l2-2.8" />
        </svg>
      )
    case 'final':
      return (
        <svg {...commonProps}>
          <path d="M7 4.5h7l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
          <path d="M14 4.5V9h4" />
          <path d="M8 14h8M8 17h5" />
          <circle cx="16.5" cy="15.5" r="1.8" />
        </svg>
      )
    default:
      return (
        <svg {...commonProps}>
          <path d="M7 4.5h7l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
          <path d="M14 4.5V9h4" />
        </svg>
      )
  }
}
