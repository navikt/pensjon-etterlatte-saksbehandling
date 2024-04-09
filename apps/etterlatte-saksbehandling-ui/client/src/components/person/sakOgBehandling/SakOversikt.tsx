import { BehandlingTable } from './BehandlingTable'
import styled from 'styled-components'
import { Container, FlexRow, GridContainer, SpaceChildren } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { Alert, BodyShort, Heading, HelpText, HStack, ReadMore, Tag } from '@navikt/ds-react'
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
import { OpprettSakModal } from '~components/person/sakOgBehandling/OpprettSakModal'
import { Buildings3Icon, LocationPinIcon } from '@navikt/aksel-icons'
import { SakOversiktHeader } from '~components/person/sakOgBehandling/SakOversiktHeader'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import { ForenkletOppgaverTable } from '~components/person/sakOgBehandling/ForenkletOppgaverTable'

const ETTERLATTEREFORM_DATO = '2024-01'

export const SakOversikt = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }) => {
  const [navkontorResult, hentNavkontor] = useApiCall(hentNavkontorForPerson)
  const [hentFlyktningStatus, hentFlyktning] = useApiCall(hentFlyktningStatusForSak)
  const [yrkesskadefordelStatus, hentYrkesskadefordel] = useApiCall(hentMigrertYrkesskadeFordel)

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentNavkontor(fnr)
      hentFlyktning(sakResult.data.sak.id)

      const migrertBehandling =
        sakResult.data.sak.sakType === SakType.BARNEPENSJON &&
        sakResult.data.behandlinger.find(
          (behandling) =>
            behandling.kilde === Vedtaksloesning.PESYS && behandling.virkningstidspunkt?.dato === ETTERLATTEREFORM_DATO
        )
      if (migrertBehandling) {
        hentYrkesskadefordel(migrertBehandling.id)
      }
    }
  }, [fnr, sakResult])

  return (
    <Container>
      {mapResult(sakResult, {
        pending: <Spinner visible label="Henter sak og behandlinger" />,
        error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
        success: ({ sak, behandlinger }) => (
          <SpaceChildren gap="2rem">
            <SakOversiktHeader sak={sak} navkontorResult={navkontorResult} />

            <Skille />

            <div>
              <Heading size="medium" spacing>
                Oppgaver
              </Heading>
              <ForenkletOppgaverTable sakId={sak.id} />
            </div>

            <div>
              <Heading size="medium" spacing>
                Behandlinger
              </Heading>
              <BehandlingTable sakOgBehandlinger={{ sak, behandlinger }} />
            </div>
          </SpaceChildren>
        ),
      })}
    </Container>
    // <GridContainer>
    //     (sakOgBehandlinger) => (
    //       <>
    //         <MainContent>
    //           {isSuccess(hentFlyktningStatus) && hentFlyktningStatus.data?.erFlyktning && (
    //             <>
    //               <FlexRow>
    //                 <Alert variant="info">
    //                   Saken er markert med flyktning i Pesys og første virkningstidspunkt var{' '}
    //                   {formaterStringDato(hentFlyktningStatus.data.virkningstidspunkt)}
    //                 </Alert>
    //               </FlexRow>
    //               <hr />
    //             </>
    //           )}
    //           {isFailureHandler({
    //             apiResult: hentFlyktningStatus,
    //             errorMessage: 'Klarte ikke hente informasjon om flyktningstatus',
    //           })}
    //           {mapResult(yrkesskadefordelStatus, {
    //             success: (yrkesskadefordelSvar) =>
    //               yrkesskadefordelSvar.migrertYrkesskadefordel && (
    //                 <>
    //                   <FlexRow>
    //                     <Alert variant="info">
    //                       Søker har yrkesskadefordel fra før 01.01.2024 og har rett til stønad til fylte 21 år.
    //                     </Alert>
    //                   </FlexRow>
    //                   <hr />
    //                 </>
    //               ),
    //             pending: <Spinner visible={true} label="Henter status yrkesskadefordel" />,
    //             error: () => <ApiErrorAlert>Klarte ikke hente informasjon om yrkesskadefordel</ApiErrorAlert>,
    //           })}
    //           <SelectWrapper>
    //             <BodyShort spacing>Denne saken tilhører enhet {sakOgBehandlinger.sak.enhet}.</BodyShort>
    //             {enhetErSkrivbar(sakOgBehandlinger.sak.enhet, innloggetSaksbehandler.skriveEnheter) && (
    //               <FlexRow>
    //                 <EndreEnhet sakId={sakOgBehandlinger.sak.id} />
    //                 <HelpText strategy="fixed">
    //                   Om saken tilhører en annen enhet enn den du jobber i, overfører du saken til riktig enhet ved å
    //                   klikke på denne knappen. Skriv først i kommentarfeltet i Sjekklisten inne i behandlingen hvilken
    //                   enhet saken er overført til og hvorfor. Gå så til saksoversikten, og klikk på knappen &rsquo;Endre
    //                   enhet&rsquo;, og overfør til riktig behandlende enhet.
    //                 </HelpText>
    //               </FlexRow>
    //             )}
    //           </SelectWrapper>
    //
    //           <KlageListe sakId={sakOgBehandlinger.sak.id} />
    //           <TilbakekrevingListe sakId={sakOgBehandlinger.sak.id} />
    //         </MainContent>
    //         <HendelseSidebar>
    //           <RelevanteHendelser sak={sakOgBehandlinger.sak} behandlingliste={sakOgBehandlinger.behandlinger} />
    //         </HendelseSidebar>
    //       </>
    //     )
    //   )}
    // </GridContainer>
  )
}

const Skille = styled.hr`
  border-color: var(--a-surface-active);
  width: 100%;
  align-self: center;
`

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
