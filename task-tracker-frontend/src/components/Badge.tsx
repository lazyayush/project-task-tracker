interface BadgeProps {
  children: React.ReactNode;
  color: 'ink-soft' | 'amber' | 'success';
}

const colorClasses = {
  'ink-soft': 'bg-ink/5 text-ink-soft',
  amber: 'bg-amber/10 text-amber',
  success: 'bg-success/10 text-success',
};

export function Badge({ children, color }: BadgeProps) {
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${colorClasses[color]}`}>
      {children}
    </span>
  );
}