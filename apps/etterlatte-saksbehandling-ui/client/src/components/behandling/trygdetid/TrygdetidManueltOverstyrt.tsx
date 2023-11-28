import React, { useState } from 'react'
import { Alert, Button, Checkbox, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { IDetaljertBeregnetTrygdetid, ITrygdetid, oppdaterTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'

export const TrygdetidManueltOverstyrt = ({
  behandlingId,
  beregnetTrygdetid,
  oppdaterTrygdetid,
}: {
  behandlingId: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
}) => {
  const [anvendtTrygdetid, setAnvendtTrygdetid] = useState<number | undefined>(
    beregnetTrygdetid.resultat.samletTrygdetidNorge
  )

  const [skalHaProrata, setSkalHaProrata] = useState<boolean>(beregnetTrygdetid.resultat.prorataBroek != null)
  const [prorataTeller, setTeller] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.teller)
  const [prorataNevner, setNevner] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.nevner)

  const [status, oppdaterTrygdetidRequest] = useApiCall(oppdaterTrygdetidOverstyrtMigrering)

  const lagre = () => {
    oppdaterTrygdetidRequest(
      {
        behandlingId: behandlingId,
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

  return (
    <FormWrapper>
      <TextField
        label="Anvendt trygdetid"
        placeholder="Anvendt trygdetid"
        value={anvendtTrygdetid || ''}
        pattern="[0-9]{11}"
        maxLength={11}
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
      {isFailure(status) && <Alert variant="error">Det oppsto en feil ved oppdatering av trygdetid.</Alert>}
    </FormWrapper>
  )
}

const FormWrapper = styled.div`
  width: 10em;
  display: grid;
  gap: var(--a-spacing-4);
`
const Knapp = styled.div`
  margin-top: 1em;
`
