import React, { FormEvent, useState } from 'react'
import { Button, Heading, TextField } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import { FormKnapper, FormWrapper } from '~components/behandling/trygdetid/styled'
import { ITrygdetid, lagreOppsummertTrygdetid } from '~shared/api/trygdetid'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
}
export const TrygdetidBeregnet: React.FC<Props> = ({ trygdetid, setTrygdetid }) => {
  const { behandlingId: behandlingsId } = useParams()
  const [nasjonalTrygdetid, setNasjonalTrygdetid] = useState<number>(
    trygdetid.beregnetTrygdetid == null ? 0 : trygdetid.beregnetTrygdetid.nasjonal
  )
  const [fremtidigTrygdetid, setFremtidigTrygdetid] = useState<number>(
    trygdetid.beregnetTrygdetid == null ? 0 : trygdetid.beregnetTrygdetid.fremtidig
  )
  const [oppsummertTrygdetid, setOppsummertTrygdetid] = useState<number>(
    trygdetid.beregnetTrygdetid == null ? 0 : trygdetid.beregnetTrygdetid.total
  )
  const [oppsummertTrygdetidStatus, requestLagreOppsummertTrygdetid] = useApiCall(lagreOppsummertTrygdetid)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!behandlingsId) throw new Error('Mangler behandlingsid')
    if (!oppsummertTrygdetid) throw new Error('Oppsummert trygdetid kan ikke vaere null')
    requestLagreOppsummertTrygdetid(
      {
        behandlingsId,
        beregnetTrygdetid: {
          nasjonal: nasjonalTrygdetid,
          fremtidig: fremtidigTrygdetid,
          total: oppsummertTrygdetid,
        },
      },
      (respons) => {
        setTrygdetid(respons)
      }
    )
  }

  return (
    <>
      <ContentHeader>
        <Heading spacing size="medium" level="2">
          Total trygdetid
        </Heading>
        <TrygdetidForm onSubmit={onSubmit}>
          <FormWrapper>
            <TextField
              label="Nasjonal trygdetid (år)"
              size="medium"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={nasjonalTrygdetid!!}
              onChange={(e) => setNasjonalTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Fremtidig trygdetid (år)"
              size="medium"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={fremtidigTrygdetid!!}
              onChange={(e) => setFremtidigTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Sum trygdetid (år)"
              size="medium"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={oppsummertTrygdetid!!}
              onChange={(e) => setOppsummertTrygdetid(Number(e.target.value))}
            />

            <FormKnapper>
              <Button loading={isPending(oppsummertTrygdetidStatus)} type="submit">
                Lagre
              </Button>
            </FormKnapper>
          </FormWrapper>
        </TrygdetidForm>
      </ContentHeader>
    </>
  )
}

const TrygdetidForm = styled.form`
  margin-left: auto;
`
