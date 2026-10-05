# Cost function re-estimation: audit findings

Status: findings from the choice-model audit and re-estimation on branch
`michaz/choicemodel` (2026-08). Estimates are smoke-scale (1pct, k=9 plan candidates)
pending k=70 / 10pct confirmation. Companion documents: `InLoopElasticityMonitoring.md`
(instruments), the estimation scripts `trip_model.py` / `plan_model.py` (specifications
as executable formulas).

## The question

Simulated demand in the published v7.1 model is price-inelastic by roughly an order of
magnitude against literature values (pt fare elasticity ~ -0.02..-0.05 vs. ~ -0.3;
car cost <= ~ -0.16 open-loop vs. ~ -0.3..-0.4). The audit traced where the price
response went. Short answer: **variance was systematically booked into containers that
do not respond to prices** -- and each container has a name and a history.

## Findings

### 1. There is no price variation in the estimation data

Fuel price, daily costs, and fares are constants across the SrV sample. Every money
parameter in the published chain (`UTIL_MONEY`, `EXP_INCOME`, the price perception
factor) was identified through functional form and assumed cost structures, never
through observed price responses. A freely estimated car distance-cost term is a time
coefficient in disguise (corr(km, hours) = 0.94). Consequence: money coefficients must
be *chosen and defended* (VTTS transfer: mUoM = performing / literature VTTS), not
estimated -- until real price variation is brought in (Deutschlandticket / SrV 2023,
spatial parking costs, SP data).

### 2. The price perception factor (0.2686) is a patched symptom, not a preference

Origin (office history + artifacts): with full daily costs and uniform time valuation,
share-only ASC calibration produced a positive car constant and concentrated car trips
on short-trip/many-trip agents ("ASC harvesting") -- a wildly wrong distance-bracket
mode share. The fix scaled costs down until the constant stayed negative. This trades a
*level* symptom (invisible to share calibration) for a *derivative* cost: every fare and
fixed-cost policy operates at 27% strength. In estimation, the factor was identified via
a per-trip proration of daily costs by distance share (`dist_weight`) that is collinear
with fuel within-person and endogenous between-persons; internally acknowledged as
bogus. **Retired**: daily costs enter at full value, as day-level lumps conditional on
use -- which is also the office's own ownership philosophy ("fixed costs accepted or
not"), estimated correctly for the first time at the plan level.

### 3. The income exponent has no support in choice data

