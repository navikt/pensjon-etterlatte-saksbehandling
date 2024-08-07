import { Familieforhold } from './familieforhold/Familieforhold'
import { Box, Heading, HStack } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErRedigerbar, soeknadsoversiktErFerdigUtfylt } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { Utlandstilknytning } from '~components/behandling/soeknadsoversikt/utlandstilknytning/Utlandstilknytning'
import { SakType } from '~shared/types/sak'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/Start'
import { IDetaljertBehandling, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BoddEllerArbeidetUtlandet } from '~components/behandling/soeknadsoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetUtlandet'
import OppdaterGrunnlagModal from '~components/behandling/handlinger/OppdaterGrunnlagModal'
import { SkalViseBosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/SkalViseBosattUtland'
import {
  BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER,
  BP_FOERSTEGANGSBEHANDLING_HJEMLER,
  BP_FORELDRELOES_PAA_GAMMELT_REGELVERK_BEHANDLES_I_PESYS_BESKRIVELSE,
  OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE,
  OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER,
  OMS_FOERSTEGANGSBEHANDLING_HJEMLER,
} from '~components/behandling/virkningstidspunkt/utils'
import Virkningstidspunkt from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { GrunnlagForVirkningstidspunkt } from '~components/behandling/soeknadsoversikt/GrunnlagForVirkningstidspunkt'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { ViderefoereOpphoer } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoer'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const personopplysninger = usePersonopplysninger()
  const erBosattUtland = behandling.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND
  const erForeldreloes = (personopplysninger?.avdoede || []).length >= 2

  const hjemlerVirkningstidspunkt = (sakType: SakType, erBosattUtland: boolean) => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return erBosattUtland ? BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER : BP_FOERSTEGANGSBEHANDLING_HJEMLER
      case SakType.OMSTILLINGSSTOENAD:
        return erBosattUtland ? OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER : OMS_FOERSTEGANGSBEHANDLING_HJEMLER
    }
  }

  const beskrivelseVirkningstidspunkt = (sakType: SakType, erBosattUtland: boolean, erForeldreloes: boolean) => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return bpBeskrivelseVirkningstidspunkt(erBosattUtland, erForeldreloes)
      case SakType.OMSTILLINGSSTOENAD:
        return omsBeskrivelseVirkningstidspunkt(erBosattUtland)
    }
  }

  function bpBeskrivelseVirkningstidspunkt(erBosattUtland: boolean, erForeldreloes: boolean) {
    const standard = erBosattUtland
      ? BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE
      : BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE
    return erForeldreloes ? standard + BP_FORELDRELOES_PAA_GAMMELT_REGELVERK_BEHANDLES_I_PESYS_BESKRIVELSE : standard
  }

  function omsBeskrivelseVirkningstidspunkt(erBosattUtland: boolean) {
    return erBosattUtland
      ? OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE
      : OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE
  }

  const viderefoertOpphoerEnabled = useFeatureEnabledMedDefault('viderefoer-opphoer', false)

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Søknadsoversikt
        </Heading>
        {behandling.soeknadMottattDato && <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />}
      </Box>
      <Box paddingBlock="8" paddingInline="16 8">
        {redigerbar && (
          <HStack justify="end">
            <OppdaterGrunnlagModal
              behandlingId={behandling.id}
              behandlingStatus={behandling.status}
              enhetId={behandling.sakEnhetId}
            />
          </HStack>
        )}
        <Utlandstilknytning behandling={behandling} redigerbar={redigerbar} />
        {personopplysninger && (
          <OversiktGyldigFramsatt behandling={behandling} personopplysninger={personopplysninger} />
        )}
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == SakType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={redigerbar}
                soeker={personopplysninger?.soeker?.opplysning}
                gjenlevendeForelder={personopplysninger?.gjenlevende?.find((po) => po)}
                behandlingId={behandling.id}
              />
            )}
            <Virkningstidspunkt
              erBosattUtland={erBosattUtland}
              redigerbar={redigerbar}
              behandling={behandling}
              hjemler={hjemlerVirkningstidspunkt(behandling.sakType, erBosattUtland)}
              beskrivelse={beskrivelseVirkningstidspunkt(behandling.sakType, erBosattUtland, erForeldreloes)}
            >
              {{ info: <GrunnlagForVirkningstidspunkt /> }}
            </Virkningstidspunkt>{' '}
            {viderefoertOpphoerEnabled && <ViderefoereOpphoer behandling={behandling} redigerbar={redigerbar} />}
            <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
          </>
        )}
        <SkalViseBosattUtland behandling={behandling} redigerbar={redigerbar} />
      </Box>

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <Familieforhold behandling={behandling} personopplysninger={personopplysninger} redigerbar={redigerbar} />
      </Box>
      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            {soeknadsoversiktErFerdigUtfylt(behandling) && <Start disabled={!erGyldigFremsatt} />}
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}
