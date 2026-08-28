import type {
  Actor,
  AgentRun,
  CostSummary,
  Domain,
  Memory,
  Permissions,
  Repository,
  Skill,
  WorkItem,
} from '../api/types';

/**
 * Fixtures for the mocked API. Four domains with genuinely different shapes —
 * a busy production tenant, a quieter staging tenant with a narrower permission
 * grant, a sandbox with almost nothing in it, and a tenant this actor can only
 * observe — so empty states, locked navigation, refused actions and the domain
 * switcher all have something real to render.
 */

export const actor: Actor = {
  id: 'act_01',
  name: 'Max Holman',
  handle: 'maxholman',
  email: 'maxholman3@gmail.com',
  role: 'owner',
  initials: 'MH',
};

export const domains: Domain[] = [
  {
    id: 'modus',
    name: 'Modus Core',
    environment: 'production',
    description: 'The harness building itself. Beans, adapters, and the agent runtime.',
    actorCount: 6,
    monthToDateSpendUsd: 428.6,
    monthlyBudgetUsd: 750,
  },
  {
    id: 'atlas-ledger',
    name: 'Atlas Ledger',
    environment: 'staging',
    description: 'Double-entry accounting service. Migration work item tracked here.',
    actorCount: 3,
    monthToDateSpendUsd: 96.42,
    monthlyBudgetUsd: 300,
  },
  {
    id: 'sandbox',
    name: 'Sandbox',
    environment: 'sandbox',
    description: 'Throwaway tenant for trying triggers and skills before they graduate.',
    actorCount: 1,
    monthToDateSpendUsd: 3.18,
    monthlyBudgetUsd: 25,
  },
  {
    id: 'beacon',
    name: 'Beacon Analytics',
    environment: 'production',
    description: 'Another team’s tenant. This actor can see everything and change nothing.',
    actorCount: 9,
    monthToDateSpendUsd: 512.4,
    monthlyBudgetUsd: 900,
  },
];

export const permissions: Permissions[] = [
  {
    domainId: 'modus',
    capabilities: [
      'work.read',
      'work.write',
      'repositories.read',
      'agents.read',
      'agents.run',
      'memories.read',
      'cost.read',
      'skills.read',
      'settings.read',
      'settings.write',
    ],
  },
  {
    // No cost visibility and read-only settings — the shell must reflect this.
    domainId: 'atlas-ledger',
    capabilities: [
      'work.read',
      'repositories.read',
      'agents.read',
      'agents.run',
      'memories.read',
      'skills.read',
      'settings.read',
    ],
  },
  {
    domainId: 'sandbox',
    capabilities: ['work.read', 'agents.read', 'agents.run', 'cost.read', 'skills.read'],
  },
  {
    // An observer: reads every surface, holds no authority to spend money.
    // agents.read without agents.run is the whole point of this grant — it is
    // what makes the console's refusal path reachable.
    domainId: 'beacon',
    capabilities: [
      'work.read',
      'repositories.read',
      'agents.read',
      'memories.read',
      'cost.read',
      'skills.read',
      'settings.read',
    ],
  },
];

const bean = (lines: string[]) => lines.join('\n');

