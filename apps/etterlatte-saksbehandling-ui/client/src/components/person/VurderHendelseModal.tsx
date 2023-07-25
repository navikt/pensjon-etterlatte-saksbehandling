import { Alert, BodyShort, Button, Heading, Modal, Select, TextField } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { useNavigate } from 'react-router-dom'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

type Props = {
  open: boolean
  setOpen: (value: boolean) => void
  sakId: number
  revurderinger: Array<Revurderingsaarsak>
  valgtHendelse?: Grunnlagsendringshendelse
}
const VurderHendelseModal = (props: Props) => {
  const { revurderinger, valgtHendelse } = props
  const [error, setError] = useState<string | null>(null)
  const [valgtAarsak, setValgtAarsak] = useState<Revurderingsaarsak | undefined>(undefined)
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettRevurderingApi)
  const [begrunnelse, setBegrunnelse] = useState('')
  const navigate = useNavigate()

  const onSubmit = () => {
    if (valgtAarsak === undefined) {
      return setError('Du må velge en årsak')
    }

    opprettRevurdering(
      { sakId: props.sakId, aarsak: valgtAarsak, paaGrunnAvHendelseId: valgtHendelse?.id, begrunnelse: begrunnelse },
      (revurderingId: string) => {
        navigate(`/behandling/${revurderingId}/`)
      },
      () => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
      }
    )
  }

  return (
    <>
      <Modal open={props.open} onClose={() => props.setOpen(false)}>
        <Modal.Content>
          <ModalContentWrapper>
            <Heading spacing size="large">
              Vurder hendelse
            </Heading>
            <Alert variant={'warning'}>{valgtHendelse ? tekster.get(valgtHendelse.type)!.tittel : ''}</Alert>
            <BodyShort>
              <HjemmelLenke tittel={'§18-8 Lovparagraf'} lenke={''} />
            </BodyShort>
            <BodyShort spacing style={{ marginBottom: '2em' }}>
              {valgtHendelse ? tekster.get(valgtHendelse.type)!.beskrivelse : ''}
            </BodyShort>
            <div>
              <Select
                label="Velg revurderingsårsak"
                value={valgtAarsak}
                onChange={(e) => setValgtAarsak(e.target.value as Revurderingsaarsak)}
                error={error}
              >
                <option value={''}>Velg en årsak</option>
                {revurderinger.map((aarsak) => (
                  <option value={aarsak} key={aarsak}>
                    {tekstRevurderingsaarsak[aarsak]}
                  </option>
                ))}
              </Select>
              <TextField
                label="Begrunnelse"
                size="medium"
                type="text"
                value={begrunnelse}
                onChange={(e) => setBegrunnelse(e.target.value)}
              />
            </div>
          </ModalContentWrapper>

          <ButtonContainer>
            <Button variant={'secondary'} onClick={() => props.setOpen(false)}>
              Avbryt
            </Button>
            <Button loading={isPending(opprettRevurderingStatus)} onClick={onSubmit}>
              Start revurdering
            </Button>
          </ButtonContainer>
        </Modal.Content>
      </Modal>
    </>
  )
}

const ModalContentWrapper = styled.div`
  padding: 0 1em;
`

export const ButtonContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 0.5em;
  padding-top: 2em;
`

export default VurderHendelseModal

interface Grunnlagsendringstekst {
  tittel: string
  beskrivelse: string
}

const tekster = new Map<GrunnlagsendringsType, Grunnlagsendringstekst>([
  [
    GrunnlagsendringsType.DOEDSFALL,
    {
      tittel: 'Dødsfall',
      beskrivelse: 'Dødsfallsbeskrivelse her',
    },
  ],
  [
    GrunnlagsendringsType.UTFLYTTING,
    {
      tittel: 'Utflytting',
      beskrivelse: 'Utflyttingsbeskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.FORELDER_BARN_RELASJON,
    {
      tittel: 'Foreldre-barn-relasjon',
      beskrivelse: 'Foreldre-barn-relasjon-beskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
    {
      tittel: 'Vergemål eller fremtidsfullmakt',
      beskrivelse: 'Vergemål, fremtidsfullmakt, beskrivelse her',
    },
  ],
  [
    GrunnlagsendringsType.SIVILSTAND,
    {
      tittel: 'Sivilstand',
      beskrivelse: 'Sivilstand-beskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.GRUNNBELOEP,
    {
      tittel: 'Grunnbeløp endra',
      beskrivelse: 'Grunnbeløpet veldig endra med ein ganske lang tekst her',
    },
  ],
  [
    GrunnlagsendringsType.INSTITUSJONSOPPHOLD,
    {
      tittel: 'Institusjonsopphold',
      beskrivelse: 'Institusjonsoppholdbeskrivelse her',
    },
  ],
])
