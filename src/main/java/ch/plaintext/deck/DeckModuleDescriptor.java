/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.modules.ModuleDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Meldet das Deck-Modul an der zentralen Modul-Verwaltung an (Karte 2162).
 *
 * <p>Ohne diese Bean liefe das Modul unverändert — es taucht nur nicht unter „Root | Module" auf,
 * liesse sich nicht abschalten, und {@code ModuleDataService.export} lehnte es mit „Unbekanntes
 * Modul" ab. {@code ModuleDescriptorVertragTest} verlangt sie deshalb für jedes Modul mit eigenen
 * Entities.</p>
 *
 * <p>{@link #entities()} muss <b>alle</b> Entities des Moduls nennen, nicht nur die wichtigste:
 * der Export liest genau diese Liste, und ein fehlender Eintrag fällt in keinem Test auf — der
 * Vertragstest prüft nur die Existenz des Descriptors.</p>
 */
@Component
public class DeckModuleDescriptor implements ModuleDescriptor {

    /** Modul-Id; derselbe Wert, den {@code plaintext.menu.module-roles} als Schlüssel verwendet. */
    public static final String MODUL_ID = "deck";

    @Override
    public String moduleId() {
        return MODUL_ID;
    }

    @Override
    public String displayName() {
        return "Deck";
    }

    @Override
    public List<Class<?>> entities() {
        return List.of(DeckKarte.class, DeckKommentar.class, DeckNachricht.class);
    }
}