const modusWork: WorkItem[] = [
  {
    id: 'wi_0001',
    key: '0001',
    title: 'Foundation documentation package',
    kind: 'epic',
    status: 'done',
    assignee: 'maxholman',
    updatedAt: '2026-08-26T09:12:00Z',
    labels: ['documentation', 'foundation'],
    spendUsd: 41.28,
    parentKey: null,
    body: bean([
      '## Scope',
      'Architecture overview, domain model, and the bean schema that every later',
      'work item is validated against.',
      '',
      '## Success criteria',
      '- [x] Domain model documented',
      '- [x] Bean frontmatter schema drafted',
      '- [x] Reviewed by a second actor',
    ]),
  },
  {
    id: 'wi_0002',
    key: '0002',
    title: 'Backoffice foundation',
    kind: 'epic',
    status: 'in-review',
    assignee: 'maxholman',
    updatedAt: '2026-08-28T14:40:00Z',
    labels: ['backoffice', 'design-system', 'foundation'],
    spendUsd: 63.9,
    parentKey: null,
    body: bean([
      '## Scope',
      'Vite + React + TypeScript backoffice: design tokens, primitives, app shell,',
      'domain switcher, work list, agent console, cost surface, mocked API seam.',
      '',
      '## Success criteria',
      '- [x] Tokenised light and dark themes, contrast verified',
      '- [x] Streaming agent console against a transport interface',
      '- [ ] Real SSE transport (deferred to a later work item)',
    ]),
  },
  {
    id: 'wi_0003',
    key: '0003',
    title: 'Stream Claude Code output over SSE',
    kind: 'story',
    status: 'in-progress',
    assignee: 'maxholman',
    updatedAt: '2026-08-28T11:02:00Z',
    labels: ['runtime', 'streaming'],
    spendUsd: 18.44,
    parentKey: '0002',
    body: bean([
      '## Scope',
      'Replace the mock transport with a real `EventSource` implementation that',
      'reconnects with backoff and replays missed events by sequence number.',
      '',
      '## Notes',
      'The backoffice already consumes `StreamTransport`, so this is a swap of one',
      'implementation, not a rewrite of the console.',
    ]),
  },
  {
    id: 'wi_0004',
    key: '0004',
    title: 'Per-domain spend caps with hard stop',
    kind: 'story',
    status: 'ready',
    assignee: null,
    updatedAt: '2026-08-27T16:20:00Z',
    labels: ['cost', 'policy'],
    spendUsd: 0,
    parentKey: null,
    body: bean([
      '## Scope',
      'A domain exceeding its monthly budget refuses to start new agent runs and',
      'surfaces the refusal as an action in the backoffice.',
    ]),
  },
  {
    id: 'wi_0005',
    key: '0005',
    title: 'Trigger: open PR review on push to main',
    kind: 'story',
    status: 'blocked',
    assignee: 'maxholman',
    updatedAt: '2026-08-25T08:55:00Z',
    labels: ['triggers', 'github'],
    spendUsd: 7.1,
    parentKey: null,
    body: bean([
      '## Blocked on',
      'GitHub App installation permissions for the Modus org. Waiting on org admin.',
    ]),
  },
  {
    id: 'wi_0006',
    key: '0006',
    title: 'Bean frontmatter schema ratification',
    kind: 'task',
    status: 'in-review',
    assignee: 'maxholman',
    updatedAt: '2026-08-28T07:30:00Z',
    labels: ['schema', 'foundation'],
    spendUsd: 4.02,
    parentKey: '0001',
    body: bean([
      '## Scope',
      'Freeze the required frontmatter keys so tooling can validate bean files in CI.',
    ]),
  },
  {
    id: 'wi_0007',
    key: '0007',
    title: 'Memory compaction for long-running domains',
    kind: 'story',
    status: 'backlog',
    assignee: null,
    updatedAt: '2026-08-21T13:45:00Z',
    labels: ['memories', 'cost'],
    spendUsd: 0,
    parentKey: null,
    body: bean([
      '## Scope',
      'Summarise stale memories into a compacted digest once a domain crosses a',
      'token threshold, so context cost stops growing with tenant age.',
    ]),
  },
];

const atlasWork: WorkItem[] = [
  {
    id: 'wi_a01',
    key: '0011',
    title: 'Migrate ledger postings to append-only store',
    kind: 'epic',
    status: 'in-progress',
    assignee: 'maxholman',
    updatedAt: '2026-08-28T10:15:00Z',
    labels: ['migration', 'ledger'],
    spendUsd: 52.8,
    parentKey: null,
    body: bean([
      '## Scope',
      'Move postings off the mutable table onto an append-only journal with a',
      'materialised balance projection.',
    ]),
  },
  {
    id: 'wi_a02',
    key: '0012',
    title: 'Backfill projection from 2019 archive',
    kind: 'story',
    status: 'ready',
    assignee: null,
    updatedAt: '2026-08-27T09:00:00Z',
    labels: ['migration'],
    spendUsd: 0,
    parentKey: '0011',
    body: bean(['## Scope', 'Replay the archive through the new projection and reconcile totals.']),
  },
  {
    id: 'wi_a03',
    key: '0013',
    title: 'Reconciliation report for finance',
    kind: 'task',
    status: 'backlog',
    assignee: null,
    updatedAt: '2026-08-24T15:30:00Z',
    labels: ['reporting'],
    spendUsd: 0,
    parentKey: '0011',
    body: bean(['## Scope', 'Nightly reconciliation summary, delivered as a signed CSV.']),
  },
];

