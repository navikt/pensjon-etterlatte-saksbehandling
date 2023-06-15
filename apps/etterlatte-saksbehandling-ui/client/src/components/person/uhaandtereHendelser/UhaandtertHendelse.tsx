import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import React, { useState } from 'react'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/uhaandtereHendelser/utils'
import { Alert, BodyShort, Button, Heading, Label, Loader, Modal, Textarea } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { HendelseBeskrivelse } from '~components/person/uhaandtereHendelser/HendelseBeskrivelse'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import InstitusjonsoppholdVurderingBegrunnelse from '~components/person/uhaandtereHendelser/InstitusjonsoppholdVurderingBegrunnelse'

const UhaandtertHendelse = (props: {
  hendelse: Grunnlagsendringshendelse
  disabled: boolean
  startRevurdering: (hendelse: Grunnlagsendringshendelse) => void
  revurderinger: Array<Revurderingsaarsak>
}) => {
  const { hendelse, disabled, startRevurdering, revurderinger } = props
  const { samsvarMellomKildeOgGrunnlag, opprettet } = hendelse
  const [open, setOpen] = useState(false)
  const [hendelsekommentar, oppdaterKommentar] = useState<string>('')
  const [res, lukkGrunnlagshendelseFunc, resetApiCall] = useApiCall(lukkGrunnlagshendelse)
  const stoetterRevurdering = stoetterRevurderingAvHendelse(hendelse, revurderinger)
  const { type: samsvarType } = samsvarMellomKildeOgGrunnlag
  const lukkGrunnlagshendelseWrapper = () => {
    lukkGrunnlagshendelseFunc(
      { ...hendelse, kommentar: hendelsekommentar },
      () => {
        setOpen(false)
        location.reload()
      },
      (err) => {
        console.error(`Feil status: ${err.status} error: ${err.error}`)
      }
    )
  }
  return (
    <Wrapper>
      <Label spacing>{grunnlagsendringsTittel[samsvarType]}</Label>
      <Content>
        <Header>
          <BodySmall>Dato</BodySmall>
          <BodySmall>{formaterStringDato(opprettet)}</BodySmall>
        </Header>
        <HendelseBeskrivelse hendelse={hendelse} />

        <div>
          {disabled ? (
            <Alert variant="info" inline>
              Denne saken har en åpen revurdering, denne må behandles før en ny kan startes.
            </Alert>
          ) : (
            <>
              {stoetterRevurdering ? (
                <Button disabled={disabled} onClick={() => startRevurdering(hendelse)}>
                  Start revurdering
                </Button>
              ) : (
                <Alert variant="info" inline>
                  Automatisk revurdering støttes ikke for denne hendelsen
                </Alert>
              )}
            </>
          )}
          <MarginTop15>
            <Button onClick={() => setOpen(true)}>Lukk hendelse</Button>
            <Modal open={open} onClose={() => setOpen((x) => !x)} closeButton={false} aria-labelledby="modal-heading">
              <Modal.Content>
                <Heading spacing level="2" size="medium" id="modal-heading">
                  Avslutt uten revurdering
                </Heading>
                {hendelse.type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD ? (
                  <>
                    <InstitusjonsoppholdVurderingBegrunnelse
                      sakId={hendelse.sakId}
                      grunnlagsEndringshendelseId={hendelse.id}
                      lukkGrunnlagshendelseWrapper={lukkGrunnlagshendelseWrapper}
                    />
                    <Button
                      variant="secondary"
                      onClick={() => {
                        setOpen(false)
                      }}
                    >
                      Lukk modal
                    </Button>
                  </>
                ) : (
                  <>
                    <MaxWidth>
                      I noen tilfeller krever ikke ny informasjon eller hendelser noen revurdering. Beskriv hvorfor en
                      revurdering ikke er nødvendig.
                    </MaxWidth>
                    <Textarea
                      label={'Begrunnelse'}
                      value={hendelsekommentar}
                      onChange={(e) => oppdaterKommentar(e.target.value)}
                    />
                    <MarginTop15>
                      <MarginRight15>
                        <Button onClick={lukkGrunnlagshendelseWrapper}>Lagre</Button>
                      </MarginRight15>
                      <Button
                        variant="secondary"
                        onClick={() => {
                          oppdaterKommentar('')
                          setOpen(false)
                          resetApiCall()
                        }}
                      >
                        Avbryt
                      </Button>
                    </MarginTop15>
                  </>
                )}
                {isPending(res) && <Loader />}
                {isFailure(res) && <ApiErrorAlert>Vi kunne ikke lukke hendelsen</ApiErrorAlert>}
              </Modal.Content>
            </Modal>
          </MarginTop15>
        </div>
      </Content>
    </Wrapper>
  )
}

const MarginTop15 = styled.div`
  margin-top: 15px;
`

const MaxWidth = styled.p`
  max-width: 500px;
`

const MarginRight15 = styled.span`
  margin-right: 15px;
`

const Wrapper = styled.div`
  margin-top: 3rem;
  margin-bottom: 1rem;
  max-width: 50rem;
`
const Header = styled.div`
  display: flex;
  flex-direction: column;
  align-self: flex-start;
`

const Content = styled.div`
  display: grid;
  grid-template-columns: 5rem auto 13rem;
  gap: 2em;
  align-items: center;
`

const BodySmall = styled(BodyShort).attrs({ size: 'small' })`
  max-width: 25em;
`

export default UhaandtertHendelse
