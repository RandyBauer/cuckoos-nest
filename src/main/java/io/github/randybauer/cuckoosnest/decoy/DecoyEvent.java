package io.github.randybauer.cuckoosnest.decoy;

/** Published when a decoy is created or retired, so other parts of the app can react. */
public sealed interface DecoyEvent {

    Decoy decoy();

    record Created(Decoy decoy) implements DecoyEvent {}

    record Retired(Decoy decoy) implements DecoyEvent {}
}
