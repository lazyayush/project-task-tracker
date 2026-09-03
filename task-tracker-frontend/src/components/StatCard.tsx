interface StatCardProps {
  label: string;
  value: number;
  accent?: 'primary' | 'amber' | 'success';
}

const accentClasses = {
  primary: 'text-primary',
  amber: 'text-amber',
  success: 'text-success',
};

export function StatCard({ label, value, accent = 'primary' }: StatCardProps) {
  return (
    <div className="bg-white border border-line rounded-lg p-5">
      <p className="text-sm text-ink-soft font-medium">{label}</p>
      <p className={`font-display text-3xl font-semibold mt-1 ${accentClasses[accent]}`}>{value}</p>
    </div>
  );
}