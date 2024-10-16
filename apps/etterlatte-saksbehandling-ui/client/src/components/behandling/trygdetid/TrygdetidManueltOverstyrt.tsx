import React, { useState } from 'react'
import { Alert, BodyShort, Box, Button, Checkbox, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IDetaljertBeregnetTrygdetid,
  ITrygdetid,
  oppdaterTrygdetidOverstyrtMigrering,
  opprettTrygdetidOverstyrtMigrering,
} from '~shared/api/trygdetid'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'

import { isPending, mapAllApiResult } from '~shared/api/apiUtils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Toast } from '~shared/alerts/Toast'

export const TrygdetidManueltOverstyrt = ({
  behandling,
  trygdetidId,
  ident,
  beregnetTrygdetid,
  oppdaterTrygdetid,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  trygdetidId: string
  ident: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  redigerbar: boolean
}) => {
  const personopplysninger = usePersonopplysninger()

  const [skalHaProrata, setSkalHaProrata] = useState<boolean>(beregnetTrygdetid.resultat.prorataBroek != null)

  const [anvendtTrygdetid, setAnvendtTrygdetid] = useState<number | undefined>(
    skalHaProrata
      ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
      : beregnetTrygdetid.resultat.samletTrygdetidNorge
  )

  const [prorataTeller, setTeller] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.teller)
  const [prorataNevner, setNevner] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.nevner)

  const [status, oppdaterTrygdetidRequest] = useApiCall(oppdaterTrygdetidOverstyrtMigrering)
  const [opprettStatus, opprettOverstyrtTrygdetid] = useApiCall(opprettTrygdetidOverstyrtMigrering)

  const lagre = () => {
    oppdaterTrygdetidRequest(
      {
        behandlingId: behandling.id,
        trygdetidId: trygdetidId,
        anvendtTrygdetid: anvendtTrygdetid!!,
        prorataBroek: skalHaProrata
          ? {
              teller: prorataTeller!!,
              nevner: prorataNevner!!,
            }
          : undefined,
      },
      (trygdetid) => {
        oppdaterTrygdetid(trygdetid)
      }
    )
  }

  const overskrivOverstyrtTrygdetid = () => {
    opprettOverstyrtTrygdetid({ behandlingId: behandling.id, overskriv: true }, () => window.location.reload())
  }

  const identErIGrunnlag = personopplysninger?.avdoede?.find((person) => person.opplysning.foedselsnummer === ident)

  if (!identErIGrunnlag) {
    if (ident == 'UKJENT_AVDOED' && behandling.behandlingType !== IBehandlingsType.REVURDERING) {
      return (
        <>
          <Alert variant="error">
            Brev støtter ikke ukjent avdød. Dersom avdød ikke ble oppgitt ved opprettelse av behandlingen må du opprette
            ny overstyrt trygdetid.
          </Alert>
          <br />
          <Button variant="danger" onClick={overskrivOverstyrtTrygdetid} loading={isPending(opprettStatus)}>
            Opprett på nytt
          </Button>
        </>
      )
    }
    if (ident !== 'UKJENT_AVDOED') {
      return <Alert variant="error">Fant ikke avdød ident {ident} (trygdetid) i behandlingsgrunnlaget</Alert>
    }
  }

  return (
    <>
      <Heading size="small" level="3">
        Manuelt overstyrt trygdetid
      </Heading>

      {ident == 'UKJENT_AVDOED' && (
        <Box maxWidth="40rem">
          <VStack gap="1">
            <Alert variant="warning">OBS! Trygdetiden er koblet til ukjent avdød</Alert>
            {redigerbar && (
              <Button variant="danger" onClick={overskrivOverstyrtTrygdetid} loading={isPending(opprettStatus)}>
                Opprett overstyrt trygdetid på nytt
              </Button>
            )}
          </VStack>
        </Box>
      )}
      {redigerbar && (
        <>
          <FormWrapper>
            <TextField
              label="Anvendt trygdetid"
              placeholder="Anvendt trygdetid"
              value={anvendtTrygdetid || ''}
              pattern="[0-9]{11}"
              maxLength={11}
              htmlSize={20}
              onChange={(e) => setAnvendtTrygdetid(Number(e.target.value))}
            />

            <Checkbox checked={skalHaProrata} onChange={() => setSkalHaProrata(!skalHaProrata)}>
              Prorata brøk
            </Checkbox>
            {skalHaProrata && (
              <InputRow>
                <TextField
                  label="Prorata teller"
                  placeholder="Prorata teller"
                  value={prorataTeller || ''}
                  pattern="[0-9]{11}"
                  maxLength={11}
                  onChange={(e) => setTeller(Number(e.target.value))}
                />
                <TextField
                  label="Prorata nevner"
                  placeholder="Prorata nevner"
                  value={prorataNevner || ''}
                  pattern="[0-9]{11}"
                  maxLength={11}
                  onChange={(e) => setNevner(Number(e.target.value))}
                />
              </InputRow>
            )}

            <Knapp>
              <Button
                variant="secondary"
                onClick={lagre}
                loading={isPending(status)}
                disabled={
                  anvendtTrygdetid == null || (skalHaProrata && (prorataNevner == null || prorataTeller == null))
                }
              >
                Send inn
              </Button>
            </Knapp>
          </FormWrapper>
          {mapAllApiResult(
            status,
            <Spinner label="Lagrer trygdetid" />,
            null,
            () => (
              <ApiErrorAlert>En feil har oppstått ved lagring av trygdetid</ApiErrorAlert>
            ),
            () => (
              <Toast melding="Trygdetid lagret" position="bottom-center" />
            )
          )}

          {mapAllApiResult(
            opprettStatus,
            <Spinner label="Overstyrer trygdetid" />,
            null,
            () => (
              <ApiErrorAlert>En feil har oppstått ved overstyring av trygdetid</ApiErrorAlert>
            ),
            () => (
              <Toast melding="Trygdetid overstyrt" position="bottom-center" />
            )
          )}
        </>
      )}
      {!redigerbar && (
        <VStack gap="2">
          <Heading size="xsmall" level="4">
            Anvendt trygdetid
          </Heading>
          <BodyShort>{anvendtTrygdetid}</BodyShort>
          <Heading size="xsmall" level="4">
            Prorata-brøk
          </Heading>
          <BodyShort>{skalHaProrata ? 'Ja' : 'Nei'}</BodyShort>

          <Checkbox checked={skalHaProrata} readOnly={true}>
            Prorata-brøk
          </Checkbox>
          {skalHaProrata && (
            <HStack gap="4">
              <Box>
                <Heading size="xsmall" level="4">
                  Prorata teller
                </Heading>
                <BodyShort>{prorataTeller}</BodyShort>
              </Box>
              <Box>
                <Heading size="xsmall" level="4">
                  Prorata nevner
                </Heading>
                <BodyShort>{prorataNevner}</BodyShort>
              </Box>
            </HStack>
          )}
        </VStack>
      )}
    </>
  )
}

const FormWrapper = styled.div`
  display: grid;
  gap: var(--a-spacing-4);
`
const Knapp = styled.div`
  margin-top: 1em;
`