const beaconWork: WorkItem[] = [
  {
    id: 'wi_0042',
    key: '0042',
    title: 'Nightly rollup drifts from the event log',
    kind: 'story',
    status: 'in-progress',
    assignee: 'priya',
    updatedAt: '2026-08-28T08:30:00Z',
    labels: ['pipeline', 'correctness'],
    spendUsd: 118.4,
    parentKey: null,
    body: bean([
      '## Problem',
      'The nightly rollup and a replay of the event log disagree by roughly',
      '0.3% on any month that contains a late-arriving correction.',
      '',
      '## Success criteria',
      '- [ ] Replay and rollup agree to the cent on the last twelve months',
      '- [ ] A disagreement fails the nightly job instead of publishing',
    ]),
  },
  {
    id: 'wi_0043',
    key: '0043',
    title: 'Schema drift alert is too noisy to act on',
    kind: 'task',
    status: 'ready',
    assignee: null,
    updatedAt: '2026-08-26T16:10:00Z',
    labels: ['alerting'],
    spendUsd: 12.8,
    parentKey: '0042',
    body: bean([
      '## Problem',
      'Every additive column change pages someone. Only incompatible changes',
      'should.',
    ]),
  },
];

export const workByDomain: Record<string, WorkItem[]> = {
  modus: modusWork,
  'atlas-ledger': atlasWork,
  sandbox: [],
  beacon: beaconWork,
};

export const repositoriesByDomain: Record<string, Repository[]> = {
  modus: [
    {
      id: 'repo_01',
      name: 'Modus',
      remote: 'git@github.com:m4xy/Modus.git',
      defaultBranch: 'main',
      lastSyncedAt: '2026-08-28T14:38:00Z',
      openWorkItems: 5,
      status: 'connected',
    },
    {
      id: 'repo_02',
      name: 'modus-skills',
      remote: 'git@github.com:m4xy/modus-skills.git',
      defaultBranch: 'main',
      lastSyncedAt: '2026-08-28T06:02:00Z',
      openWorkItems: 1,
      status: 'syncing',
    },
  ],
  'atlas-ledger': [
    {
      id: 'repo_11',
      name: 'atlas-ledger',
      remote: 'git@github.com:m4xy/atlas-ledger.git',
      defaultBranch: 'trunk',
      lastSyncedAt: '2026-08-28T12:10:00Z',
      openWorkItems: 3,
      status: 'error',
    },
  ],
  sandbox: [],
  beacon: [
    {
      id: 'repo_21',
      name: 'beacon-pipelines',
      remote: 'git@github.com:m4xy/beacon-pipelines.git',
      defaultBranch: 'main',
      lastSyncedAt: '2026-08-28T11:44:00Z',
      openWorkItems: 4,
      status: 'connected',
    },
  ],
};

export const agentRunsByDomain: Record<string, AgentRun[]> = {
  modus: [
    {
      id: 'run_301',
      workItemKey: '0002',
      trigger: 'manual · console',
      model: 'claude-opus-5',
      status: 'running',
      startedAt: '2026-08-28T14:31:00Z',
      durationMs: 412_000,
      costUsd: 3.84,
      tokensIn: 148_200,
      tokensOut: 22_940,
    },
    {
      id: 'run_300',
      workItemKey: '0003',
      trigger: 'push · main',
      model: 'claude-sonnet-5',
      status: 'succeeded',
      startedAt: '2026-08-28T11:02:00Z',
      durationMs: 236_000,
      costUsd: 1.12,
      tokensIn: 96_400,
      tokensOut: 14_120,
    },
    {
      id: 'run_299',
      workItemKey: '0005',
      trigger: 'schedule · 08:00',
      model: 'claude-haiku-4-5',
      status: 'failed',
      startedAt: '2026-08-28T08:00:00Z',
      durationMs: 41_000,
      costUsd: 0.09,
      tokensIn: 18_300,
      tokensOut: 2_040,
    },
    {
      id: 'run_298',
      workItemKey: '0001',
      trigger: 'manual · console',
      model: 'claude-opus-5',
      status: 'succeeded',
      startedAt: '2026-08-27T17:44:00Z',
      durationMs: 512_000,
      costUsd: 5.61,
      tokensIn: 212_800,
      tokensOut: 31_500,
    },
  ],
  'atlas-ledger': [
    {
      id: 'run_120',
      workItemKey: '0011',
      trigger: 'manual · console',
      model: 'claude-sonnet-5',
      status: 'succeeded',
      startedAt: '2026-08-28T10:15:00Z',
      durationMs: 302_000,
      costUsd: 1.94,
      tokensIn: 118_000,
      tokensOut: 16_800,
    },
  ],
  sandbox: [],
  beacon: [
    {
      id: 'run_501',
      workItemKey: '0042',
      trigger: 'trigger · nightly rollup',
      model: 'claude-opus-5',
      status: 'succeeded',
      startedAt: '2026-08-28T02:00:00Z',
      durationMs: 884_000,
      costUsd: 9.12,
      tokensIn: 402_600,
      tokensOut: 58_400,
    },
    {
      id: 'run_502',
      workItemKey: '0043',
      trigger: 'trigger · schema drift',
      model: 'claude-haiku-4-5',
      status: 'failed',
      startedAt: '2026-08-27T21:12:00Z',
      durationMs: 61_000,
      costUsd: 0.14,
      tokensIn: 88_400,
      tokensOut: 6_200,
    },
  ],
};

