# Zombie Warhorses

Tamed horses **fight alongside you**. If zombies kill one, it rises as a hostile zombie horse —
which you can tame back with a golden carrot, but never restore.

Minecraft 1.20.1 / Forge. MIT.

## The loop

```
wild horse ──(tame)──→ warhorse          fights nearby monsters while its owner is close
                │
                └──(killed by a zombie)──→ hostile zombie horse   attacks every player;
                                            │                     zombies come to ride it
                                            └──(golden carrot, vanilla-style taming)──→ calm mount
```

There is **no cure** — a zombie horse never becomes a normal horse again. Losing a horse to zombies
is permanent, and the thing that killed it is now a mount for the horde.

## Behaviour details

**Warhorses only fight while their owner is nearby.** Vanilla tamed wolves retaliate for their
owner; a warhorse picks its own targets, which would make an unattended or logged-out horse charge
into a mob and die. Gating on owner proximity keeps it a companion rather than a suicide.

**Tamed horses stop fleeing.** The vanilla panic goal is replaced with one that only applies to
wild horses.

**Horses have no `ATTACK_DAMAGE` attribute**, so `doHurtTarget()` does nothing. The melee goal
reuses vanilla pathfinding but applies a fixed damage value on hit (4.0 for horses, 5.0 for zombie
horses).

**Zombie riders are found by the horse, not the zombies.** An earlier design had every zombie scan
its surroundings each tick for a horse — that was a measurable cause of server stalls. It is
inverted: only riderless, untamed zombie horses scan, and only every *N* ticks. Zombie horses are
rare (they only come from conversions), so the cost went from *O(zombies)* to effectively nothing.

## Armour

Zombie horses have no armour slot in vanilla. On conversion, the saddle and horse armour are
stashed into the entity's NBT. The saddle is re-fitted when you tame it; **the armour stays in NBT
and is applied as an attribute modifier, with a client-side render layer so you can still see it**.

Server-side `persistentData` is not synced to clients, so a small packet carries the worn armour to
the client cache purely for rendering.

## Optional

Pairs with a `horse_stats` datapack: a tamed zombie horse then becomes a genuinely fast special
mount, which is the reward for taking one alive.

## Build

```bash
export JAVA_HOME=/path/to/jdk-17      # JDK 17 ならどれでもよい
./gradlew build --no-daemon
# → build/libs/zombie_warhorses-<version>.jar
```

## Contributing

Issues and pull requests are welcome. The damage values, owner range and scan interval are
constants in `ModEvents` and the goal classes rather than config keys — if you want them
configurable, that is a reasonable request.
