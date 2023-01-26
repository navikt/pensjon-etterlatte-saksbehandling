import { Content } from '~shared/styled'
import { Button, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { Infoboks } from '~components/behandling/soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { useEffect, useState } from 'react'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { useAppSelector } from '~store/Store'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { Opphoersgrunn, OVERSETTELSER_OPPHOERSGRUNNER } from '~components/person/ManueltOpphoerModal'
import { hentManueltOpphoerDetaljer } from '~shared/api/behandling'
import Spinner from '~shared/Spinner'
import { fold3, useApiCall } from '~shared/hooks/useApiCall'
import { IDetaljertBehandling, Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
import { PersonHeader } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { Child } from '@navikt/ds-icons'
import differenceInYears from 'date-fns/differenceInYears'

interface Persongalleri {
  soeker: string
  avdoed?: string
}

export interface ManueltOpphoerDetaljer {
  id: string
  virkningstidspunkt?: Virkningstidspunkt
  persongalleri: Persongalleri
  opphoerAarsaker: Opphoersgrunn[]
  fritekstAarsak?: string
  andreBehandlinger: Partial<IDetaljertBehandling>[]
}

const useManueltOpphoerDetaljer = (behandlingId?: string) => {
  const [manueltOpphoerDetaljer, hent, reset] = useApiCall(hentManueltOpphoerDetaljer)
  useEffect(() => {
    const oppdater = async () => {
      if (behandlingId === undefined) {
        reset()
        return
      }
      hent(behandlingId)
    }
    void oppdater()
  }, [behandlingId])
  return manueltOpphoerDetaljer
}

export const ManueltOpphoerOversikt = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const [loadingBeregning, setLoadingBeregning] = useState(false)
  const behandlingRoutes = useBehandlingRoutes()
  const [feilmelding, setFeilmelding] = useState('')
  const manueltOpphoerDetaljer = useManueltOpphoerDetaljer(behandling.id)

  async function lagBeregningOgGaaVidere() {
    setLoadingBeregning(true)
    setFeilmelding('')
    try {
      await opprettEllerEndreBeregning(behandling.id)
      behandlingRoutes.next()
    } catch (e) {
      setFeilmelding('Kunne ikke opprette beregning for behandlingen. Prøv igjen senere.')
    } finally {
      setLoadingBeregning(false)
    }
  }
  const soeker = behandling.søker

  return (
    <Content>
      <MainSection>
        <SectionSpacing>
          <Heading size="large" level="1">
            Manuelt opphør
          </Heading>
          <Infoboks>
            Det har kommet inn nye endringer på saken som gjør at den ikke kan behandles i Doffen. Saken må opprettes
            manuelt i PeSys og deretter opphøres i nytt system. Se rutiner for å opprette sak i PeSys
          </Infoboks>
        </SectionSpacing>
        <SectionSpacing>
          <Heading size="medium" level="2">
            Om saken
          </Heading>
          {soeker ? (
            <PersonHeader>
              <span className="icon">
                <Child />
              </span>
              {`${soeker.fornavn} ${soeker.etternavn}`}
              <span className="personRolle"> ({differenceInYears(new Date(), new Date(soeker.foedselsdato))} år)</span>
            </PersonHeader>
          ) : (
            <p>Kunne ikke hente ut detaljer om søker</p>
          )}
          {fold3(
            manueltOpphoerDetaljer,
            () => (
              <Spinner visible label="Henter detaljer om det manuelle opphøret" />
            ),
            () => (
              <MainSection>
                <Feilmelding>Kunne ikke hente ut detaljer om manuelt opphør</Feilmelding>
              </MainSection>
            ),
            (manueltOpphoerDetaljer) => (
              <>
                <Heading size="small" level="3">
                  Grunner til manuelt opphør
                </Heading>
                <OppsummeringListe>
                  {manueltOpphoerDetaljer.opphoerAarsaker.map((opphoersgrunn) => (
                    <li key={opphoersgrunn}>{OVERSETTELSER_OPPHOERSGRUNNER[opphoersgrunn]}</li>
                  ))}
                  {manueltOpphoerDetaljer.fritekstAarsak?.length ? (
                    <li>Annet: {manueltOpphoerDetaljer.fritekstAarsak}</li>
                  ) : null}
                </OppsummeringListe>
                <Heading size="small" level="3">
                  Behandlinger som blir opphørt og må behandles på nytt i Pesys:
                </Heading>
                <OppsummeringListe>
                  {manueltOpphoerDetaljer.andreBehandlinger.map((behandling) => (
                    <li key={behandling.id}>
                      {formaterEnumTilLesbarString(behandling.behandlingType ?? '')} med virkningstidspunkt{' '}
                      {formaterStringDato(behandling.virkningstidspunkt!.dato)}
                    </li>
                  ))}
                </OppsummeringListe>
                <p>Virkningstidspunkt for opphøret: {manueltOpphoerDetaljer.virkningstidspunkt?.dato}</p>
              </>
            )
          )}
        </SectionSpacing>
      </MainSection>
      <BehandlingHandlingKnapper>
        <Button variant="primary" size="medium" loading={loadingBeregning} onClick={lagBeregningOgGaaVidere}>
          Se over beregning
        </Button>
      </BehandlingHandlingKnapper>
      {feilmelding.length > 0 && <Feilmelding>{feilmelding}</Feilmelding>}
    </Content>
  )
}

const Feilmelding = styled.p`
  color: var(--navds-semantic-color-feedback-danger-text);
`

const MainSection = styled.main`
  padding: 4em;
`

const SectionSpacing = styled.section`
  padding: 2em 0;
`

const OppsummeringListe = styled.ul`
  margin: 0 0 2em 2em;
`