export const memoriesByDomain: Record<string, Memory[]> = {
  modus: [
    {
      id: 'mem_01',
      title: 'Style is enforced by tools, never by review comments',
      scope: 'domain',
      updatedAt: '2026-08-26T10:00:00Z',
      tokens: 180,
      excerpt:
        'Formatting disagreements go to the formatter config, not to a PR thread. Reviewers spend their attention on behaviour.',
    },
    {
      id: 'mem_02',
      title: 'Beans are the unit of work',
      scope: 'domain',
      updatedAt: '2026-08-24T18:20:00Z',
      tokens: 240,
      excerpt:
        'Every change traces to a numbered markdown bean. No bean, no branch — the harness will not open a session without one.',
    },
    {
      id: 'mem_03',
      title: 'Gradle module layout',
      scope: 'repository',
      updatedAt: '2026-08-22T11:05:00Z',
      tokens: 620,
      excerpt:
        'core/ holds pure domain logic, adapters/ holds ports, app/ wires them. Nothing in core may import an adapter.',
    },
  ],
  'atlas-ledger': [
    {
      id: 'mem_11',
      title: 'Postings are immutable once journalled',
      scope: 'domain',
      updatedAt: '2026-08-27T09:40:00Z',
      tokens: 310,
      excerpt: 'Corrections are new compensating entries. Never update a posting in place.',
    },
  ],
  sandbox: [],
  beacon: [
    {
      id: 'mem_21',
      title: 'Rollups are recomputed, never patched',
      scope: 'domain',
      updatedAt: '2026-08-25T14:05:00Z',
      tokens: 260,
      excerpt:
        'A wrong rollup is deleted and rebuilt from the event log. Editing an aggregate in place hides the bug that produced it.',
    },
  ],
};

export const skillsByDomain: Record<string, Skill[]> = {
  modus: [
    {
      id: 'skill_01',
      name: 'bean-writer',
      summary: 'Drafts and updates work item markdown from a conversation.',
      installedVersion: '1.4.0',
      invocations30d: 62,
      enabled: true,
    },
    {
      id: 'skill_02',
      name: 'repo-surveyor',
      summary: 'Builds a structural map of a repository before a run starts.',
      installedVersion: '0.9.2',
      invocations30d: 41,
      enabled: true,
    },
    {
      id: 'skill_03',
      name: 'cost-auditor',
      summary: 'Flags runs whose spend exceeds the stage budget.',
      installedVersion: '0.3.0',
      invocations30d: 8,
      enabled: false,
    },
  ],
  'atlas-ledger': [
    {
      id: 'skill_11',
      name: 'schema-differ',
      summary: 'Diffs migration DDL against the live schema and reports drift.',
      installedVersion: '2.1.1',
      invocations30d: 19,
      enabled: true,
    },
  ],
  sandbox: [],
  beacon: [
    {
      id: 'skill_21',
      name: 'metric-explainer',
      summary: 'Traces a dashboard number back to the query and the rows behind it.',
      installedVersion: '1.0.4',
      invocations30d: 33,
      enabled: true,
    },
  ],
};

