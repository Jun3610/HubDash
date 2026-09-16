---
name: hubdash-workflow
description: Mandatory git and devlog workflow for the HubDash repository. Use this skill whenever doing ANY development work in HubDash (server/ or elsewhere in this repo) — implementing a feature, fixing a bug, writing a plan, or making any code or doc change, even a small one. It governs two things every session in this repo must do without being asked: (1) commit to git with the project's devlog-style commit body AND write a same-day entry in server/devlog/YYYY-MM-DD.md — neither is optional, both happen for every unit of work; (2) all GitHub work (issues, branches, PRs, merges) follows the issue -> branch -> PR -> merge flow autonomously, without pausing for confirmation at each step. Check this skill before starting work in HubDash and again before considering any task "done."
---

# HubDash Development Workflow

HubDash is a personal portfolio project. The user has explicitly and repeatedly confirmed
(2026-09-12 through 2026-09-16) that they want two things to happen automatically, every time,
without being asked — not as a nice-to-have, but as the baseline expectation for working in this
repo. Treat both as load-bearing: skipping either one is a task left unfinished, not a shortcut.

## Rule 1: Every unit of work gets a commit AND a devlog entry

**Why:** the user moved their personal dev journal out of Notion and into this repo specifically
so the history lives next to the code. A commit shows *what* changed; the devlog shows *why*,
*what went wrong*, and *what was learned* — information that doesn't fit in a diff and disappears
if it isn't written down at the time. Skipping the devlog because "the commit message covers it"
defeats the reason it was moved into the repo.

**Do both, every time you finish a task, fix, or meaningful step:**

1. **Commit** with this exact body structure (the header date is the date the work actually
   happened, in `YYYY-MM-DD` form):

   ```
   <one-line subject, imperative, e.g. "feat: HabitService 추가">

   ## YYYY-MM-DD
   ### 한 일
   - (what you did)

   ### 막힌 것
   - (a problem you hit)
   → 원인: (root cause)
   → 해결: (how you fixed it)

   ### 배운 것
   - (something learned, if anything)
   ```

   Sections with nothing to report may be omitted, but `### 한 일` is always present. **Never use
   square brackets (`[`/`]`) anywhere** — in commit subjects, bodies, issue titles, or PR
   titles/bodies. The user has said this is the thing they dislike most about conventional commit
   styles like `[Feature]`; use `feat:`/`fix:`/`docs:`/`test:` prefixes instead.

2. **Devlog entry** in `server/devlog/YYYY-MM-DD.md`, same structure (한 일 / 막힌 것 / 배운 것).
   - If today's file doesn't exist yet, copy `server/devlog/TEMPLATE.md`.
   - If it already exists (e.g. you're resuming after a break, or this is a later session the
     same day), append a new `---`-separated section rather than overwriting — see existing
     files like `server/devlog/2026-09-16.md` for the pattern of multiple same-day sections.
   - If a session spans midnight or resumes after a gap (rate limits, a restart), split entries
     by the *actual* calendar date the work happened, not the date the session started.
   - Commit the devlog file itself (a small `docs:` commit) using the same body convention.

Do this per logical unit of work — per implementation task, per bug fix, per review-fix round —
not just once at the very end of a long session. If you're running a multi-task plan (e.g. via
subagent-driven-development), each task's own commit already satisfies Rule 1 for that task; the
devlog entry can be written once per work session covering everything done since the last entry,
rather than once per task, since the devlog is a narrative summary, not a per-commit mirror.

## Rule 2: GitHub work follows issue -> branch -> PR -> merge, autonomously

**Why:** the user said explicitly — "중간중간 깃허브 이슈 머지 그런거는 알아서 해야한다" (issue/PR/merge
work should be handled without asking each time). Stopping to ask "should I create the PR now?"
or "should I merge this?" on every domain/feature is friction the user has already resolved in
advance. The exception (see below) is the only case where you pause.

**The flow, in order:**

1. **Issue.** Default to creating an issue, and err toward more of them rather than fewer — the
   user has said explicitly (2026-09-16) that issues should be granular and frequent, not just a
   single umbrella checkbox list. Concretely:
   - A big umbrella issue can still track a whole initiative (e.g. issue #4 "Phase 2: 멀티모듈 +
     도메인 기반 아키텍처 전환" tracks the domain-by-domain rollout), but each meaningfully-sized
     piece of work under it — each domain, each distinct bug, each deferred follow-up from a
     review — gets its **own issue**, linked back with `관련 이슈: #N` in its body and referenced
     from the umbrella issue's checklist line (e.g. `- [ ] life 도메인 CRUD — 이슈 #14`).
   - **Don't let a final-review "follow-up work" list stay only in a PR description or devlog
     entry.** If a review surfaces something real that isn't being fixed now (a cross-cutting gap,
     a design decision to make later, a test gap), open an issue for it immediately, in the same
     session — not "note it and open an issue later," which in practice doesn't happen. Issues
     #15-#18 (GlobalExceptionHandler 400-vs-500, missing Flyway-exercising test, EntityNotFound
     message factory, pagination convention) were follow-ups mentioned in PR #12/#13 but only
     actually filed as issues after the user pointed out issues weren't being created often enough
     — don't repeat that gap.
   - Update the relevant checkbox(es) as sub-items complete.
2. **Branch.** Name it `feature/<short-description>-<issue-number>` (e.g. `feature/domain-hub-4`,
   `feature/domain-study-4`) — the issue number in the branch name lets anyone trace which issue
   a branch belongs to at a glance, without opening GitHub. This was an explicit user request
   (2026-09-13) after earlier branches didn't include it.
3. **PR.** Push and `gh pr create` with a body summarizing what was built, how it was tested, and
   any follow-up items — see recent PRs (#12, #13) for the level of detail expected: a summary,
   a test section (including real-environment verification if the change touches
   infrastructure), and a follow-up-work section for anything deferred rather than fixed.
4. **Merge.** `gh pr merge --merge --delete-branch` once the PR's own checks (tests, review) are
   green. Don't leave finished PRs open waiting for a go-ahead — merging is part of finishing the
   task, not a separate decision.

Do all four steps without pausing to confirm with the user, for this repo specifically. This
overrides the general instinct to ask before pushing/merging — the user has already given that
authorization in advance, repo-wide.

**The one exception:** genuinely destructive or hard-to-reverse actions outside this normal flow
— force-pushing, deleting a branch that has unmerged work, `git reset --hard`, discarding
uncommitted changes — still warrant a pause and confirmation, exactly as general safety practice
requires. The autonomy this skill grants is for the *normal* issue → branch → PR → merge cycle,
not for destructive operations that fall outside it.

## Quick self-check before calling something "done"

- [ ] Is there a commit (or several) with the devlog-style body for what I just did?
- [ ] Does `server/devlog/YYYY-MM-DD.md` reflect this work, for the date it actually happened?
- [ ] If this closes out a piece of work tracked by an issue: is the checkbox updated, is there a
      PR, and — once it's green — has it been merged?

If any box is unchecked, the task isn't finished yet, even if the code itself is correct.
