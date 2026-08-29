import type { Capability } from '../api/types';

export interface NavItem {
  /** Path segment below /domains/:domainId */
  segment: string;
  label: string;
  /** Capability the actor must hold. Missing it hides or locks the entry. */
  capability: Capability;
  /**
   * How the shell reacts when the capability is absent:
   *  - 'hide'  the surface should not be discoverable at all
   *  - 'lock'  the surface exists in this product but not for this actor, so we
   *            show it disabled rather than pretending it does not exist
   */
  whenDenied: 'hide' | 'lock';
  description: string;
}

export const navItems: NavItem[] = [
  {
    segment: 'work',
    label: 'Work',
    capability: 'work.read',
    whenDenied: 'lock',
    description: 'Epics, stories and tasks tracked as beans',
  },
  {
    segment: 'repositories',
    label: 'Repositories',
    capability: 'repositories.read',
    whenDenied: 'lock',
    description: 'Connected repositories and sync state',
  },
  {
    segment: 'agents',
    label: 'Agents',
    capability: 'agents.read',
    whenDenied: 'lock',
    description: 'Console and run history',
  },
  {
    segment: 'memories',
    label: 'Memories',
    capability: 'memories.read',
    whenDenied: 'lock',
    description: 'What this domain remembers between runs',
  },
  {
    segment: 'cost',
    label: 'Cost',
    capability: 'cost.read',
    whenDenied: 'hide',
    description: 'Spend by stage, model and work item',
  },
  {
    segment: 'skills',
    label: 'Skills',
    capability: 'skills.read',
    whenDenied: 'lock',
    description: 'Installed modules an agent can call',
  },
  {
    segment: 'settings',
    label: 'Settings',
    capability: 'settings.read',
    whenDenied: 'hide',
    description: 'Domain configuration and budgets',
  },
];