/** Deterministic pseudo-random so the charts look alive but never flicker between runs. */
function seeded(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1_664_525 + 1_013_904_223) % 4_294_967_296;
    return state / 4_294_967_296;
  };
}

function dailySeries(days: number, base: number, seed: number) {
  const random = seeded(seed);
  const start = new Date('2026-08-01T00:00:00Z');
  return Array.from({ length: days }, (_, index) => {
    const date = new Date(start.getTime() + index * 86_400_000);
    const weekend = date.getUTCDay() === 0 || date.getUTCDay() === 6;
    const noise = 0.55 + random() * 0.9;
    const usd = Math.round(base * noise * (weekend ? 0.32 : 1) * 100) / 100;
    return { date: date.toISOString().slice(0, 10), usd };
  });
}

export const costByDomain: Record<string, CostSummary> = {
  modus: {
    monthToDateUsd: 428.6,
    previousMonthToDateUsd: 351.05,
    monthlyBudgetUsd: 750,
    forecastUsd: 612.4,
    runs: 214,
    daily: dailySeries(28, 18.4, 7),
    byStage: [
      { stage: 'plan', label: 'Plan', usd: 61.2, tokensIn: 2_140_000, tokensOut: 214_000 },
      {
        stage: 'implement',
        label: 'Implement',
        usd: 216.4,
        tokensIn: 7_820_000,
        tokensOut: 1_180_000,
      },
      { stage: 'review', label: 'Review', usd: 88.9, tokensIn: 3_410_000, tokensOut: 402_000 },
      { stage: 'verify', label: 'Verify', usd: 43.7, tokensIn: 1_620_000, tokensOut: 168_000 },
      { stage: 'summarise', label: 'Summarise', usd: 18.4, tokensIn: 740_000, tokensOut: 96_000 },
    ],
    byModel: [
      {
        model: 'claude-opus-5',
        label: 'Opus 5',
        usd: 268.3,
        tokensIn: 6_140_000,
        tokensOut: 1_020_000,
      },
      {
        model: 'claude-sonnet-5',
        label: 'Sonnet 5',
        usd: 132.8,
        tokensIn: 7_920_000,
        tokensOut: 880_000,
      },
      {
        model: 'claude-haiku-4-5',
        label: 'Haiku 4.5',
        usd: 27.5,
        tokensIn: 1_670_000,
        tokensOut: 160_000,
      },
    ],
    byWorkItem: [
      { key: '0002', title: 'Backoffice foundation', usd: 63.9, runs: 24 },
      { key: '0011', title: 'Agent runtime hardening', usd: 58.2, runs: 31 },
      { key: '0003', title: 'Stream Claude Code output over SSE', usd: 18.44, runs: 12 },
      { key: '0001', title: 'Foundation documentation package', usd: 41.28, runs: 18 },
      { key: '0005', title: 'Trigger: open PR review on push to main', usd: 7.1, runs: 6 },
    ],
  },
  'atlas-ledger': {
    monthToDateUsd: 96.42,
    previousMonthToDateUsd: 122.8,
    monthlyBudgetUsd: 300,
    forecastUsd: 138.0,
    runs: 47,
    daily: dailySeries(28, 4.1, 19),
    byStage: [
      { stage: 'plan', label: 'Plan', usd: 14.2, tokensIn: 480_000, tokensOut: 52_000 },
      {
        stage: 'implement',
        label: 'Implement',
        usd: 48.9,
        tokensIn: 1_820_000,
        tokensOut: 240_000,
      },
      { stage: 'review', label: 'Review', usd: 20.1, tokensIn: 760_000, tokensOut: 88_000 },
      { stage: 'verify', label: 'Verify', usd: 9.7, tokensIn: 340_000, tokensOut: 36_000 },
      { stage: 'summarise', label: 'Summarise', usd: 3.52, tokensIn: 150_000, tokensOut: 18_000 },
    ],
    byModel: [
      {
        model: 'claude-sonnet-5',
        label: 'Sonnet 5',
        usd: 78.2,
        tokensIn: 3_020_000,
        tokensOut: 360_000,
      },
      {
        model: 'claude-haiku-4-5',
        label: 'Haiku 4.5',
        usd: 18.22,
        tokensIn: 1_100_000,
        tokensOut: 96_000,
      },
    ],
    byWorkItem: [
      { key: '0011', title: 'Migrate ledger postings to append-only store', usd: 52.8, runs: 22 },
      { key: '0012', title: 'Backfill projection from 2019 archive', usd: 28.4, runs: 15 },
      { key: '0013', title: 'Reconciliation report for finance', usd: 15.22, runs: 10 },
    ],
  },
  /**
   * Nine models over a month is ordinary for a team that never pins one. This
   * fixture exists so the six-plus case the chart palette has to survive is
   * actually rendered somewhere, not just reasoned about.
   */
  beacon: {
    monthToDateUsd: 512.4,
    previousMonthToDateUsd: 470.15,
    monthlyBudgetUsd: 900,
    forecastUsd: 731.2,
    runs: 268,
    daily: dailySeries(28, 21.2, 13),
    byStage: [
      { stage: 'plan', label: 'Plan', usd: 68.4, tokensIn: 2_410_000, tokensOut: 244_000 },
      {
        stage: 'implement',
        label: 'Implement',
        usd: 251.6,
        tokensIn: 8_940_000,
        tokensOut: 1_320_000,
      },
      { stage: 'review', label: 'Review', usd: 112.8, tokensIn: 4_180_000, tokensOut: 486_000 },
      { stage: 'verify', label: 'Verify', usd: 51.2, tokensIn: 1_910_000, tokensOut: 196_000 },
      {
        stage: 'summarise',
        label: 'Summarise',
        usd: 28.4,
        tokensIn: 1_040_000,
        tokensOut: 132_000,
      },
    ],
    byModel: [
      {
        model: 'claude-opus-5',
        label: 'Opus 5',
        usd: 188.4,
        tokensIn: 4_310_000,
        tokensOut: 716_000,
      },
      {
        model: 'claude-sonnet-5',
        label: 'Sonnet 5',
        usd: 142.6,
        tokensIn: 8_510_000,
        tokensOut: 945_000,
      },
      {
        model: 'claude-opus-4-8',
        label: 'Opus 4.8',
        usd: 74.2,
        tokensIn: 1_700_000,
        tokensOut: 282_000,
      },
      {
        model: 'claude-sonnet-4-6',
        label: 'Sonnet 4.6',
        usd: 51.3,
        tokensIn: 3_060_000,
        tokensOut: 340_000,
      },
      {
        model: 'claude-haiku-4-5',
        label: 'Haiku 4.5',
        usd: 38.6,
        tokensIn: 2_340_000,
        tokensOut: 224_000,
      },
      {
        model: 'claude-opus-4-6',
        label: 'Opus 4.6',
        usd: 17.3,
        tokensIn: 396_000,
        tokensOut: 65_800,
      },
    ],
    byWorkItem: [
      { key: '0042', title: 'Nightly rollup drifts from the event log', usd: 118.4, runs: 46 },
      { key: '0043', title: 'Schema drift alert is too noisy to act on', usd: 12.8, runs: 9 },
    ],
  },
  sandbox: {
    monthToDateUsd: 3.18,
    previousMonthToDateUsd: 0,
    monthlyBudgetUsd: 25,
    forecastUsd: 4.6,
    runs: 4,
    daily: dailySeries(28, 0.14, 41),
    byStage: [
      { stage: 'plan', label: 'Plan', usd: 0.62, tokensIn: 22_000, tokensOut: 3_100 },
      { stage: 'implement', label: 'Implement', usd: 1.84, tokensIn: 68_000, tokensOut: 9_400 },
      { stage: 'review', label: 'Review', usd: 0.44, tokensIn: 16_000, tokensOut: 2_100 },
      { stage: 'verify', label: 'Verify', usd: 0.2, tokensIn: 7_400, tokensOut: 900 },
      { stage: 'summarise', label: 'Summarise', usd: 0.08, tokensIn: 3_100, tokensOut: 400 },
    ],
    byModel: [
      {
        model: 'claude-haiku-4-5',
        label: 'Haiku 4.5',
        usd: 3.18,
        tokensIn: 116_500,
        tokensOut: 15_900,
      },
    ],
    byWorkItem: [],
  },
};
