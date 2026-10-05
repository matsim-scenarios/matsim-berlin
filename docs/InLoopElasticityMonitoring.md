# In-loop instruments: elasticity monitoring and ASC calibration

Status: elasticity monitoring (route 1) and in-loop ASC calibration implemented on branch
`michaz/choicemodel`; monitoring routes 2 and 3 are design sketches. Smoke-scale validation
done for the monitor (1pct); values provisional pending 10pct. Both instruments share the
same analytical core: a scale-1 logit over each agent's scored plan memory, from which
expected mode trips and their derivatives are closed-form.

## Motivation

The price elasticity of the simulated demand is a *derivative* of the model, and no part of
the classical pipeline (estimation → calibration → validation) ever observes it: estimation
fits choices, calibration matches share *levels*, dashboards compare *distributions*. Two
models can agree on all of these and differ by an order of magnitude in their price response.
This monitor makes the derivative layer observable while a run is still relaxing, at
negligible cost, so that elasticity regressions are caught in iteration 50 of a run instead
of in a dedicated study years later.

## Route 1 (implemented): analytic derivative through the selection stage

`org.matsim.analysis.PointElasticityStatsModule`, registered unconditionally in
`OpenBerlinScenario.prepareControler`.

Every iteration, for every agent of subpopulation `person`:

1. Treat the agent's plan memory as a choice set and their plan choice as a **scale-1 logit**
   over the scored plans. This is exact under the `ChangeExpBeta` selector (its stationary
   selection is `P_j ∝ exp(score_j)`); under `BestScore` it is a well-defined diagnostic
   ("what a logit selector would do with these memories").
2. For each plan, compute the **money exposure** that a multiplicative cost factor scales,
   mirroring the `--car-cost-factor` / `--pt-cost-factor` semantics:
   - car: fuel (`monetaryDistanceRate` × car and ride distance) + car `dailyMonetaryConstant`
     if car is used;
   - pt: pt `dailyMonetaryConstant` if pt is used;
   both priced through the person's own `marginalUtilityOfMoney` (income scaling, where
   configured, is thereby honored automatically).
3. The derivative of expected mode trips w.r.t. the cost factor (at factor 1) is then
   closed-form per agent: the **covariance, under the plan-choice distribution, between the
   plan's mode trips and its money exposure**:

   `d E[trips_m] / d f = Σ_j P_j · n_j · x_j − (Σ_j P_j n_j)(Σ_j P_j x_j)`

   Summed over agents and divided by expected trips of the mode, this yields a point
   elasticity of the behavioral stage at the current relaxation state.

Frozen components (taste variations, situational error terms) are inside the scores and are
therefore handled correctly by construction; the measured object is the model's elasticity
*at fixed draws*, which is the model's own definition of its price response.

### Output

