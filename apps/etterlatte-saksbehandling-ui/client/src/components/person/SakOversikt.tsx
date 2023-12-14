import { Behandlingsliste } from './Behandlingsliste'
import styled from 'styled-components'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { Alert, BodyShort, Heading, HelpText, HStack, Tag } from '@navikt/ds-react'
import { formaterEnumTilLesbarString, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE, OpprettKlage } from '~components/person/OpprettKlage'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { KlageListe } from '~components/person/KlageListe'
import { tagColors } from '~shared/Tags'
import { SakMedBehandlinger } from '~components/person/typer'
import { isSuccess, mapApiResult, Result } from '~shared/api/apiUtils'
import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { EndreEnhet } from '~components/person/EndreEnhet'
import { hentNavkontorForPerson } from '~shared/api/sak'
import { hentFlyktningStatusForSak } from '~shared/api/sak'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const SakOversikt = ({ sakStatus, fnr }: { sakStatus: Result<SakMedBehandlinger>; fnr: string }) => {
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)
  const [hentNavkontorStatus, hentNavkontor] = useApiCall(hentNavkontorForPerson)
  const [hentFlyktningStatus, hentFlyktning] = useApiCall(hentFlyktningStatusForSak)

  useEffect(() => {
    hentNavkontor(fnr)
    if (isSuccess(sakStatus)) {
      hentFlyktning(sakStatus.data.sak.id)
    }
  }, [fnr, sakStatus])

  return (
    <GridContainer>
      {mapApiResult(
        sakStatus,
        <Spinner visible={true} label="Henter sak og behandlinger" />,
        (error) => (
          <Alert variant="error">{JSON.stringify(error)}</Alert>
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

                <FlexRow justify="right">
                  <OpprettKlage sakId={sakOgBehandlinger.sak.id} />
                </FlexRow>
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
                <BodyShort>
                  Denne saken tilhører enhet {sakOgBehandlinger.sak.enhet}.
                  <FlexRow>
                    <EndreEnhet sakId={sakOgBehandlinger.sak.id} />
                    <HelpText strategy="fixed">
                      Om saken tilhører en annen enhet enn den du jobber i, overfører du saken til riktig enhet ved å
                      klikke på denne knappen. Skriv først i kommentarfeltet i Sjekklisten inne i behandlingen hvilken
                      enhet saken er overført til og hvorfor. Gå så til saksoversikten, og klikk på knappen &rsquo;Endre
                      enhet&rsquo;, og overfør til riktig behandlende enhet.
                    </HelpText>
                  </FlexRow>
                </BodyShort>
              </SelectWrapper>
              <hr />
              <Behandlingsliste behandlinger={sakOgBehandlinger.behandlinger} sakId={sakOgBehandlinger.sak.id} />

              {kanBrukeKlage ? <KlageListe sakId={sakOgBehandlinger.sak.id} /> : null}
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
