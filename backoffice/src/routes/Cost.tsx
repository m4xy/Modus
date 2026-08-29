import { useDomain } from '../app/DomainContext';
import { useCostSummary } from '../api/queries';
import { PageHeader } from '../components/PageHeader';
import { DailySpendChart, ModelSplit, StageBars } from '../components/charts/Charts';
import { Card, CardBody, CardHeader, EmptyState, SkeletonList, Table, Td, Th } from '../ui';
import { formatCount, formatUsd } from '../lib/format';
import styles from './Cost.module.css';

export function Cost() {
  const { domain } = useDomain();
  const query = useCostSummary(domain.id);

  if (query.isPending) {
    return (
      <>
        <PageHeader
          eyebrow={`${domain.id} · cost`}
          title="Cost"
          description="Loading spend for this domain."
        />
        <SkeletonList rows={6} label="Loading cost summary" />
      </>
    );
  }

  if (query.isError || !query.data) {
    return (
      <>
        <PageHeader
          eyebrow={`${domain.id} · cost`}
          title="Cost"
          description="Spend for this domain."
        />
        <EmptyState
          title="Cost data is unavailable"
          description="The cost summary request failed. Reload to try again."
        />
      </>
    );
  }

  const summary = query.data;
  const delta = summary.monthToDateUsd - summary.previousMonthToDateUsd;
  const deltaPercent =
    summary.previousMonthToDateUsd === 0
      ? null
      : Math.round((delta / summary.previousMonthToDateUsd) * 100);
  const budgetPercent = Math.round((summary.monthToDateUsd / summary.monthlyBudgetUsd) * 100);
  const overBudget = budgetPercent > 100;

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · cost`}
        title="Cost"
        description="What this domain has spent on model calls this month, and where it went. Every run is attributed to a stage, a model and a work item."
      />

      <div className={styles.stack}>
        <section className={styles.hero} aria-label="Spend summary">
          <div className={styles.heroMain}>
            <span className={styles.heroLabel}>Month to date</span>
            <span className={styles.heroValue} data-testid="cost-hero">
              {formatUsd(summary.monthToDateUsd)}
            </span>
            <span className={styles.heroDelta}>
              <span className={delta >= 0 ? styles.up : styles.down}>
                {delta >= 0 ? '▲' : '▼'} {formatUsd(Math.abs(delta))}
                {deltaPercent !== null ? ` (${Math.abs(deltaPercent)}%)` : ''}
              </span>
              versus the same point last month
            </span>
          </div>

          <div className={styles.heroAside}>
            <div className={styles.asideRow}>
              <span className={styles.asideLabel}>Budget used</span>
              <span className={styles.asideValue}>
                {budgetPercent}% of {formatUsd(summary.monthlyBudgetUsd)}
              </span>
              <div
                className={styles.budgetTrack}
                role="meter"
                aria-valuenow={budgetPercent}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Share of the monthly budget used"
              >
                <div
                  className={`${styles.budgetFill} ${overBudget ? styles.budgetOver : ''}`}
                  style={{ width: `${Math.min(100, budgetPercent)}%` }}
                />
              </div>
            </div>
            <div className={styles.asideRow}>
              <span className={styles.asideLabel}>Forecast at this rate</span>
              <span className={styles.asideValue}>{formatUsd(summary.forecastUsd)}</span>
            </div>
            <div className={styles.asideRow}>
              <span className={styles.asideLabel}>Runs billed</span>
              <span className={styles.asideValue}>{formatCount(summary.runs)}</span>
            </div>
          </div>
        </section>

        <Card>
          <CardHeader
            eyebrow="Trend"
            title="Daily spend"
            description="Hover a column for the exact day."
          />
          <CardBody>
            <DailySpendChart points={summary.daily} title={`Daily spend in ${domain.name}`} />
          </CardBody>
        </Card>

        <div className={styles.split}>
          <Card>
            <CardHeader
              eyebrow="Pipeline"
              title="By stage"
              description="Where in a run the money goes."
            />
            <CardBody>
              <StageBars stages={summary.byStage} />
            </CardBody>
          </Card>

          <Card>
            <CardHeader
              eyebrow="Models"
              title="By model"
              description="Cheaper models do more tokens for less."
            />
            <CardBody>
              <ModelSplit models={summary.byModel} />
            </CardBody>
          </Card>
        </div>

        <Card>
          <CardHeader
            eyebrow="Attribution"
            title="By work item"
            description="Spend traced back to the bean that caused it."
          />
          <CardBody flush>
            {summary.byWorkItem.length === 0 ? (
              <EmptyState
                title="No attributed spend yet"
                description="Once a run is tied to a work item, its cost shows up here."
              />
            ) : (
              <Table caption={`Spend by work item in ${domain.name}`}>
                <thead>
                  <tr>
                    <Th>Work item</Th>
                    <Th numeric>Runs</Th>
                    <Th numeric>Spend</Th>
                    <Th numeric>Share</Th>
                  </tr>
                </thead>
                <tbody>
                  {summary.byWorkItem.map((item) => (
                    <tr key={item.key}>
                      <Td primary>
                        <span
                          style={{
                            fontFamily: 'var(--font-mono)',
                            color: 'var(--ink-3)',
                          }}
                        >
                          {item.key}
                        </span>{' '}
                        {item.title}
                      </Td>
                      <Td numeric mono>
                        {item.runs}
                      </Td>
                      <Td numeric>{formatUsd(item.usd)}</Td>
                      <Td numeric mono>
                        {Math.round((item.usd / summary.monthToDateUsd) * 100)}%
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )}
          </CardBody>
        </Card>
      </div>
    </>
  );
}
