import { useState } from 'react';
import type { CostPoint, ModelCost, StageCost } from '../../api/types';
import { formatDay, formatTokens, formatUsd, formatUsdCompact } from '../../lib/format';
import styles from './Charts.module.css';

/**
 * Hand-built SVG rather than a charting library: three chart forms do not
 * justify 200KB of dependency, and drawing them ourselves is the only way the
 * marks obey the same token layer as the rest of the interface.
 *
 * Shared rules, applied in every chart here:
 *  - series colours come from the validated --series-* slots, assigned in fixed
 *    order and never cycled: there are five slots, and a sixth or later series
 *    is folded into a neutral "Other" bucket rather than reusing a colour,
 *    because a repeated colour reads as a repeated category;
 *  - a 2px gap in the surface colour separates touching marks;
 *  - gridlines are hairline and recessive, labels wear text tokens (never the
 *    series colour), and numbers are tabular.
 */

const VIEW_W = 720;
const VIEW_H = 200;
const PAD_LEFT = 44;
const PAD_RIGHT = 8;
const PAD_TOP = 14;
const PAD_BOTTOM = 22;

function niceCeiling(value: number): number {
  if (value <= 0) return 1;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  const steps = [1, 2, 2.5, 5, 10];
  for (const step of steps) {
    if (value <= step * magnitude) return step * magnitude;
  }
  return 10 * magnitude;
}

/** A column with a 4px rounded cap and square feet on the baseline. */
function columnPath(x: number, y: number, width: number, height: number): string {
  const r = Math.min(4, width / 2, height);
  return [
    `M${x} ${y + height}`,
    `V${y + r}`,
    `A${r} ${r} 0 0 1 ${x + r} ${y}`,
    `H${x + width - r}`,
    `A${r} ${r} 0 0 1 ${x + width} ${y + r}`,
    `V${y + height}`,
    'Z',
  ].join(' ');
}

export interface DailySpendChartProps {
  points: CostPoint[];
  title: string;
}

export function DailySpendChart({ points, title }: DailySpendChartProps) {
  const [active, setActive] = useState<number | null>(null);

  const max = niceCeiling(Math.max(...points.map((point) => point.usd), 0));
  const plotW = VIEW_W - PAD_LEFT - PAD_RIGHT;
  const plotH = VIEW_H - PAD_TOP - PAD_BOTTOM;
  const band = plotW / Math.max(points.length, 1);
  const barWidth = Math.min(24, band - 2); // 2px surface gap between neighbours
  const ticks = [0, max / 2, max];

  const peakIndex = points.reduce(
    (best, point, index) => (point.usd > (points[best]?.usd ?? 0) ? index : best),
    0,
  );

  const activePoint = active === null ? null : points[active];

  return (
    <figure className={styles.figure}>
      <svg
        className={styles.chart}
        viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
        role="img"
        aria-label={`${title}. Peak day ${points[peakIndex] ? formatDay(points[peakIndex].date) : ''} at ${formatUsd(points[peakIndex]?.usd ?? 0)}.`}
        onMouseLeave={() => setActive(null)}
      >
        {ticks.map((tick) => {
          const y = PAD_TOP + plotH - (tick / max) * plotH;
          return (
            <g key={tick}>
              <line className={styles.grid} x1={PAD_LEFT} x2={VIEW_W - PAD_RIGHT} y1={y} y2={y} />
              <text className={styles.axisText} x={PAD_LEFT - 8} y={y + 3} textAnchor="end">
                {formatUsdCompact(tick)}
              </text>
            </g>
          );
        })}

        {points.map((point, index) => {
          const height = Math.max(2, (point.usd / max) * plotH);
          const x = PAD_LEFT + index * band + (band - barWidth) / 2;
          const y = PAD_TOP + plotH - height;
          return (
            <path
              key={point.date}
              className={`${styles.bar} ${active !== null && active !== index ? styles.dimmed : ''}`}
              d={columnPath(x, y, barWidth, height)}
              fill="var(--series-1)"
            />
          );
        })}

        {/* One direct label — on the peak — rather than a number on every column. */}
        {points[peakIndex] && (
          <text
            className={styles.valueLabel}
            x={PAD_LEFT + peakIndex * band + band / 2}
            y={PAD_TOP + plotH - (points[peakIndex].usd / max) * plotH - 6}
            textAnchor="middle"
          >
            {formatUsd(points[peakIndex].usd)}
          </text>
        )}

        {points.map((point, index) =>
          index % 7 === 0 ? (
            <text
              key={`tick-${point.date}`}
              className={styles.axisText}
              x={PAD_LEFT + index * band + band / 2}
              y={VIEW_H - 6}
              textAnchor="middle"
            >
              {formatDay(point.date)}
            </text>
          ) : null,
        )}

        {points.map((point, index) => (
          <rect
            key={`hit-${point.date}`}
            className={styles.hit}
            x={PAD_LEFT + index * band}
            y={PAD_TOP}
            width={band}
            height={plotH}
            onMouseEnter={() => setActive(index)}
          />
        ))}
      </svg>

      {activePoint && active !== null && (
        <div
          className={styles.tooltip}
          style={{
            left: `${((PAD_LEFT + active * band + band / 2) / VIEW_W) * 100}%`,
            top: '4px',
          }}
        >
          <p className={styles.tooltipLabel}>{formatDay(activePoint.date)}</p>
          <p className={styles.tooltipValue}>{formatUsd(activePoint.usd)}</p>
        </div>
      )}

      <figcaption className={styles.tooltipNote}>
        Daily spend across every stage. Weekends fall away because triggers are quieter.
      </figcaption>
    </figure>
  );
}

