import { Behandlingsliste } from './Behandlingsliste'
import styled from 'styled-components'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { Alert, BodyShort, Heading, HelpText, HStack, Tag } from '@navikt/ds-react'
import { formaterEnumTilLesbarString, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { KlageListe } from '~components/person/KlageListe'
import { tagColors } from '~shared/Tags'
import { SakMedBehandlinger } from '~components/person/typer'
import { isSuccess, mapApiResult, mapResult, Result } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { EndreEnhet } from '~components/person/EndreEnhet'
import { hentFlyktningStatusForSak, hentNavkontorForPerson } from '~shared/api/sak'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppSelector } from '~store/Store'
import { TilbakekrevingListe } from '~components/person/TilbakekrevingListe'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { SakType } from '~shared/types/sak'
import { hentMigrertYrkesskadeFordel } from '~shared/api/vilkaarsvurdering'
import { Vedtaksloesning } from '~shared/types/IDetaljertBehandling'

const ETTERLATTEREFORM_DATO = '2024-01'

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
  const [hentNavkontorStatus, hentNavkontor] = useApiCall(hentNavkontorForPerson)
  const [hentFlyktningStatus, hentFlyktning] = useApiCall(hentFlyktningStatusForSak)
  const [yrkesskadefordelStatus, hentYrkesskadefordel] = useApiCall(hentMigrertYrkesskadeFordel)

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentNavkontor(fnr)
      hentFlyktning(sakStatus.data.sak.id)

      const migrertBehandling =
        sakStatus.data.sak.sakType === SakType.BARNEPENSJON &&
        sakStatus.data.behandlinger.find(
          (behandling) =>
            behandling.kilde === Vedtaksloesning.PESYS && behandling.virkningstidspunkt?.dato === ETTERLATTEREFORM_DATO
        )
      if (migrertBehandling) {
        hentYrkesskadefordel(migrertBehandling.id)
      }
    }
  }, [fnr, sakResult])

  return (
    <GridContainer>
      {mapApiResult(
        sakResult,
        <Spinner visible={true} label="Henter sak og behandlinger" />,
        (error) => (
          <MainContent>
            {error.status === 404 ? (
              <ApiWarningAlert>{error.detail}</ApiWarningAlert>
            ) : (
              <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
            )}
          </MainContent>
        ),
        (sakOgBehandlinger) => (
          <>
            <MainContent>
              <Heading size="medium" spacing>
                <HStack gap="2">
                  Saknummer {sakOgBehandlinger.sak.id}{' '}
                  <Tag variant="success" size="medium">
                    {formaterSakstype(sakOgBehandlinger.sak.sakType)}
                  </Tag>
                  {sakOgBehandlinger.sak.utlandstilknytning && (
                    <Tag variant={tagColors[sakOgBehandlinger.sak.utlandstilknytning.type]} size="medium">
                      {formaterEnumTilLesbarString(sakOgBehandlinger.sak.utlandstilknytning.type)}
                    </Tag>
                  )}
                </HStack>
              </Heading>
              {isSuccess(hentFlyktningStatus) && hentFlyktningStatus.data?.erFlyktning && (
                <>
                  <FlexRow>
                    <Alert variant="info">
                      Saken er markert med flyktning i Pesys og første virkningstidspunkt var{' '}
                      {formaterStringDato(hentFlyktningStatus.data.virkningstidspunkt)}
                    </Alert>
                  </FlexRow>
                  <hr />
                </>
              )}
              {isFailureHandler({
                apiResult: hentFlyktningStatus,
                errorMessage: 'Klarte ikke hente informasjon om flyktningstatus',
              })}
              {mapResult(yrkesskadefordelStatus, {
                success: (yrkesskadefordelSvar) =>
                  yrkesskadefordelSvar.migrertYrkesskadefordel && (
                    <>
                      <FlexRow>
                        <Alert variant="info">
                          Søker har yrkesskadefordel fra før 01.01.2024 og har rett til stønad til fylte 21 år.
                        </Alert>
                      </FlexRow>
                      <hr />
                    </>
                  ),
                pending: <Spinner visible={true} label="Henter status yrkesskadefordel" />,
                error: () => <ApiErrorAlert>Klarte ikke hente informasjon om yrkesskadefordel</ApiErrorAlert>,
              })}
              {mapApiResult(
                hentNavkontorStatus,
                <Spinner visible label="Laster navkontor ..." />,
                () => (
                  <ApiErrorAlert>Kunne ikke hente navkontor</ApiErrorAlert>
                ),
                (navkontor) => (
                  <BodyShort spacing>Navkontor er: {navkontor.navn}</BodyShort>
                )
              )}
              <SelectWrapper>
                <BodyShort spacing>Denne saken tilhører enhet {sakOgBehandlinger.sak.enhet}.</BodyShort>
                {enhetErSkrivbar(sakOgBehandlinger.sak.enhet, innloggetSaksbehandler.skriveEnheter) && (
                  <FlexRow>
                    <EndreEnhet sakId={sakOgBehandlinger.sak.id} />
                    <HelpText strategy="fixed">
                      Om saken tilhører en annen enhet enn den du jobber i, overfører du saken til riktig enhet ved å
                      klikke på denne knappen. Skriv først i kommentarfeltet i Sjekklisten inne i behandlingen hvilken
                      enhet saken er overført til og hvorfor. Gå så til saksoversikten, og klikk på knappen &rsquo;Endre
                      enhet&rsquo;, og overfør til riktig behandlende enhet.
                    </HelpText>
                  </FlexRow>
                )}
              </SelectWrapper>
              <hr />
              <Behandlingsliste sakOgBehandlinger={sakOgBehandlinger} />

              <KlageListe sakId={sakOgBehandlinger.sak.id} />
              <TilbakekrevingListe sakId={sakOgBehandlinger.sak.id} />
            </MainContent>
            <HendelseSidebar>
              <RelevanteHendelser sak={sakOgBehandlinger.sak} behandlingliste={sakOgBehandlinger.behandlinger} />
            </HendelseSidebar>
          </>
        )
      )}
    </GridContainer>
  )
}

const MainContent = styled.div`
  flex: 1 0 auto;
  margin: 3em 1em;
`

const HendelseSidebar = styled.div`
  min-width: 40rem;
  border-left: 1px solid gray;
  padding: 3em 2rem;
  margin: 0 1em;
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    padding: 0.6em;
  }
`
const SelectWrapper = styled.div`
  margin: 0 0 0 0;
  max-width: 20rem;
`
