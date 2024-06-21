import React, { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { hentFlyktningStatusForSak, hentNavkontorForPerson } from '~shared/api/sak'
import { Alert, BodyShort, Heading, HStack, Loader, ReadMore, Tag, VStack } from '@navikt/ds-react'
import { tagColors } from '~shared/Tags'
import { formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
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
import styled from 'styled-components'
import { SakTypeTag } from '~shared/tags/SakTypeTag'

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
    <VStack gap="4">
      <Heading size="medium">Saksnummer {sak.id}</Heading>
      <VStack gap="4" align="start">
        <SakTypeTag sakType={sak.sakType} />

        {!!sak.utlandstilknytning?.type && (
          <Tag variant={tagColors[sak.utlandstilknytning?.type]}>
            {formaterEnumTilLesbarString(sak.utlandstilknytning?.type)}
          </Tag>
        )}
        <SakStatus sakId={sak.id} />
      </VStack>

      <HStack gap="4" wrap={false}>
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

      <HStack gap="4" wrap={false}>
        <Buildings3Icon aria-hidden width="1.75rem" height="1.75rem" />
        <BodyShort>Denne saken tilhører enhet {sak.enhet}</BodyShort>
      </HStack>
      {enhetErSkrivbar(sak.enhet, innloggetSaksbehandler.skriveEnheter) && <EndreEnhet sakId={sak.id} />}
      {enhetErSkrivbar(sak.enhet, innloggetSaksbehandler.skriveEnheter) && (
        <ReadMore header="Ønsker du å bytte enhet?">
          <ol>
            <li>
              Skriv i kommentarfeltet i sjekklisten inne i behandlingen hvilken enhet saken overføres til og hvorfor.
            </li>
            <li>Deretter går du til saksoversikten og klikker på knappen ovenfor for å endre enhet.</li>
            <li>Overfør til riktig behandlende enhet.</li>
          </ol>
        </ReadMore>
      )}

      {mapResult(flyktningResult, {
        success: (data) =>
          !!data?.erFlyktning && (
            <>
              <HorisontaltSkille />
              <div>
                <Alert variant="info" size="small" inline>
                  Saken er markert med flyktning i Pesys og første virkningstidspunkt var{' '}
                  {formaterStringDato(data.virkningstidspunkt)}
                </Alert>
              </div>
            </>
          ),
      })}

      {mapResult(yrkesskadefordelResult, {
        success: (data) =>
          data.migrertYrkesskadefordel && (
            <>
              <HorisontaltSkille />
              <div>
                <Alert variant="info" size="small" inline>
                  Søker har yrkesskadefordel fra før 01.01.2024 og har rett til stønad til fylte 21 år.
                </Alert>
              </div>
            </>
          ),
      })}
    </VStack>
  )
}

const HorisontaltSkille = styled.hr`
  border-color: var(--a-surface-active);
  width: 100%;
`
