#!/usr/bin/env python

import json
import os
import time
from dataclasses import dataclass
from random import Random

import argparse

import jax.numpy as jnp
import optax
from jax import grad, random
from requests import post
from tqdm import trange

from gen_code import speedRelative_priority as p
from gen_code import speedRelative_right_before_left as rbl
from gen_code import speedRelative_traffic_light as tl

URL = "http://localhost:9090"


def as_list(array):
    return [float(x) for x in array]


def req(priority, rbl, traffic_light):
    req = {
        "priority": as_list(priority),
        "rbl": as_list(rbl),
        "traffic_light": as_list(traffic_light),
    }

    res = post(URL, json=req)
    return req, res.json()


@dataclass
class Model:
    name: str
    module: object
    optimizer: optax.adam
    opt_state: object
    params: jnp.array
    loss: callable


if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Optimize parameters for free-speed model. Server must be running before starting")
    parser.add_argument("--steps", type=int, help="Number of training steps", default="1000")
    parser.add_argument("--resume", help="File with parameters to to resume", default=None)

    args = parser.parse_args()

    batch_size = 128
    batches = 5
    learning_rate = 1e-4

    models = {}
    resume = {}

    if args.resume:
        print("Resuming from", args.resume)
        with open(args.resume) as f:
            resume = json.load(f)

    for (module, name) in ((p, "priority"), (tl, "traffic_light"), (rbl, "rbl")):
        schedule = optax.exponential_decay(
            init_value=learning_rate, decay_rate=0.8,
            # Every 5% steps, decay 0.8
            transition_steps=int(batches * args.steps / 20), transition_begin=int(0.35 * args.steps * batches),
            staircase=False
        )

        optimizer = optax.adam(schedule)
        params = jnp.array(resume[name] if name in resume else module.params)
        opt_state = optimizer.init(params)

        models[name] = Model(
            name, module, optimizer, opt_state, params, grad(module.batch_loss)
        )

    r = Random(42)

    out = os.path.join("output_params", time.strftime("%Y%m%d-%H%M"))
    os.makedirs(out, exist_ok=True)

    print("Writing to", out)

    with trange(args.steps) as outer:

        # A simple update loop.
        for i in outer:
            params, result = req(models["priority"].params, models["rbl"].params, models["traffic_light"].params)

            name = "it%03d_mse_%.2f_rmse_%.2f.json" % (i, result["mse"], result["rmse"])

            with open(os.path.join(out, name), "w") as f:
                json.dump(params, f)

            outer.set_postfix(mse=result["mse"], rmse=result["rmse"])

            for k, m in models.items():

                xs = [d["x"] for d in result[k]]
                xs = jnp.array(xs)

                ys = [d["yTrue"] for d in result[k]]
                ys = jnp.array(ys)

                for j in range(batches):
                    key = random.PRNGKey(r.getrandbits(31))

                    idx = random.choice(key, jnp.arange(0, xs.shape[0]), shape=(batch_size,))

                    grads = m.loss(m.params, xs[idx], ys[idx])

                    updates, m.opt_state = m.optimizer.update(grads, m.opt_state)
                    m.params = optax.apply_updates(m.params, updates)
