package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

class UtledNyeTilretteleggingerTjenesteTest {

    private final SvangerskapspengerRepository svangerskapspengerRepository = Mockito.mock(SvangerskapspengerRepository.class);
    private final UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste = Mockito
            .mock(UtledTilretteleggingerMedArbeidsgiverTjeneste.class);
    private final UtledNyeTilretteleggingerTjeneste utledNyeTilretteleggingerTjeneste = new UtledNyeTilretteleggingerTjeneste(
            svangerskapspengerRepository, utledTilretteleggingerMedArbeidsgiverTjeneste);

    @Test
    void skal_utlede_tilrettelegginger_med_og_uten_arbeidsgiver() {

        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().build();

        var tilretteleggingEntiteter = new ArrayList<SvpTilretteleggingEntitet>();
        tilretteleggingEntiteter.add(new SvpTilretteleggingEntitet.Builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123")).build());
        when(utledTilretteleggingerMedArbeidsgiverTjeneste.utled(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyList()))
                .thenReturn(tilretteleggingEntiteter);

        var grunnlagEntitet = new SvpGrunnlagEntitet.Builder()
                .medOpprinneligeTilrettelegginger(List.of(new SvpTilretteleggingEntitet.Builder().build()))
                .medBehandlingId(behandling.getId())
                .build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.of(grunnlagEntitet));

        // Act
        var result = utledNyeTilretteleggingerTjeneste.utled(behandling, skjæringstidspunkt);

        // Assert
        assertThat(result).hasSize(2);

    }

    @Test
    void skal_kaste_exception_når_ingen_grunnlag_blir_funnet() {
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> utledNyeTilretteleggingerTjeneste.utled(behandling, skjæringstidspunkt));
    }
}