- `elasticity_stats.csv`: `iteration, elasticity_car, elasticity_pt, elasticity_bike_speed,
  share_car, share_pt, share_bike`. The bike series is the elasticity of bike trips w.r.t.
  a bike *speed* factor (exposure = -(bike time coefficient) x bike hours, read from the
  person's scoring parameters, so it is correct in both model arms); expected sign positive.
  The bike hours are taken from the leg's travel time or, where that is gone, from its
  route: `VspPlansCleaner` clears the leg travel time of every executed plan when
  `plans.removingUnnecessaryPlanAttributes` is set, which all run configs do. Until
  2026-10-05 the monitor read the leg only, so **every bike-speed value produced before
  that date is zero by construction**, not a measurement.
- `elasticityEstimate.png`: rewritten every iteration (core score-statistics style).

### Reading guide

- The metric is a **trajectory, not a number**. Starting from relaxed plans, expect a ramp
  from near zero (pruned, top-heavy memories carry little margin mass), a hump tracking the
  annealing/innovation rate (exploration refills the choice sets), and a plateau as
  innovation dies. **The plateau is the estimate.**
- The plateau's *noise texture* fingerprints the selector: frozen to ~5 decimals under
  `BestScore` (deterministic fixed point), small jitter under `ChangeExpBeta` (stationary
  mixing).
- **Noise floor**: a continuation run with innovation disabled leaves memories frozen; the
  residual fluctuation calibrates how much movement is meaningful.
- **Integrity check**: a continuation of relaxed plans with unchanged parameters must plateau
  where it started. Disagreement means the run differs from the one that produced the plans.
- Compare plateau to plateau (or matched annealing phases); never mid-churn to converged.

### Caveats

- **Open loop**: response at frozen network conditions. Congestion feedback is not included
  (see route 3).
- **Choice-set conditioned**: the metric sees only alternatives present in memory. Weak
  exploration biases it toward zero — which is itself diagnostic (the gap to a route-2
  measurement localizes "inelastic utilities" vs. "inelastic exploration").
- The pt daily-cost lever produces derivative mass only for agents whose memories straddle
  the "pt-free day" margin (all-or-nothing exposure). Its structural smallness is a finding
  about the fare specification, not a monitor artifact.

### First observations (1pct, smoke scale)

Re-estimated utilities under the re-estimated apparatus vs. under the published apparatus
(identical utilities, one accidental-but-useful ablation): plateau `e_car` −0.132 vs −0.112,
`e_pt` −0.0147 vs −0.0109. The apparatus (situational error components + ride-only taste +
`ChangeExpBeta` instead of wide frozen taste + `BestScore`) contributes +18 % / +35 %;
the remaining distance to literature values lives in the utilities and in the open-loop /
exploration gaps. The published-baseline cell is pending.

## Route 2 (sketch): within-run perturbation

Finite differences without a second run, sharing the mobsim:

- **Split-sample**: randomly partition agents; one half scores (and chooses) under a slightly
  perturbed price. The A–B share difference tracks a finite-difference elasticity with
  maximal common random numbers. Bias: the perturbed half shifts network conditions for
  everyone; second-order for small perturbations.
- **Ghost twins**: massless clones that experience the network but do not load it, scored at
  the perturbed price. Per-agent paired comparison, zero equilibrium contamination, open
  loop like route 1 but including full behavioral re-optimization (innovation acts on the
  ghosts) — thereby measuring the exploration component that route 1 misses.

## Route 3 (sketch): closed-loop assembly via the implicit function theorem

The equilibrium elasticity differs from the open-loop one by demand–supply feedback:

`dQ/dp = (∂Q/∂p) / (1 − ∂Q/∂c · ∂c/∂Q)`

Route 1 supplies `∂Q/∂p`. The congestion sensitivity `∂c/∂Q` is estimable *from the run
itself*: the annealing noise perturbs demand iteration to iteration; regressing realized
travel times on realized volumes over recent iterations gives a local estimate. Together
they estimate the equilibrium elasticity without running the policy.

## In-loop ASC calibration (implemented)

`org.matsim.analysis.InLoopAscCalibration`, enabled with `--in-loop-asc-calibration`
(default off; the offset store is bound and inert in every run so the scoring factories can
depend on it unconditionally).

### Idea

The outer-loop python calibration (`calibrate.py` / `ASCCalibrator`) treats equilibrium
shares as an oracle: one full relaxed run per constant update, with the logit-style update
`Δasc = ln(target) − ln(share)` between runs. The in-loop version interleaves instead:
constants are updated *during* a single run while the co-evolution relaxes, so calibration
and equilibrium converge jointly. This is standard two-timescale stochastic approximation
(slow constant updates riding on the fast plan/network adaptation); Cadyts is the
in-ecosystem precedent for in-loop calibration with convergence theory. The same
per-iteration machinery as the elasticity monitor supplies the local sensitivity
`∂ ln(share_m)/∂ asc_m` (covariance formula with exposure ≡ trips of the mode), turning the
fixed-gain update into a Newton-conditioned one — measured gains instead of guessed ones.

### Mechanics

- **Offsets through scoring, not config**: per-mode additive constants are applied once per
  trip (by main mode) via a `SumScoringFunction.TripScoring` component wired into
  `BerlinScoringFunctionFactory`. Mutating config constants mid-run is *not* an option:
  `ScoringParametersForPerson` implementations cache per person and would go stale silently.
- **Measurement**: expected shares (smooth, not realized selections) over persons with ids
  starting `berlin`, mirroring the python calibration's person filter; the offsets apply to
  everyone's scoring, as in the outer loop.
- **Update**: relative log error `(ln T_m − ln S_m) − (ln T_walk − ln S_walk)` (walk fixed),
  divided by the measured sensitivity (floored at 0.25 so stale early memories cannot cause
  explosive steps), gain 0.3, step cap ±0.2 utils/iteration. While the measured sensitivity
  is below the floor, the update is therefore 1.2 x the log error per iteration.
- **Self-determined convergence**: the calibrator ignores the annealing schedule and the
  iteration budget. It updates from iteration 0 and **commits** when, for 25 consecutive
  iterations (earliest at iteration 30), all share errors stay within 0.5 percentage points,
  the largest applied step stays below 0.02 utils, and the churn (share of plan objects in
  the counted memories that are new since the previous iteration) stays below 0.02. The
  churn condition ties the commit to the end of innovation: at an innovation rate of 0.45
  the churn is around 0.1, so with the default annealing a commit cannot happen before
  innovation is switched off. After committing, offsets are frozen for the rest of the run —
  the run's tail relaxes under the final constants, which doubles as the confirmation phase.
  The commit is provisional: five consecutive iterations with a share error beyond twice the
  tolerance re-arm the calibrator.

### Output

- `asc_calibration_stats.csv`: per iteration, the committed flag, the churn, and per mode
  the offset, the share error and the measured sensitivity `d ln(share)/d asc` before the
  floor is applied. The sensitivity column tells whether the system can respond to the
  offsets yet; far below the floor, the update is integrating an error it cannot influence.
- `ascOffsets.png`, `ascShareErrors.png`: rewritten every iteration (commit iteration shown
  in the offsets chart title once reached).
- `asc_offsets_final.txt` on commit: final offsets and config-ready constants
  (current config constant + offset per mode).

### Correctness conditions and caveats

- **Vintage mixing**: plans in memory carry the offsets of the iteration they were last
  scored in. This mixing vanishes as updates settle and is second-order at the gains used —
  the standard Cadyts situation. It is the reason the calibrator pairs naturally with the
  `ChangeExpBeta` selector (memory plans get re-executed and thus re-scored); under
  `BestScore`, never-reselected plans keep stale offsets indefinitely.
- **Diagonal Jacobian**: cross-mode sensitivities are ignored; the step cap covers for it.
- **The commit-then-relax tail is load-bearing**: the fixed point being certified is "the
  *converged* system under the final constants matches targets", so the run must continue
  long enough after commit for the shares to be read off a settled state. The post-commit
  stretch of `ascShareErrors.png` is that certificate.
- **Compensator discipline** (the central lesson of the surrounding audit): an in-loop
  calibrator is a continuous residual absorber. The offset *trajectory* is therefore part of
  the result, not debug output — the final drift of the constants from their estimated
  values remains the structural-mismatch diagnostic, and a calibration whose trajectory is
  not inspected is the perception factor waiting to happen again.
- For decisions of record, one plain confirmation run at the final constants (calibration
  off) remains the honest closing step — one oracle query to certify the answer, instead of
  one per update.

### Interplay with the elasticity monitor

Both instruments run every iteration and share the plan-memory logit core. After the
calibrator commits, the elasticity monitor's post-commit plateau is the **calibrated**
open-loop elasticity — a single run thus calibrates itself, certifies its shares, and
measures its own price response on the way out.
