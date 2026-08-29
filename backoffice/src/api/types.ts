/**
 * Wire types for the Modus API.
 *
 * Everything below the domain root is tenant-scoped: the server namespaces every
 * resource under `/domains/{domainId}`, so these types deliberately do NOT carry
 * a domainId on each record — the path already established the tenant.
 */

export type DomainId = string;

export type Environment = 'production' | 'staging' | 'sandbox';

export interface Domain {
    id: DomainId;
    name: string;
    environment: Environment;
    description: string;
    actorCount: number;
    monthToDateSpendUsd: number;
    monthlyBudgetUsd: number;
}

/**
 * Capabilities are granted per (actor, domain) pair. The shell reads them to
 * decide which navigation entries exist at all, and which are visible-but-locked.
 */
export type Capability =
    | 'work.read'
    | 'work.write'
    | 'repositories.read'
    | 'agents.read'
    | 'agents.run'
    | 'memories.read'
    | 'cost.read'
    | 'skills.read'
    | 'settings.read'
    | 'settings.write';

export interface Permissions {
    domainId: DomainId;
    capabilities: Capability[];
}

export type ActorRole = 'owner' | 'maintainer' | 'operator' | 'viewer';

export interface Actor {
    id: string;
    name: string;
    handle: string;
    email: string;
    role: ActorRole;
    initials: string;
}

export interface Session {
    actor: Actor;
    domains: Domain[];
    permissions: Permissions[];
}

/* ---- Work ---------------------------------------------------------- */

export type WorkStatus = 'backlog' | 'ready' | 'in-progress' | 'in-review' | 'blocked' | 'done';
export type WorkKind = 'epic' | 'story' | 'task';

export interface WorkItem {
    id: string;
    /** The bean number, e.g. "0002" — how humans refer to the item. */
    key: string;
    title: string;
    kind: WorkKind;
    status: WorkStatus;
    assignee: string | null;
    updatedAt: string;
    labels: string[];
    spendUsd: number;
    /** Markdown body of the bean file, rendered for humans. */
    body: string;
    parentKey: string | null;
}

/* ---- Repositories -------------------------------------------------- */

export interface Repository {
    id: string;
    name: string;
    remote: string;
    defaultBranch: string;
    lastSyncedAt: string;
    openWorkItems: number;
    status: 'connected' | 'syncing' | 'error';
}

/* ---- Agents -------------------------------------------------------- */

export type AgentRunStatus = 'queued' | 'running' | 'succeeded' | 'failed' | 'cancelled';

export interface AgentRun {
    id: string;
    workItemKey: string | null;
    trigger: string;
    model: string;
    status: AgentRunStatus;
    startedAt: string;
    durationMs: number;
    costUsd: number;
    tokensIn: number;
    tokensOut: number;
}

/* ---- Memories & skills --------------------------------------------- */

export interface Memory {
    id: string;
    title: string;
    scope: 'domain' | 'repository' | 'work-item';
    updatedAt: string;
    tokens: number;
    excerpt: string;
}

export interface Skill {
    id: string;
    name: string;
    summary: string;
    installedVersion: string;
    invocations30d: number;
    enabled: boolean;
}

/* ---- Cost ---------------------------------------------------------- */

export type CostStage = 'plan' | 'implement' | 'review' | 'verify' | 'summarise';

export interface CostPoint {
    /** ISO date (day granularity). */
    date: string;
    usd: number;
}

export interface StageCost {
    stage: CostStage;
    label: string;
    usd: number;
    tokensIn: number;
    tokensOut: number;
}

export interface ModelCost {
    model: string;
    label: string;
    usd: number;
    tokensIn: number;
    tokensOut: number;
}

export interface WorkItemCost {
    key: string;
    title: string;
    usd: number;
    runs: number;
}

export interface CostSummary {
    monthToDateUsd: number;
    previousMonthToDateUsd: number;
    monthlyBudgetUsd: number;
    forecastUsd: number;
    runs: number;
    daily: CostPoint[];
    byStage: StageCost[];
    byModel: ModelCost[];
    byWorkItem: WorkItemCost[];
}
