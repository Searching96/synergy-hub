Based on the project analysis, here are **Development Guidelines for AI Agents** to continue work on this PostalFlow project:

## 📋 Project Overview
**PostalFlow** — A dual-role postal/delivery management system supporting customers and delivery drivers with Vietnamese language UI.

---

## 🔧 Quick Rules

### Critical Code Organization
- **`main.tsx`** — Only file that calls `createRoot().render()`. Must NOT be duplicated elsewhere.
- **`App.tsx`** — React Router configuration. Should ONLY export the App component, never render directly.
- **`client/global.css`** — Theme colors, design tokens, global styles
- **API routes** — Prefix all server endpoints with `/api/`

### Tech Stack (Must Use)
- **Build**: Vite + pnpm (prefer pnpm over npm/yarn)
- **Frontend**: React 18 + React Router 6 SPA
- **Styling**: TailwindCSS 3 + `cn()` utility for conditional classes
- **UI**: Radix UI components from `client/components/ui/` + Lucide React icons
- **Backend**: Express (minimal; only for private logic like DB/keys)
- **Testing**: Vitest
- **Types**: TypeScript everywhere + Zod validation

### Language
- **UI text**: Vietnamese (Tiếng Việt)
- **Code comments**: English

---

## 🚀 Common Development Tasks

### Adding a New Page
1. Create `client/pages/YourPage.tsx` (function component)
2. Add route in `client/App.tsx` above the catch-all `"*"` route
3. Use shared layouts: `MobileShell`, `CustomerShell`, or `DriverShell`

### Adding an API Endpoint
1. Create `server/routes/your-route.ts` with `RequestHandler` (use for private logic only)
2. Define shared types in `shared/api.ts` if needed
3. Register in `server/index.ts` with `app.get/post('/api/your-endpoint', handler)`
4. Fetch in React with: `fetch('/api/endpoint').then(r => r.json())`

### Styling Components
```typescript
className={cn(
  "base-classes",
  isMobile && "mobile-specific",
  props.className
)}
```

---

## ⚠️ Known Issues & Fixes

### React createRoot() Error
**Symptom**: "You are calling ReactDOMClient.createRoot() on a container that has already been passed to createRoot() before."

**Cause**: Multiple files calling `createRoot()` on the same element.

**Fix**: 
- `main.tsx` = **ONLY** place that calls `createRoot().render()`
- `App.tsx` = **ONLY** exports the App component, NO render calls

---

## 📁 Project Structure Quick Reference
```
client/pages/              # Route components
  customer/                # Customer flows
  delivery-driver/         # Driver flows
  mobile/                  # Mobile-specific pages
client/components/
  ui/                      # Pre-built Radix UI components
  MobileShell.tsx          # Shared mobile layout
  CustomerShell.tsx        # Customer layout wrapper
  DriverShell.tsx          # Driver layout wrapper
server/routes/             # API handlers (minimal)
shared/api.ts              # Shared types (TypeScript interfaces)
```

---

## 🛠 Path Aliases
- `@/*` → `client/`
- `@shared/*` → `shared/`

---

## 📝 Commands to Know
```bash
pnpm dev          # Dev server with hot reload
pnpm build        # Production build
pnpm test         # Run tests with Vitest
pnpm typecheck    # Check TypeScript errors
pnpm start        # Run production binary
```

---

## ✅ Checklist Before Committing
- [ ] No `console.log()` in production code
- [ ] No hardcoded API keys/secrets
- [ ] TypeScript strict mode passes
- [ ] Uses `cn()` for conditional classes
- [ ] Routes defined in `App.tsx` (not elsewhere)
- [ ] UI text is Vietnamese
- [ ] Uses existing Radix UI components when possible
- [ ] No placeholders like `{/* TODO */}` in final code

This project is **production-ready** — maintain quality standards consistently.

# 🤖 AI Agent Behavior Guidelines for PostalFlow

## 1. **Interaction & Communication**
- **Be direct**: Answer in 1-3 sentences unless detail is requested
- **Avoid preamble**: Don't say "I will...", "Let me...", "Here's what I'll do..." before actually doing it
- **Ask first**: Clarify ambiguous requests before writing code
- **Show progress**: Use TodoWrite tool for multi-step tasks to keep user informed
- **No false claims**: Don't claim to be "working on" something unless actively calling tools

---

## 2. **Code Quality Standards**
- **Follow AGENTS.md**: It's the source of truth for this project
- **No placeholders**: No `{/* TODO */}`, `{/* Rest of code... */}`, or summarization comments
- **Mimic existing patterns**: Check neighboring files for conventions before implementing
- **Use existing utilities**: Check if libraries are already available before adding new ones
- **Type safety**: Always use TypeScript strict mode
- **No hardcoded values**: Implement proper logic, not test-case-specific hacks

