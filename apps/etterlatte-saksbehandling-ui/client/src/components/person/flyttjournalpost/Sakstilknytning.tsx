import { Alert, Button, Heading, Modal, Panel, Select } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { isPending, isSuccess, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiError } from '~shared/api/apiClient'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React, { useEffect, useState } from 'react'
import { Journalpost, KnyttTilAnnenSakResponse } from '~shared/types/Journalpost'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { knyttTilAnnenSak } from '~shared/api/dokument'
import { hentSakForPerson, hentSakMedBehandlnger } from '~shared/api/sak'
import { fnrErGyldig } from '~utils/fnr'
import { SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formattering'
import { opprettOppgave } from '~shared/api/oppgaver'
import { useNavigate } from 'react-router-dom'
import { FeilregistrerJournalpost } from '~components/person/flyttjournalpost/FeilregistrerJournalpost'

interface Props {
  bruker: string
  valgtJournalpost: Journalpost
  oppdaterJournalposter: () => void
}

export const Sakstilknytning = ({ bruker, valgtJournalpost, oppdaterJournalposter }: Props) => {
  const navigate = useNavigate()

  const [sakType, setSakType] = useState<SakType>()

  const [sakMedBehandlingerStatus, apiHentSakMedBehandlinger] = useApiCall(hentSakMedBehandlnger)
  const [hentEllerOpprettSakStatus, apiHentEllerOpprettSak] = useApiCall(hentSakForPerson)
  const [nyOppgaveStatus, apiOpprettNyOppgave] = useApiCall(opprettOppgave)

  const [knyttTilAnnenSakStatus, apiKnyttTilAnnenSak] = useApiCall(knyttTilAnnenSak)

  useEffect(() => {
    if (!fnrErGyldig(bruker)) return

    apiHentSakMedBehandlinger(bruker)
  }, [])

  const opprettSak = () => {
    if (!sakType) return

    apiHentEllerOpprettSak(
      {
        fnr: bruker,
        type: sakType,
        opprettHvisIkkeFinnes: true,
      },
      () => apiHentSakMedBehandlinger(bruker)
    )
  }

  const opprettNyOppgave = (sakId: number, journalpostId: string) => {
    apiOpprettNyOppgave(
      {
        sakId,
        request: {
          oppgaveType: 'JOURNALFOERING',
          referanse: journalpostId!!,
          merknad: 'Flyttet fra annen sak',
        },
      },
      () => setTimeout(() => navigate('/'), 5000)
    )
  }

  const knyttJournalpostTilGjennySak = () => {
    if (!valgtJournalpost || !isSuccess(sakMedBehandlingerStatus)) return

    const sak = sakMedBehandlingerStatus.data.sak

    apiKnyttTilAnnenSak(
      {
        journalpostId: valgtJournalpost.journalpostId,
        request: {
          bruker: {
            id: sak.ident,
            idType: 'FNR',
          },
          fagsakId: sak.id.toString(),
          fagsaksystem: 'EY',
          journalfoerendeEnhet: sak.enhet,
          sakstype: 'FAGSAK',
          tema: temaFraSakstype(sak.sakType),
        },
      },
      (response: KnyttTilAnnenSakResponse) => {
        opprettNyOppgave(sak.id, response.nyJournalpostId)
      }
    )
  }

  if (valgtJournalpost.sak?.fagsaksystem === 'EY') {
    return (
      <>
        <Alert variant="info">
          Journalposten er allerede tilkoblet en sak i Gjenny (saksid {valgtJournalpost.sak.fagsakId})
        </Alert>

        {isSuccess(sakMedBehandlingerStatus) && !sakMedBehandlingerStatus.data.behandlinger.length && (
          <>
            <br />
            <Alert variant="warning">
              Det finnes ingen behandlinger på sak {sakMedBehandlingerStatus.data.sak.id}. Ønsker du å opprette en
              behandlingsoppgave?
            </Alert>
            <br />

            <FlexRow justify="right">
              <Button
                onClick={() => opprettNyOppgave(sakMedBehandlingerStatus.data.sak.id, valgtJournalpost.journalpostId)}
                loading={isPending(nyOppgaveStatus)}
              >
                Opprett oppgave
              </Button>
            </FlexRow>
          </>
        )}

        {isSuccess(nyOppgaveStatus) && (
          <Modal open={true}>
            <Alert variant="success">Ny oppgave er klar for behandling. Du sendes straks til oppgavebenken.</Alert>
          </Modal>
        )}
      </>
    )
  }

  return (
    <>
      <Heading size="small">Valgt journalpost</Heading>

      {valgtJournalpost.sak?.fagsaksystem !== 'EY' && (
        <FeilregistrerJournalpost valgtJournalpost={valgtJournalpost} oppdaterJournalposter={oppdaterJournalposter} />
      )}

      <hr />
      <br />

      {mapApiResult(
        sakMedBehandlingerStatus,
        <Spinner label="Sjekker om bruker har sak og behandling" visible />,
        (error: ApiError) => {
          if (error.code === 'PERSON_MANGLER_SAK') {
            return (
              <Panel>
                <Alert variant="warning">
                  Personen ({bruker}) har ingen sak i Gjenny
                  <br />
                  For å flytte en journalpost til Gjenny må brukeren ha en sak. Velg saktype og opprett sak.
                </Alert>
                <br />

                <FlexRow justify="right" align="end">
                  <Select
                    label="Sakstype"
                    value={sakType || ''}
                    onChange={(e) => setSakType(e.target.value as SakType)}
                  >
                    <option value={undefined}>Velg saktype ...</option>
                    <option value={SakType.BARNEPENSJON}>{formaterSakstype(SakType.BARNEPENSJON)}</option>
                    <option value={SakType.OMSTILLINGSSTOENAD}>{formaterSakstype(SakType.OMSTILLINGSSTOENAD)}</option>
                  </Select>
                  <Button onClick={opprettSak} disabled={!sakType} loading={isPending(hentEllerOpprettSakStatus)}>
                    Opprett {sakType ? formaterSakstype(sakType) : 'sak'}
                  </Button>
                </FlexRow>
              </Panel>
            )
          } else return <ApiErrorAlert>{error.detail}</ApiErrorAlert>
        },
        (sak) => (
          <Panel>
            <Heading size="small">Brukers sak i Gjenny</Heading>
            <InfoWrapper>
              <Info label="SakID" tekst={sak.sak.id} />
              <Info label="Sakstype" tekst={formaterSakstype(sak.sak.sakType)} />
              <Info label="Enhet" tekst={sak.sak.enhet} />
            </InfoWrapper>

            <br />

            <Alert variant="info">
              Journalposten er tilknyttet en annen sak, men kan knyttes til Gjenny-saken. Når du knytter en journalpost
              til en annen sak vil det bli laget en kopi av journalposten som knyttes til saken. Den gamle (med feil
              sakstilknytning) må være feilregistrert. Det vil også bli opprettet en oppgave i oppgavelisten for
              behandling av den nye journalposten.
              <br /> <br />
              <strong>OBS!</strong> Det er ingen begrensning på antall ganger du kan opprette kopi-journalposter
              tilknyttet saken, så pass på at det ikke allerede finnes en ny versjon av journalposten som er tilknyttet
              saken.
            </Alert>

            <br />

            <FlexRow justify="right">
              <Button
                onClick={knyttJournalpostTilGjennySak}
                loading={isPending(knyttTilAnnenSakStatus) || isPending(nyOppgaveStatus)}
                disabled={valgtJournalpost.journalstatus !== 'FEILREGISTRERT' || isSuccess(nyOppgaveStatus)}
              >
                Knytt til sak {sak.sak.id}
              </Button>
            </FlexRow>
          </Panel>
        )
      )}

      {isSuccess(nyOppgaveStatus) && (
        <Modal open={true}>
          <Alert variant="success">Ny oppgave er klar for behandling. Du sendes straks til oppgavebenken.</Alert>
        </Modal>
      )}
    </>
  )
}
