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
    trygdetid.oppsummertTrygdetid == null ? 0 : trygdetid.oppsummertTrygdetid.nasjonalTrygdetid
  )
  const [fremtidigTrygdetid, setFremtidigTrygdetid] = useState<number>(
    trygdetid.oppsummertTrygdetid == null ? 0 : trygdetid.oppsummertTrygdetid.fremtidigTrygdetid
  )
  const [oppsummertTrygdetid, setOppsummertTrygdetid] = useState<number>(
    trygdetid.oppsummertTrygdetid == null ? 0 : trygdetid.oppsummertTrygdetid.totalt
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
          Total trygdetid
        </Heading>
        <TrygdetidForm onSubmit={onSubmit}>
          <FormWrapper>
            <TextField
              label="Nasjonal trygdetid (år)"
              size="medium"
              type="number"
              value={nasjonalTrygdetid!!}
              onChange={(e) => setNasjonalTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Fremtidig trygdetid (år)"
              size="medium"
              type="number"
              value={fremtidigTrygdetid!!}
              onChange={(e) => setFremtidigTrygdetid(Number(e.target.value))}
            />
            <TextField
              label="Sum trygdetid (år)"
              size="medium"
              type="number"
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
