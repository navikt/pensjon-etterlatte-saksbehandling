import React, { useState } from 'react'
import { Alert, Box, Button, Checkbox, Heading, TextField, VStack } from '@navikt/ds-react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IDetaljertBeregnetTrygdetid,
  ITrygdetid,
  oppdaterTrygdetidOverstyrtMigrering,
  opprettTrygdetidOverstyrtMigrering,
} from '~shared/api/trygdetid'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const TrygdetidManueltOverstyrt = ({
  behandling,
  trygdetidId,
  ident,
  beregnetTrygdetid,
  oppdaterTrygdetid,
}: {
  behandling: IDetaljertBehandling
  trygdetidId: string
  ident: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
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
            <Button variant="danger" onClick={overskrivOverstyrtTrygdetid} loading={isPending(opprettStatus)}>
              Opprett overstyrt trygdetid på nytt
            </Button>
          </VStack>
        </Box>
      )}
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
            disabled={anvendtTrygdetid == null || (skalHaProrata && (prorataNevner == null || prorataTeller == null))}
          >
            Send inn
          </Button>
        </Knapp>
        {isFailureHandler({
          apiResult: status,
          errorMessage: 'Det oppsto en feil ved oppdatering av trygdetid.',
        })}
      </FormWrapper>
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
