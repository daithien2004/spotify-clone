export default function Loading() {
  return (
    <div className="flex h-screen flex-col bg-background p-2 gap-2 animate-pulse">
      {/* Top Nav Placeholder */}
      <div className="h-16 w-full bg-muted rounded-xl" />
      
      <div className="flex-1 flex gap-2 overflow-hidden">
        {/* Sidebar Placeholder */}
        <div className="hidden lg:block w-[280px] h-full bg-muted rounded-xl" />
        
        {/* Main Feed Placeholder */}
        <div className="flex-1 h-full bg-muted rounded-xl p-6 space-y-6">
          <div className="h-8 w-48 bg-muted-foreground/20 rounded-md" />
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
            {[...Array(10)].map((_, i) => (
              <div key={i} className="aspect-square bg-muted-foreground/10 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
      
      {/* Player Placeholder */}
      <div className="h-24 w-full bg-muted rounded-xl" />
    </div>
  );
}