Profiling the exponent at the plan level with honest cost lumps: best fit ~0.13 (n.s.),
CI excluding the literature band (0.5..1.0); free income-ASC covariates find only a weak
income-*taste* gradient with the wrong sign pattern for price sensitivity; taste sds are
unmoved by any income specification. Two structural reasons: income measurement is
bracket-imputed (attenuation), and the income-price margin in reality is car *ownership*
-- which the pipeline deletes (`SetCarAvailabilityByAge`: every adult gets
carAvail=always and a license; the survey's V_PKW_VERFUEG is overwritten). **Dropped**
(exponent = 0), with the note that income weighting for *welfare analysis* is a separate,
legitimate normative choice that does not require a behavioral exponent.

### 4. Uniform time valuation forced everything else to compensate

The doctrine "all travel time costs `performing`" leaves the distance-slope of mode
attractiveness with no systematic carrier. Three successive compensations were built for
the same misfit: (i) the perception factor (cost scaling), (ii) taste variations
(validated in part on distance distributions), (iii) -- this audit -- **mode-specific
time offsets**, estimated at trip level: totals walk -7.0 / ride -5.3 / bike -3.6 /
car -2.9 (incl. fuel) / pt -1.4 utils/h. The ratios reproduce literature findings
(pt in-vehicle ~ 0.5x car; walk ~ 2.4x car; ride ~ 1.8x car vs. the assumed 2x), and per
km, car and pt decay identically -- their competition is level, not slope. The ablation:
the classic uniform-time skeleton explains almost nothing at plan level (rho^2 0.03 vs
0.28+); the old spec's fit lived entirely in its compensating containers.

### 5. The taste variations are, mode by mode, not taste

Offering the plan-level estimation both containers -- person-permanent taste sds and
situational per-trip error components (EC) -- and letting the likelihood decide:

| published sd | verdict |
|---|---|
| pt 1.74 | situational noise (pt_s -> 0 robustly) |
| bike 0.88 | situational noise |
| car 1.51 | **ownership margin proxy**; dies once the daily lump enters at full value |
| ride 2.86 | **genuine person-level structure**: household driver availability |

Final structure: one situational scale EC_S ~ 2.2 (well-conditioned, agrees across
specifications), one surviving taste term (ride ~ 2.9), zero frozen mode taste
elsewhere. Supporting findings: the plan-candidate generator emits duplicate mode
sequences (~7% of slots at k=9; red-bus/blue-bus artifacts; dedup added, upstream fix
pending); estimated sds are draw-count- and ridge-sensitive (joint models at n=988 are
canyons; parsimonious models are stable); one-day SrV data cannot distinguish
day-persistent from permanent components -- the freeze is a modeling choice, documented.

### 6. The error scale is now an estimate, not a conversion

The published pipeline normalized noise at the plan level (logit kernel), simulated it
at trip level, and bridged with pi^2/6/mean-trips -- approximate by design (per-person
trip counts), and shipped with a units slip (variance 0.495 passed as sd). Replaced:
EC_S is estimated *at the granularity the simulation implements* and transfers verbatim;
the estimation's residual plan-level Gumbel is supplied by the ChangeExpBeta selector
(scale 1 matches the estimation normalization by construction). The selector returns,
with its implicit noise now corresponding to a specific estimated term.

### 7. Practice: levels, compositions, derivatives

Share calibration is blind to composition; composition is blind to derivatives. The
pipeline historically validated layer 1, patched layer 2 when it screamed, and never
observed layer 3. Standing consequences, now instrumented (see
`InLoopElasticityMonitoring.md`): distance-bracket mode share and in-loop elasticity as
acceptance metrics; calibration offsets logged as trajectories, with the budget rule
that an offset is pathological when it exceeds the priced sum of knowingly omitted
mechanisms (ride's current budget: ~2 utils = 1 SE of its constant + ln(2.7) choice-set
inflation; first live calibration landed all offsets <= 0.7).

## Recommendation: the re-specified cost function

The findings above each end in a decision. Taken together they are the audit's primary
recommendation: replace the published specification by the one below. Values are the
smoke-scale estimates (1pct, k=9) as transferred into the run class, where
`--scoring-model reestimated` applies all of them in code (`OpenBerlinScenario.prepareConfig`
/ `prepareScenario`, error scale in `BerlinScoringModule`).

| Component | Published v7.1 | Recommended | Finding |
|---|---|---|---|
| Money coefficient | 0.397 utils/EUR, estimated at trip level | *chosen* and defended by value-of-time transfer, not estimated; 0.397 retained as the current choice | 1 |
| Daily costs | car 14.30 and pt 3.00 EUR/day, scaled by the perception factor 0.269 | full value, as day-level lumps conditional on use; perception factor retired | 2 |
| Fuel | 0.149 EUR/km | unchanged | -- |
| Income exponent | 0.276 | 0; income weighting is left to welfare analysis | 3 |
| Performing | 5.50 utils/h, the only time cost of car, pt and walk | 6.0 utils/h, fixed as the reference for the offsets | 4 |
| Mode-specific time | extra disutility for bike (-1.31) and ride (-4.44) only | an offset for every mode, estimated at trip level; net utils/h: walk -7.13, bike -3.91, ride -4.02, car -2.01, pt -1.84 (ride = 2 x car; car and ride before fuel) | 4 |
| pt transfer | -1 per switch, fixed | -0.32 per switch, estimated jointly with the pt time offset | 4 |
| Bus leg | -0.164 per leg | dropped | 4 |
| Person-level taste | normal sds on the constants of car 1.51, pt 1.74, bike 0.88, ride 2.86 | ride only, sd 2.92 (household driver availability); none for car, pt, bike | 5 |
| Situational error | normal per trip, sd 0.495 (a variance passed as sd) | normal per trip, sd 2.21, estimated at plan level at the granularity the simulation implements | 5, 6 |
| Plan selector | BestScore | ChangeExpBeta; its scale-1 logit is the estimation's residual plan-level Gumbel | 6 |
| Mode constants | calibrated means (10pct): car -2.09, pt -1.18, bike -1.09, ride -5.78 | estimation results as starting values: car -1.66, pt -2.93, bike -2.28, ride -7.05; then calibrated, in-loop (`--in-loop-asc-calibration`) | 7 |
| Acceptance | mode shares | mode shares, distance-bracket shares, in-loop elasticity, and the offset budget rule | 7 |

How these values are estimated is laid out in the two-stage tables at the end of this
section. Three things in this table are choices rather than estimates and should be defended as
such: the money coefficient (finding 1), the exponent of zero (finding 3), and freezing the
ride term as permanent rather than day-persistent (finding 5).

The net time values are the ones currently in the code. Finding 4 quotes car and ride
including fuel and slightly different totals for bike (-3.6) and pt (-1.4); the two should
be reconciled when the k=70 / 10pct estimates replace the smoke-scale ones.

### How the values are obtained: two estimation stages

Both the published and the recommended specification are estimated in two steps on the SrV
reference persons. Their reported day is replayed in the simulation, so every alternative,
chosen or not, has a routed travel time and distance. The two steps differ in what counts
as one choice:

- **Trip level.** One observation is one reported trip; the alternatives are the five
  modes for that trip. A trip knows its own time, distance and transfers, and nothing
  about the rest of the day.
- **Plan level.** One observation is one person's whole day; the alternatives are the
  reported day and a set of other mode combinations for the same activity chain
  ("candidates", generated by the SubtourModeChoice mutation). Only here do day-level
  things exist: a daily cost paid once if a mode is used at all, a vehicle that has to
  come back home, and anything that is constant for a person across the day.

The first stage fixes what can be read off single trips, the second estimates the rest
with the first stage's results held fixed. What the two specifications put into which
stage is the difference.

**Stage 1, trip level** (multinomial logit)

| | Published | Recommended |
|---|---|---|
| Script | `estimate_biogeme_trip_choice.py` | `trip_model.py` |
| Data | 27,732 trips | 31,976 trips |
| Travel time | one coefficient for all modes (performing), estimated; ride counted twice | performing fixed at 6.0; on top of it one coefficient per mode (walk, bike, car, pt), estimated; ride counted as twice car |
| pt transfer | fixed at -1 | estimated |
| Fuel | 0.149 EUR/km | 0.149 EUR/km |
| Daily costs | included: split over the day's trips by each trip's share of the daily distance, times a perception factor that is estimated | left out: a single trip cannot carry a per-day cost |
| Money coefficient, income exponent | estimated | not estimated: coefficient fixed at the chosen value, exponent 0 |
| Constants | four, estimated, not carried forward | four, estimated, not carried forward |
| Result handed to stage 2 | performing, money coefficient, income exponent, perception factor | the four per-mode time coefficients and the transfer penalty |

**Stage 2, plan level** (mixed logit)

| | Published | Recommended |
|---|---|---|
| Script | `estimate_biogeme_plan_choice.py` | `plan_model.py` |
| Data | 8,400 persons, 70 candidates each | 988 persons, 9 candidates each so far (70 pending); duplicate candidates removed |
| Held fixed from stage 1 | performing, money coefficient, income exponent, perception factor | per-mode time coefficients, transfer penalty |
| Daily costs | once per day if the mode is used, times the perception factor | once per day if the mode is used, at full value |
| Estimated: constants | one mean per mode | one per mode |
| Estimated: differences between persons | a normal sd on the constant of car, pt, bike and ride; one draw per person, the same on all of that person's trips | the same, for ride only |
| Estimated: noise per trip | none | one scale for a normal draw per mode and trip, independent across a person's trips |
| Estimated: other | extra time disutility for bike and ride; bus-leg constant | none |
| Result handed to the simulation | the four sds (drawn once per person), bike and ride time terms, bus constant; constants as calibration start | the noise scale, the ride sd; constants as calibration start |
| Noise per trip in the simulation | not from the estimation: the Gumbel variance divided by the mean number of trips per day | the estimated scale, unchanged |

Read across, the tables are findings 2, 4, 5 and 6 again: the published chain puts a
day-level quantity, the daily cost with its perception factor, into the trip-level stage,
where it can only enter through the proration; it estimates no time structure beyond one
coefficient; and it offers the plan-level stage only one container for unexplained
variation, differences between persons. The recommended chain moves the daily cost to the
stage where it exists, gives every mode its own time coefficient, and lets per-person and
per-trip variation compete.

## Alternative avenue: a minimal incremental fix to the *original* model

For the case where the full re-specification is too large a step, the smallest
well-grounded change-set that should materially narrow the car and pt ASC distributions
(and thereby restore price response) uses only switches that already exist in the
published pipeline -- no new code, no new data:

1. **Estimate car and pt time efforts** in the published stage-2
   (`--effort car nan --effort pt nan`, extending the mechanism already used for bike
   and ride). This gives the distance slope a systematic carrier, which is what
   historically made full costs "impossible" (the positive-ASC/harvesting episode).
2. **Drop the perception factor** (`--price-perception 1.0`): daily costs at full value.
   Safe once (1) carries the composition; restores the fare/fixed-cost lever to 100%.
3. **Enable the error components** (`--est-error-component`, the unused flag): the
   trip-level noise gets its own container, and the car/pt sds shrink toward whatever
   person-permanent structure genuinely remains (expected: pt near zero, car much
   reduced since the full daily lump now carries the ownership margin).
4. Fix the pseudo-random scale units (0.495 -> 0.703), or better, use the estimated ec_s.

Then recalibrate ASCs as usual. Expected outcome: same share fit, distance brackets held
by the effort terms, car/pt taste distributions much narrower, pt fare lever ~3.7x
stronger -- i.e., most of the elasticity recovery at perhaps a tenth of the change
surface. What it deliberately does not fix: ride availability, the income question, the
stage-1 proration (moot if stage-1 money is fixed externally), fare structure (the pt
daily-lump remains a structurally weak lever; per-trip/zone fares and an Abo margin are
the real fix and require model extensions).

## Open validation item: bike speed elasticity

The historical motivation for taste variations was that the homogeneous model's
bike-speed response was considered too high (validated in the published work against
Li et al. 2018 at +31.4% share per +2 km/h -- an infrastructure-elasticity source whose
transferability to a uniform speed change is itself questionable). The re-estimated
model changes both the bike time lever (3.9 vs 6.8 utils/h) and the margin noise
structure (EC vs bike_s); the net effect on speed elasticity is *not* obviously in
either direction, because both specifications fit the same observed share-by-distance
curves, which anchor the speed response (the homogeneous baseline's excess elasticity
came precisely from misfitting those curves). Required before the re-specification is
declared non-regressive: (a) the in-loop monitor's bike-speed series
(implemented: exposure = -(bike time coefficient) x bike hours), (b) one finite-difference pair
(vehicle-types bike speed +2 km/h), (c) an office decision on what the *target* bike
speed elasticity actually is, since the current one is anchored to a questionable
transfer.

## Outlook: making the money terms drop out of the data

Goal (K.N.): a model in the classical mold whose cost sensitivity -- including an honest
per-cost-type "perception" -- is *estimated* from SrV or SrV-like data, rather than fixed
externally with the residual labeled as non-monetary "effort". Finding 1 says this is
impossible within a single SrV wave as currently used: nobody in the sample faces a
different price, so the money/time split is underdetermined and any split is a choice.
The resolution is not cleverness but *variation*: price contrasts that either already sit
in the SrV data unused, or can be attached to it. With them in place, the money
coefficients are pinned by price contrasts, perception becomes an estimable ratio
(estimated cost coefficient / (mUoM x nominal cost)) with a standard error per cost
type, and whatever time-like residual remains is *earned* effort rather than labeled
effort. The current re-estimated spec is fully compatible: its time structure keeps its
identification; only the fixed money slots are replaced by estimated ones.

### Tier 1: already in the SrV data / pipeline, currently unused

1. **Abo ownership (`pt_abo_avail`).** Collected by SrV, converted, copied onto persons
   (`RunActivitySampling.copyAttributes`) -- yet the released population carries
   `ptAboAvail = never` for all 49,002 persons. Suspected flattening in the style of the
   car-availability episode (finding 3); recovering the variable is a one-afternoon
   archaeology with the highest identification payoff on this list. Identification: the
   marginal fare binds only for non-Abo travelers -- a within-sample price contrast of
   the full fare on thousands of persons. Caveat: Abo ownership is self-selected; model
   it as an *acceptance margin* (below) rather than conditioning on it naively.
2. **Car availability (`V_PKW_VERFUEG`).** In SrV, overwritten by
   `SetCarAvailabilityByAge` (finding 3). Restoring it (at least for estimation) puts the
   ownership margin -- the main real-world income-price channel for car -- back into the
   observable set.
3. **Coarse pt fare assignment from existing geography.** Whether a pt trip stays within
   Berlin AB or crosses into C / Brandenburg tariff territory is computable from the
   SrV zone data already in the pipeline (`SRV_ZONES`) plus a one-page price list.
   Kurzstrecke eligibility is derivable from the GTFS schedule already in the pipeline
   (stop-count along the routed pt alternative). Together this yields per-trip fare
   variation without any new data source; tariff-boundary discontinuities (similar
   time/distance, different fare) provide the cleanest slice.
4. **Ticket-type-per-trip, if surveyed.** SrV questionnaires have historically asked the
   ticket type used for pt trips; whether the converted tables retain it needs one look
   at the raw shared-svn tables. If present: direct per-trip fare payment data.
   (To verify -- flagged as background knowledge, not artifact fact.)
5. **Income x acceptance margins.** Income is in SrV; its explanatory power returns once
   the lumpy margins exist (Abo, car availability). The natural specification unifies
   both under the office's own ownership philosophy: a periodic fixed cost accepted or
   not (car ~14.30 EUR/day; Abo ~2.5 EUR/day) in exchange for zero/low marginal cost --
   the same mechanism on both competing modes, income-relevant on both.
6. **Speed-based fuel variation (weak, in-sample).** For fixed travel time, faster
   (peripheral/highway) car trips burn more fuel per hour than urban crawling; this
   residual variation is the only within-wave car money signal and is fragile
   (finding 1), but serves as a consistency check on any externally attached car cost
   estimate.

### Tier 2: requires attaching a new data source

7. **Full VBB tariff matrix** (relation-level fares, day/short-trip tickets) attached to
   the routed pt alternatives in `ComputeTripChoices` -- the completion of item 3.
8. **Parking management zones** (public Berlin GIS) with prices, joined to car
   alternatives by destination: spatially varying out-of-pocket car cost, the classical
   parking attribute this model never had. Confounded with density/pt supply; the
   per-trip pt LOS already in the choice data is the control.
9. **Additional SrV waves** (2013; 2023 when available) pooled with wave-specific real
   fuel prices and fares. The 2022-23 period (fuel spike, 9-EUR-Ticket, Deutschlandticket)
   is the largest transport price experiment in decades and directly measures the fare
   coefficient and its income gradient -- *if* the 2023 field dates cover it (to verify).
10. **Stated-preference data** -- the standard remedy, listed last deliberately: the
    tiers above may render it unnecessary, and RP-grounded coefficients are what the
    "classical dataset" ambition asks for.

Suggested order: 1 (archaeology) -> 3 (mechanical) -> 8 (GIS join) -> 7 -> 9 -> 10.
Expected outcome, from the literature on RP estimations with fare/parking variation:
out-of-pocket coefficients typically come out *stronger* than generic cost assumptions
-- i.e., this program is more likely to raise the model's price sensitivity further than
to lower it.
