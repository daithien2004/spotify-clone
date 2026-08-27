---
name: vercel-react
description: Apply Vercel React best practices for performance optimization
---

# /vercel-react Command

Apply Vercel React and Next.js performance optimization guidelines.

## Usage

```
/vercel-react optimize <component-path>
```

### Arguments
- `component-path`: Path to the React component or Next.js page to optimize

## Examples

Optimize a track card component:
```
/vercel-react optimize components/TrackCard.tsx
```

Optimize a playlist page:
```
/vercel-react optimize app/playlists/page.tsx
```

Optimize a search component:
```
/vercel-react optimize components/SearchBar.tsx
```

## What It Optimizes

The command applies optimizations across eight categories:

### 1. Eliminating Waterfalls (CRITICAL)
- Move await into branches where actually used (`async-defer-await`)
- Use Promise.all() for independent operations (`async-parallel`)
- Use Suspense to stream content (`async-suspense-boundaries`)

### 2. Bundle Size Optimization (CRITICAL)
- Import directly, avoid barrel files (`bundle-barrel-imports`)
- Use next/dynamic for heavy components (`bundle-dynamic-imports`)
- Load analytics/logging after hydration (`bundle-defer-third-party`)

### 3. Server-Side Performance (HIGH)
- Authenticate server actions like API routes (`server-auth-actions`)
- Use React.cache() for per-request deduplication (`server-cache-react`)
- Restructure components to parallelize fetches (`server-parallel-fetching`)

### 4. Client-Side Data Fetching (MEDIUM-HIGH)
- Use SWR for automatic request deduplication (`client-swr-dedup`)
- Use passive listeners for scroll (`client-passive-event-listeners`)

### 5. Re-render Optimization (MEDIUM)
- Extract expensive work into memoized components (`rerender-memo`)
- Subscribe to derived booleans, not raw values (`rerender-derived-state`)
- Use functional setState for stable callbacks (`rerender-functional-setstate`)
- Pass function to useState for expensive values (`rerender-lazy-state-init`)

### 6. Rendering Performance (MEDIUM)
- Use content-visibility for long lists (`rendering-content-visibility`)
- Extract static JSX outside components (`rendering-hoist-jsx`)

### 7. JavaScript Performance (LOW-MEDIUM)
- Build Map for repeated lookups (`js-index-maps`)
- Return early from functions (`js-early-exit`)
- Use Set/Map for O(1) lookups (`js-set-map-lookups`)

### 8. Advanced Patterns (LOW)
- Store event handlers in refs (`advanced-event-handler-refs`)

## Output

The command provides:
- Specific optimization recommendations for the component
- Before/after code examples
- Performance impact estimates
- Implementation priority rankings

## Related Commands
- `/db-migration create` - For creating database schema changes
- `/kafka-event create` - For generating Kafka event flows
- `/security-review check` - For performing endpoint security reviews