import { cn } from './utils';

export interface TabItem {
  key: string;
  label: string;
  icon?: React.ReactNode;
}

export function Tabs({
  items,
  active,
  onChange,
  className,
}: {
  items: TabItem[];
  active: string;
  onChange: (key: string) => void;
  className?: string;
}) {
  return (
    <div className={cn('flex items-center gap-1 overflow-x-auto border-b border-border scrollbar-thin', className)} role="tablist">
      {items.map((item) => (
        <button
          key={item.key}
          role="tab"
          aria-selected={active === item.key}
          onClick={() => onChange(item.key)}
          className={cn(
            'flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors',
            active === item.key
              ? 'border-primary text-foreground'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          )}
        >
          {item.icon}
          {item.label}
        </button>
      ))}
    </div>
  );
}
