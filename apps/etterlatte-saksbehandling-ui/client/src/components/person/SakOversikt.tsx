import { useEffect } from 'react'
import { Behandlingsliste } from './Behandlingsliste'
import styled from 'styled-components'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyShort, Heading, Link, Tag } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { formaterEnumTilLesbarString, formaterSakstype } from '~utils/formattering'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE, OpprettKlage } from '~components/person/OpprettKlage'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { KlageListe } from '~components/person/KlageListe'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { tagColors } from '~shared/Tags'

export const SakOversikt = ({ fnr }: { fnr: string }) => {
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)
  const [sakStatus, hentSak] = useApiCall(hentSakMedBehandlnger)

  useEffect(() => {
    hentSak(fnr)
  }, [fnr])

  return (
    <GridContainer>
      {mapApiResult(
        sakStatus,
        <Spinner visible={true} label="Henter sak og behandlinger" />,
        (error) => (
          <Alert variant="error">{JSON.stringify(error)}</Alert>
        ),
        (sakStatus) => (
          <>
            <MainContent>
              <Heading size="medium" spacing>
                Saknummer {sakStatus.sak.id}{' '}
                <Tag variant="success" size="medium">
                  {formaterSakstype(sakStatus.sak.sakType)}
                </Tag>
                {sakStatus.sak.utenlandstilknytning ? (
                  <Tag variant={tagColors[sakStatus.sak.utenlandstilknytning.type]} size="small">
                    {formaterEnumTilLesbarString(sakStatus.sak.utenlandstilknytning.type)}
                  </Tag>
                ) : (
                  <BodyShort>Du må velge en tilknytning for saken</BodyShort>
                )}
                <FlexRow justify="right">
                  <OpprettKlage sakId={sakStatus.sak.id} />
                  {sakStatus.sak.sakType === SakType.BARNEPENSJON && (
                    <ManueltOpphoerModal sakId={sakStatus.sak.id} behandlingliste={sakStatus.behandlinger} />
                  )}
                </FlexRow>
              </Heading>

              <BodyShort spacing>Denne saken tilhører enhet {sakStatus.sak.enhet}.</BodyShort>
              <BodyShort spacing>
                <Link href={`/person/${fnr}/sak/${sakStatus.sak.id}/brev`}>
                  Du finner brev tilhørende saken her <ExternalLinkIcon />
                </Link>
              </BodyShort>

              <hr />
              <Behandlingsliste behandlinger={sakStatus.behandlinger} sakId={sakStatus.sak.id} />

              {kanBrukeKlage ? <KlageListe sakId={sakStatus.sak.id} /> : null}
            </MainContent>
            <HendelseSidebar>
              <RelevanteHendelser sak={sakStatus.sak} fnr={fnr} behandlingliste={sakStatus.behandlinger} />
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
