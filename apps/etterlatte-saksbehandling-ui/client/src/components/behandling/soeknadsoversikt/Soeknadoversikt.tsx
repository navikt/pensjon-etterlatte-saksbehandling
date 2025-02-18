import { Familieforhold } from './familieforhold/Familieforhold'
import { Box, Heading, HStack } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
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
  beskrivelseVirkningstidspunkt,
  hjemlerVirkningstidspunkt,
} from '~components/behandling/virkningstidspunkt/utils'
import Virkningstidspunkt from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { GrunnlagForVirkningstidspunkt } from '~components/behandling/soeknadsoversikt/GrunnlagForVirkningstidspunkt'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { ViderefoereOpphoer } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoer'
import { TidligereFamiliepleier } from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleier'
import SluttBehandlingOmgjoering from '~components/behandling/soeknadsoversikt/SluttbehandlingOmgjoering'
import { SoeknadInformasjon } from '~components/behandling/soeknadsoversikt/SoeknadInformasjon'
import { ForbedretFamilieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/ForbedretFamilieforhold'

export const Soeknadsoversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const personopplysninger = usePersonopplysninger()
  const erBosattUtland = behandling.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND
  const erForeldreloes = !!personopplysninger?.avdoede?.length && personopplysninger?.avdoede.length >= 2

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Søknadsoversikt
        </Heading>
      </Box>

      <Box paddingInline="20 0" paddingBlock="8 8" maxWidth="70rem">
        <SoeknadInformasjon behandling={behandling} />
      </Box>

      <Box paddingBlock="4 0" borderWidth="0 0 1 0" borderColor="border-subtle">
        <ForbedretFamilieforhold
          behandling={behandling}
          personopplysninger={personopplysninger}
          redigerbar={redigerbar}
        />
      </Box>

      <Box paddingBlock="4 0" borderWidth="0 0 1 0" borderColor="border-subtle">
        <Familieforhold behandling={behandling} personopplysninger={personopplysninger} redigerbar={redigerbar} />
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

        {behandling.erSluttbehandling && (
          <SluttBehandlingOmgjoering behandlingId={behandling.id} redigerbar={redigerbar} />
        )}

        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == SakType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={redigerbar}
                soeker={personopplysninger?.soeker}
                gjenlevendeForelder={personopplysninger?.gjenlevende?.find((po) => po)}
                behandlingId={behandling.id}
                innsender={personopplysninger?.innsender?.opplysning}
                avdoed={personopplysninger?.avdoede.map((avdoed) => avdoed.opplysning.foedselsnummer) || []}
              />
            )}
            <Virkningstidspunkt
              erBosattUtland={erBosattUtland}
              redigerbar={redigerbar}
              behandling={behandling}
              hjemler={hjemlerVirkningstidspunkt(behandling.sakType, erBosattUtland)}
              beskrivelse={beskrivelseVirkningstidspunkt(behandling.sakType, erBosattUtland, erForeldreloes)}
            >
              <GrunnlagForVirkningstidspunkt />
            </Virkningstidspunkt>
            <ViderefoereOpphoer behandling={behandling} redigerbar={redigerbar} />
            <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
            {behandling.sakType == SakType.OMSTILLINGSSTOENAD && (
              <TidligereFamiliepleier behandling={behandling} redigerbar={redigerbar} />
            )}
          </>
        )}
        <SkalViseBosattUtland behandling={behandling} redigerbar={redigerbar} />
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
