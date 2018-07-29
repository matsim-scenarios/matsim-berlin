# Notes

### 2018-07-16 mode choice experiments

I always got nearly 100% PT.  After some debugging, find following:
* The two car trips are, w/o ttime,
```
-1.5 - 0.2/km * 25km - 1.5 - 0.2/km * 65km = -21
```
* In contrast, the two PT trips are, w/o ttime,
```
-0.6 - 0.6 = - 1.2
```
* These two are also what is found in `experienced_plans_scores.txt` since the direct marginal (dis)utility of ttime is zero.  
* Time comes in via the marginal utility of time as a resource.  beta_perf is 6; I will assume that I can use that as approximation.
* I get, with car tttime of overall 55min, -5.5.
* So the tipping point PT ttime comes from `(21+5.5)-1.2=25.3`.  Divide this by mUoTaaR=6 and obtain approx 6h20min.

Let's try for much shorter commute.
* car:
```
-1.5 - 0.2/km * 10km - 1.5 - 0.2/km*10km = -7
```
* Let's assume "km=min".  Thus add 20min, i.e. 2, results in `-9`.
* PT again `-1.2`, times two is `-2.4`, difference to car thus is `6.6`.
* 
