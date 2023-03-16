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
export const TrygdetidOppsummert: React.FC<Props> = ({ trygdetid, setTrygdetid }) => {
  const { behandlingId: behandlingsId } = useParams()
  const [nasjonalTrygdetid, setNasjonalTrygdetid] = useState<number | null>(
    trygdetid.oppsummertTrygdetid == null ? null : trygdetid.oppsummertTrygdetid.nasjonalTrygdetid
  )
  const [fremtidigTrygdetid, setFremtidigTrygdetid] = useState<number | null>(
    trygdetid.oppsummertTrygdetid == null ? null : trygdetid.oppsummertTrygdetid.fremtidigTrygdetid
  )
  const [oppsummertTrygdetid, setOppsummertTrygdetid] = useState<number | null>(
    trygdetid.oppsummertTrygdetid == null ? null : trygdetid.oppsummertTrygdetid.totalt
  )
  const [oppsummertTrygdetidStatus, requestLagreOppsummertTrygdetid] = useApiCall(lagreOppsummertTrygdetid)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!behandlingsId) throw new Error('Mangler behandlingsid')
    if (!oppsummertTrygdetid) throw new Error('Oppsummert trygdetid kan ikke vaere null')
    requestLagreOppsummertTrygdetid(
      {
        behandlingsId,
        oppsummertTrygdetid: {
          nasjonalTrygdetid: nasjonalTrygdetid,
          fremtidigTrygdetid: fremtidigTrygdetid,
          totalt: oppsummertTrygdetid,
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
          Oppsummert trygdetid
        </Heading>
        <TrygdetidForm onSubmit={onSubmit}>
          <FormWrapper>
            <TextField
              label="Nasjonal trygdetid"
              size="small"
              type="number"
              onChange={(e) => setNasjonalTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Fremtidig trygdetid"
              size="small"
              type="number"
              onChange={(e) => setFremtidigTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Sum trygdetid"
              size="small"
              type="number"
              onChange={(e) => setOppsummertTrygdetid(Number(e.target.value))}
            />
          </FormWrapper>
          <FormKnapper>
            <Button loading={isPending(oppsummertTrygdetidStatus)} type="submit">
              Lagre
            </Button>
          </FormKnapper>
        </TrygdetidForm>
      </ContentHeader>
    </>
  )
}

const TrygdetidForm = styled.form`
  margin-left: auto;
`
