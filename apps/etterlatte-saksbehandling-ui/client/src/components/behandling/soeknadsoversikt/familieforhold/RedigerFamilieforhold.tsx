import { Familieforhold } from '~shared/types/Person'
import styled from 'styled-components'
import { Alert, Button, Panel, TextField } from '@navikt/ds-react'
import { InputList, InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import React, { useState } from 'react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFamilieforhold } from '~shared/api/behandling'
import { useBehandling } from '~components/behandling/useBehandling'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { RedigertFamilieforhold } from '~shared/types/grunnlag'

type Props = {
  familieforhold: Familieforhold
}

export const RedigerFamilieforhold = ({ familieforhold }: Props) => {
  const behandlingId = useBehandling()!.id // TODO.. behandling som prop istedet

  const [redigerbartFamilieforhold, setFamilieforhold] = useState<RedigertFamilieforhold>({
    gjenlevende: familieforhold.gjenlevende.map((gjenlevende) => gjenlevende.opplysning.foedselsnummer),
    avdoede: familieforhold.avdoede.map((avdoede) => avdoede.opplysning.foedselsnummer),
  })

  const [status, redigerFamilieforholdRequest] = useApiCall(redigerFamilieforhold)
  const [feilmelding, setFeilmelding] = useState<string | null>(null)

  const lagre = () => {
    setFeilmelding(null)
    const foreldre = redigerbartFamilieforhold.avdoede.concat(redigerbartFamilieforhold.gjenlevende)
    if (foreldre.length != 2) {
      setFeilmelding('Mangler en eller flere forelder')
    }
    foreldre.find((fnr) => {
      if (fnr.length !== 11) {
        setFeilmelding('Ugyldig fødselsnummer')
      }
    })

    if (feilmelding == null) {
      redigerFamilieforholdRequest({
        behandlingId: behandlingId,
        redigert: redigerbartFamilieforhold,
      })
    }
  }

  const oppdater = (felt: 'gjenlevende' | 'avdoede', fnr: string, oppdateres: number) =>
    setFamilieforhold({
      gjenlevende:
        felt === 'gjenlevende'
          ? redigerbartFamilieforhold.gjenlevende.map((gjenlevende, index) =>
              index === oppdateres ? fnr : gjenlevende
            )
          : redigerbartFamilieforhold.gjenlevende,
      avdoede:
        felt === 'avdoede'
          ? redigerbartFamilieforhold.avdoede.map((avdoed, index) => (index === oppdateres ? fnr : avdoed))
          : redigerbartFamilieforhold.avdoede,
    })

  const fjern = (felt: 'gjenlevende' | 'avdoede', fjernes: number) =>
    setFamilieforhold({
      gjenlevende:
        felt === 'gjenlevende'
          ? redigerbartFamilieforhold.gjenlevende.filter((_, index) => index !== fjernes)
          : redigerbartFamilieforhold.gjenlevende,
      avdoede:
        felt === 'avdoede'
          ? redigerbartFamilieforhold.avdoede.filter((_, index) => index !== fjernes)
          : redigerbartFamilieforhold.avdoede,
    })

  const leggTil = (felt: 'gjenlevende' | 'avdoede') =>
    setFamilieforhold({
      gjenlevende:
        felt === 'gjenlevende'
          ? redigerbartFamilieforhold.gjenlevende.concat([''])
          : redigerbartFamilieforhold.gjenlevende,
      avdoede: felt === 'avdoede' ? redigerbartFamilieforhold.avdoede.concat(['']) : redigerbartFamilieforhold.avdoede,
    })

  const kanLeggeTil = (): boolean => {
    return redigerbartFamilieforhold.gjenlevende.length + redigerbartFamilieforhold.avdoede.length < 2
  }

  return (
    <div>
      <Form>
        <FormWrapper>
          <Panel border>
            <InputList>
              {redigerbartFamilieforhold.avdoede?.map((gjenlevende, index) => (
                <InputRow key={index}>
                  <TextField
                    label="Avdøde forelder"
                    value={gjenlevende}
                    pattern="[0-9]{11}"
                    maxLength={11}
                    onChange={(e) => oppdater('avdoede', e.target.value, index)}
                    description="Oppgi fødselsnummer"
                  />
                  <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern('avdoede', index)} />
                </InputRow>
              ))}
              {/* eslint-disable-next-line react/jsx-no-undef */}
              <Button icon={<PlusIcon />} onClick={() => leggTil('avdoede')} disabled={!kanLeggeTil()}>
                Legg til avdøde
              </Button>
            </InputList>
          </Panel>
          <Panel border>
            <InputList>
              {redigerbartFamilieforhold.gjenlevende?.map((gjenlevende, index) => (
                <InputRow key={index}>
                  <TextField
                    label="Gjenlevende forelder"
                    value={gjenlevende}
                    pattern="[0-9]{11}"
                    maxLength={11}
                    onChange={(e) => oppdater('gjenlevende', e.target.value, index)}
                    description="Oppgi fødselsnummer"
                  />
                  <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern('gjenlevende', index)} />
                </InputRow>
              ))}
              {/* eslint-disable-next-line react/jsx-no-undef */}
              <Button icon={<PlusIcon />} onClick={() => leggTil('gjenlevende')} disabled={!kanLeggeTil()}>
                Legg til gjenlevende
              </Button>
            </InputList>
          </Panel>
        </FormWrapper>
        <Knapp>
          <Button variant="secondary" onClick={lagre} loading={isPending(status)}>
            Lagre
          </Button>
        </Knapp>
        {isSuccess(status) && <Alert variant="success">Lagret redigert familieforhold</Alert>}
        {isFailure(status) && <Alert variant="error">Noe gikk galt!</Alert>}
        {feilmelding && <Alert variant="error">{feilmelding}</Alert>}
      </Form>
    </div>
  )
}

const Form = styled.div``

export const FormWrapper = styled.div`
  display: flex;
  gap: 1rem;
  margin-right: 1em;
`

const Knapp = styled.div`
  margin-top: 1em;
`