export function StageBars({ stages }: { stages: StageCost[] }) {
  const max = Math.max(...stages.map((stage) => stage.usd), 0);
  const total = stages.reduce((sum, stage) => sum + stage.usd, 0);

  return (
    <div className={styles.rows}>
      {stages.map((stage) => (
        <div className={styles.row} key={stage.stage}>
          <span className={styles.rowLabel}>{stage.label}</span>
          <div className={styles.track}>
            <div
              className={styles.fill}
              style={{
                width: `${max === 0 ? 0 : (stage.usd / max) * 100}%`,
                background: 'var(--series-1)',
              }}
            />
          </div>
          <span className={styles.rowValue}>{formatUsd(stage.usd)}</span>
          <span className={styles.rowNote}>
            {total === 0 ? '0%' : `${Math.round((stage.usd / total) * 100)}%`} of spend ·{' '}
            {formatTokens(stage.tokensIn)} in / {formatTokens(stage.tokensOut)} out
          </span>
        </div>
      ))}
    </div>
  );
}

const SERIES = [
  'var(--series-1)',
  'var(--series-2)',
  'var(--series-3)',
  'var(--series-4)',
  'var(--series-5)',
];

/** Reserved for the bucket, never for a real series. */
const OTHER_COLOUR = 'var(--series-other)';

const OTHER_KEY = '__other__';

/**
 * Keeps the "never cycled" rule true for any number of models.
 *
 * The palette has five validated slots. Given more series than that, the
 * smallest are folded into one "Other" slice in a neutral colour, so no two
 * slices ever share a colour and the CVD separation the palette was validated
 * for still holds. Models arrive ordered by spend, largest first.
 */
function withinPalette(models: ModelCost[]): ModelCost[] {
  if (models.length <= SERIES.length) return models;

  const head = models.slice(0, SERIES.length - 1);
  const tail = models.slice(SERIES.length - 1);
  return [
    ...head,
    {
      model: OTHER_KEY,
      label: `Other (${tail.length} models)`,
      usd: tail.reduce((sum, model) => sum + model.usd, 0),
      tokensIn: tail.reduce((sum, model) => sum + model.tokensIn, 0),
      tokensOut: tail.reduce((sum, model) => sum + model.tokensOut, 0),
    },
  ];
}

function colourFor(model: ModelCost, index: number): string {
  return model.model === OTHER_KEY ? OTHER_COLOUR : (SERIES[index] ?? OTHER_COLOUR);
}

export function ModelSplit({ models: allModels }: { models: ModelCost[] }) {
  const models = withinPalette(allModels);
  const total = models.reduce((sum, model) => sum + model.usd, 0);

  return (
    <div>
      <div
        className={styles.stack}
        data-testid="model-split"
        role="img"
        aria-label={`Spend split by model: ${models
          .map((model) => `${model.label} ${formatUsd(model.usd)}`)
          .join(', ')}`}
      >
        {models.map((model, index) => (
          <div
            key={model.model}
            className={styles.segment}
            style={{
              width: `${total === 0 ? 0 : (model.usd / total) * 100}%`,
              background: colourFor(model, index),
            }}
          />
        ))}
      </div>

      {/* Legend is always present for two or more series — identity never rests
          on colour matching alone. */}
      <div className={styles.legend}>
        {models.map((model, index) => (
          <span className={styles.legendItem} key={model.model}>
            <span
              className={styles.swatch}
              style={{ background: colourFor(model, index) }}
              aria-hidden="true"
            />
            {model.label}
          </span>
        ))}
      </div>

      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">Model</th>
            <th scope="col">Tokens in / out</th>
            <th scope="col">Spend</th>
          </tr>
        </thead>
        <tbody>
          {models.map((model, index) => (
            <tr key={model.model}>
              <td>
                <span className={styles.keyCell}>
                  <span
                    className={styles.swatch}
                    style={{ background: colourFor(model, index) }}
                    aria-hidden="true"
                  />
                  {model.label}
                </span>
              </td>
              <td>
                {formatTokens(model.tokensIn)} / {formatTokens(model.tokensOut)}
              </td>
              <td>{formatUsd(model.usd)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