---

## 3. **Architecture Decisions**
- **Respect file structure**: 
  - Routes in `client/pages/`
  - Components in `client/components/`
  - API logic in `server/routes/` (only when necessary)
  - Shared types in `shared/api.ts`
- **Minimize server code**: Keep Express routes minimal; prefer client-side logic
- **Use path aliases**: `@/*` for client, `@shared/*` for shared
- **Single source of truth**: Only `main.tsx` calls `createRoot()`, only `App.tsx` exports the root component

---

## 4. **UI/Styling Rules**
- **TailwindCSS first**: Use utility classes via `cn()` function
- **Component reuse**: Leverage Radix UI + Lucide icons from `client/components/ui/`
- **Vietnamese text**: All user-facing UI text must be Vietnamese
- **Responsive design**: Test mobile and desktop layouts
- **Consistent theming**: Pull colors from `client/global.css` and `tailwind.config.ts`

---

## 5. **When to Use Tools**
| Task | Tools | Order |
|------|-------|-------|
| Understand code | Read, Grep, Agent(explorer) | Check existing files first |
| Search patterns | Grep, Glob | For finding conventions |
| Make changes | Write, Edit, MultiEdit | Only after understanding context |
| Execute commands | Bash | For npm/build commands |
| Track progress | TodoWrite/TodoRead | For multi-step work |

---

## 6. **Git & Version Control Behavior**
- **Don't run git commands**: User handles push/sync via UI buttons
- **Commits are automatic**: Each change auto-commits; no manual commits
- **Track branch status**: Current branch is `ai_main_90bc40dd9221`, pushes to `main`
- **PR status**: Check repo status; inform user if not connected to remote

---

## 7. **Error Handling**
- **Read error rules**: Check `debugging-errors` custom rule when user reports issues
- **Don't workaround**: If user asks for specific tech (e.g., Neon DB) and it fails, ask about alternatives—don't use different tech
- **Inform blockers**: Tell user if something is impossible or requires external setup (MCPs, credentials)
- **Suggest resources**: Direct to https://www.builder.io/c/docs/projects for documentation

---

## 8. **MCP Integration Strategy**
- **Suggest proactively**: When task would benefit from Supabase, Neon, Netlify, etc.
- **Always list available MCPs**: Show all 13 integrations when relevant
- **Require setup first**: Don't write code that needs a DB without confirming MCP is connected
- **Respect preferences**: Prefer Supabase > Neon for databases; Netlify for deployment

---

## 9. **Testing & Quality Checks**
- **Run tests**: Use `pnpm test` (Vitest) before marking work complete
- **TypeCheck**: Run `pnpm typecheck` to validate TypeScript
- **Dev server**: Verify changes with dev server before finishing
- **No broken commits**: Never mark task complete if tests fail or code has errors

---

## 10. **User Experience Philosophy**
- **Minimal disruption**: Change only what's necessary
- **Explain decisions**: When multiple approaches exist, explain why one was chosen
- **Screenshot verification**: Take screenshots after visual changes to confirm
- **Memory creation**: Store project insights (deployment IDs, patterns, workarounds) for future sessions

---

## 11. **When to Ask vs. Decide**
| Scenario | Action |
|----------|--------|
| Ambiguous feature request | Ask for clarification |
| Multiple valid approaches | Ask which is preferred, then decide |
| Missing credentials/setup | Ask for setup, don't proceed without |
| Code style/naming unclear | Check existing code, follow pattern |
| Technical feasibility uncertain | Explain options, let user choose |

---

## 12. **Project-Specific Knowns**
- **Language**: Vietnamese UI, English code
- **Dual roles**: Customer (Khách hàng) & Driver (Bưu tá giao hàng) flows
- **Mobile-first**: Has dedicated mobile layout components
- **React Router SPA**: All routing via `App.tsx`, no server-side routing
- **Demo mode**: Runs in demo mode to explore both flows
- **Known issue**: Previous `createRoot()` error—ensure only `main.tsx` renders

---

## 13. **Sign-Off Checklist**
Before marking work complete:
- [ ] Code matches project conventions
- [ ] TypeScript strict mode passes
- [ ] Tests pass (`pnpm test`)
- [ ] No `console.log()` or debug code left
- [ ] No hardcoded secrets/keys
- [ ] Vietnamese UI text verified
- [ ] Related tasks in TodoWrite marked complete
- [ ] Screenshots taken if visual changes made

---

**Remember**: This is a production-ready starter—maintain quality standards consistently. Think like a senior developer, not a code monkey.