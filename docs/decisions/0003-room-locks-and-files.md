# ADR 0003: Room locks and JSON snapshots

**Status:** Accepted for one JVM

Serialize each room's actions with a Java monitor while allowing different rooms to execute concurrently. Persist dirty rooms to JSON files with atomic replacement on a fixed interval, and reload snapshots on startup. This uses Java-native storage without introducing JDBC or Hibernate.

A relational database with transactions would support stronger durability/querying but add persistence dependencies. Distributed actors/shared storage would support replicas but increase prototype scope. File snapshots are easy to inspect and package, but require a persistent disk, can lose recent updates on a hard crash, and do not coordinate multiple Java servers. The architecture explicitly targets a single instance.
