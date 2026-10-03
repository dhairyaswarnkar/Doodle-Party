# ADR 0004: Java authority and scheduling

**Status:** Accepted

Java validates every role, word choice, guess, drawing chunk, and round number. It calculates scores and scheduled phase transitions. Public DTOs remove private tokens and words according to the player's role.

Browser-only scoring/timers could be changed by players or diverge between clients. Keeping outcomes on the server gives one decision source and enables injected-time unit tests. The browser only displays time and paints pending strokes for responsiveness. A fixed-delay scheduler is sufficient for prototype timers; actual event arrival can be delayed by server load or network latency.
