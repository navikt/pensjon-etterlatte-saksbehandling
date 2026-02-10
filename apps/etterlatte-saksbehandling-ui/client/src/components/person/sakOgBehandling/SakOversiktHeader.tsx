import React, { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { hentFlyktningStatusForSak, hentNavkontorForPerson } from '~shared/api/sak'
import { Alert, BodyShort, Box, Heading, HStack, Loader, ReadMore, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Buildings3Icon, LocationPinIcon } from '@navikt/aksel-icons'
import { EndreEnhet } from '~components/person/sakOgBehandling/EndreEnhet'
import { ISakMedUtlandstilknytning, SakType } from '~shared/types/sak'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakStatus } from '~components/person/sakOgBehandling/SakStatus'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { IBehandlingsammendrag } from '~components/person/typer'
import { hentMigrertYrkesskadeFordel } from '~shared/api/vilkaarsvurdering'
import { Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { UtenlandstilknytningTypeTag } from '~shared/tags/UtenlandstilknytningTypeTag'
import { OpprettSaksgrunnlag } from '~components/person/sakOgBehandling/OpprettSaksgrunnlag'
import { Sakshistorikk } from '~components/person/sakOgBehandling/Sakshistorikk'
import { ENHETER } from '~shared/types/Enhet'
import { ClickEvent, trackClick } from '~utils/analytics'
import { AdressebeskyttelseGraderingTag } from '~shared/tags/AdressebeskyttelseGraderingTag'

const ETTERLATTEREFORM_DATO = '2024-01'

interface Props {
  sak: ISakMedUtlandstilknytning
  behandlinger: IBehandlingsammendrag[]
  fnr: string
}

export const SakOversiktHeader = ({ sak, behandlinger, fnr }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [navkontorResult, hentNavkontor] = useApiCall(hentNavkontorForPerson)
  const [yrkesskadefordelResult, hentYrkesskadefordel] = useApiCall(hentMigrertYrkesskadeFordel)
  const [flyktningResult, hentFlyktning] = useApiCall(hentFlyktningStatusForSak)

  useEffect(() => {
    hentNavkontor(fnr)
    hentFlyktning(sak.id)

    const migrertBehandling =
      sak.sakType === SakType.BARNEPENSJON &&
      behandlinger.find(
        (behandling) =>
          behandling.kilde === Vedtaksloesning.PESYS && behandling.virkningstidspunkt?.dato === ETTERLATTEREFORM_DATO
      )
    if (migrertBehandling) {
      hentYrkesskadefordel(migrertBehandling.id)
    }
  }, [])

  return (
    <VStack gap="space-4">
      <Heading size="medium">Saksnummer {sak.id}</Heading>
      <VStack gap="space-4" align="start">
        <SakTypeTag sakType={sak.sakType} />

        <UtenlandstilknytningTypeTag utenlandstilknytningType={sak.utlandstilknytning?.type} />

        <SakStatus sakId={sak.id} />

        {!!sak.adressebeskyttelse && <AdressebeskyttelseGraderingTag adressebeskyttelse={sak.adressebeskyttelse} />}
      </VStack>

      <HStack gap="space-4" wrap={false} align="center">
        <LocationPinIcon aria-hidden width="1.75rem" height="1.75rem" />
        <BodyShort>
          Navkontor:{' '}
          {mapResult(navkontorResult, {
            pending: <Loader />,
            error: <>Kunne ikke hente kontor</>,
            success: (navKontor) => <>{navKontor.navn}</>,
          })}
        </BodyShort>
      </HStack>
      <HStack gap="space-4" wrap={false} align="center">
        <Buildings3Icon aria-hidden width="1.75rem" height="1.75rem" />
        <BodyShort>Enhet: {ENHETER[sak.enhet] ?? sak.enhet}</BodyShort>
        {enhetErSkrivbar(sak.enhet, innloggetSaksbehandler.skriveEnheter) && (
          <EndreEnhet sakId={sak.id} gjeldendeEnhet={sak.enhet} />
        )}
      </HStack>

      <ReadMore header="Historikk" onClick={() => trackClick(ClickEvent.VIS_SAKSHISTORIKK)}>
        <Sakshistorikk sakId={sak.id} />
      </ReadMore>

      {mapResult(flyktningResult, {
        success: (data) =>
          !!data?.erFlyktning && (
            <>
              <Box paddingBlock="space-2" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
                <Alert variant="info" size="small" inline>
                  Saken er markert med flyktning i Pesys og første virkningstidspunkt var{' '}
                  {formaterDato(data.virkningstidspunkt)}
                </Alert>
              </Box>
            </>
          ),
      })}
      {mapResult(yrkesskadefordelResult, {
        success: (data) =>
          data.migrertYrkesskadefordel && (
            <>
              <Box paddingBlock="space-2" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
                <Alert variant="info" size="small" inline>
                  Søker har yrkesskadefordel fra før 01.01.2024 og har rett til stønad til fylte 21 år.
                </Alert>
              </Box>
            </>
          ),
      })}

      {!behandlinger.length && <OpprettSaksgrunnlag sak={sak} />}
    </VStack>
  )
}
