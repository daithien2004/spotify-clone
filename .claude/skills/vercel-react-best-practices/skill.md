---
name: vercel-react-best-practices
description: React and Next.js performance optimization guidelines from Vercel Engineering. Use when writing, reviewing, or refactoring React/Next.js code.
---

# Vercel React Best Practices

Performance optimization guide for React and Next.js applications.

## When to Apply

- Writing new React components or Next.js pages
- Implementing data fetching (client or server-side)
- Reviewing code for performance issues
- Refactoring existing React/Next.js code

## Quick Reference

### 1. Eliminating Waterfalls (CRITICAL)
- `async-defer-await` - Move await into branches where actually used
- `async-parallel` - Use Promise.all() for independent operations
- `async-suspense-boundaries` - Use Suspense to stream content

### 2. Bundle Size Optimization (CRITICAL)
- `bundle-barrel-imports` - Import directly, avoid barrel files
- `bundle-dynamic-imports` - Use next/dynamic for heavy components
- `bundle-defer-third-party` - Load analytics/logging after hydration

### 3. Server-Side Performance (HIGH)
- `server-auth-actions` - Authenticate server actions like API routes
- `server-cache-react` - Use React.cache() for per-request deduplication
- `server-parallel-fetching` - Restructure components to parallelize fetches

### 4. Client-Side Data Fetching (MEDIUM-HIGH)
- `client-swr-dedup` - Use SWR for automatic request deduplication
- `client-passive-event-listeners` - Use passive listeners for scroll

### 5. Re-render Optimization (MEDIUM)
- `rerender-memo` - Extract expensive work into memoized components
- `rerender-derived-state` - Subscribe to derived booleans, not raw values
- `rerender-functional-setstate` - Use functional setState for stable callbacks
- `rerender-lazy-state-init` - Pass function to useState for expensive values

### 6. Rendering Performance (MEDIUM)
- `rendering-content-visibility` - Use content-visibility for long lists
- `rendering-hoist-jsx` - Extract static JSX outside components

### 7. JavaScript Performance (LOW-MEDIUM)
- `js-index-maps` - Build Map for repeated lookups
- `js-early-exit` - Return early from functions
- `js-set-map-lookups` - Use Set/Map for O(1) lookups

### 8. Advanced Patterns (LOW)
- `advanced-event-handler-refs` - Store event handlers in refs
