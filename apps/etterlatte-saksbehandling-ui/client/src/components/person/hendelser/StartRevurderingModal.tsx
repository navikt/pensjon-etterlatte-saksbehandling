import React, { ReactNode, useState } from 'react'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { Alert, BodyShort, Button, Heading, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { isPending } from '~shared/api/apiUtils'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import styled from 'styled-components'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'

interface Props {
  hendelse: Grunnlagsendringshendelse
  sakId: number
  revurderinger: Array<Revurderingaarsak>
}

export const StartRevurderingModal = ({ hendelse, sakId, revurderinger }: Props): ReactNode => {
  const navigate = useNavigate()

  const [open, setOpen] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  const [valgtAarsak, setValgtAarsak] = useState<Revurderingaarsak | undefined>(undefined)
  const [begrunnelse, setBegrunnelse] = useState('')
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const [opprettRevurderingResult, opprettRevurdering] = useApiCall(opprettRevurderingApi)

  const onSubmit = () => {
    if (valgtAarsak === undefined) {
      return setError('Du må velge en årsak')
    }

    opprettRevurdering(
      {
        sakId: sakId,
        aarsak: valgtAarsak,
        paaGrunnAvHendelseId: hendelse.id,
        fritekstAarsak: fritekstgrunn,
        begrunnelse: begrunnelse,
      },
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
      <Button size="small" icon={<ArrowsCirclepathIcon aria-hidden />}>
        Start revurdering
      </Button>

      <Modal open={open} onClose={() => setOpen(false)} aria-label="Vurder hendelse">
        <Modal.Body>
          <ModalContentWrapper>
            <Heading spacing size="large">
              Vurder hendelse
            </Heading>
            <Alert variant="warning">{hendelse ? tekster.get(hendelse.type)!.tittel : ''}</Alert>
            <BodyShort>
              <HjemmelLenke tittel="§18-8 Lovparagraf" lenke="" />
            </BodyShort>
            <BodyShort spacing style={{ marginBottom: '2em' }}>
              {hendelse ? tekster.get(hendelse.type)!.beskrivelse : ''}
            </BodyShort>
            <div>
              <Select
                label="Velg revurderingsårsak"
                value={valgtAarsak}
                onChange={(e) => setValgtAarsak(e.target.value as Revurderingaarsak)}
                error={error}
              >
                <option value="">Velg en årsak</option>
                {revurderinger.map((aarsak) => (
                  <option value={aarsak} key={aarsak}>
                    {tekstRevurderingsaarsak[aarsak]}
                  </option>
                ))}
              </Select>
              {valgtAarsak && [Revurderingaarsak.ANNEN, Revurderingaarsak.ANNEN_UTEN_BREV].includes(valgtAarsak) && (
                <VStack gap="10" style={{ marginTop: '2rem' }}>
                  <TextField
                    label="Beskriv årsak"
                    size="medium"
                    type="text"
                    value={fritekstgrunn}
                    onChange={(e) => setFritekstgrunn(e.target.value)}
                  />
                  <Alert variant="warning" style={{ maxWidth: '20em' }}>
                    Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen.
                  </Alert>
                </VStack>
              )}
              <MarginTop>
                <TextField
                  label="Begrunn valget for hendelsen"
                  size="medium"
                  type="text"
                  value={begrunnelse}
                  onChange={(e) => setBegrunnelse(e.target.value)}
                />
              </MarginTop>
            </div>
          </ModalContentWrapper>

          <ButtonGroup>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Avbryt
            </Button>
            <Button loading={isPending(opprettRevurderingResult)} onClick={onSubmit}>
              Start revurdering
            </Button>
          </ButtonGroup>
        </Modal.Body>
      </Modal>
    </>
  )
}

const ModalContentWrapper = styled.div`
  padding: 0 1em;
`

const MarginTop = styled.div`
  margin-top: 1rem;
`

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
    GrunnlagsendringsType.ADRESSE,
    {
      tittel: 'Adresse',
      beskrivelse: 'Adressebeskrivelse',
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
