import { Alert, BodyShort, Button, Heading, Label, Loader, Modal, Textarea } from '@navikt/ds-react'
import styled from 'styled-components'
import { Grunnlagsendringshendelse, STATUS_IRRELEVANT } from '~components/person/typer'
import {
  FnrTilNavnMapContext,
  grunnlagsendringsTittel,
  stoetterRevurderingAvHendelse,
} from '~components/person/uhaandtereHendelser/utils'
import { formaterStringDato } from '~utils/formattering'
import { isFailure, isPending, isSuccess, Result, useApiCall } from '~shared/hooks/useApiCall'
import React, { useMemo, useState } from 'react'
import { PersonerISakResponse } from '~shared/api/grunnlag'
import HistoriskeHendelser from '~components/person/uhaandtereHendelser/HistoriskeHendelser'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OpprettNyBehandling } from '~components/person/OpprettNyBehandling'
import OpprettRevurderingModal from '~components/person/OpprettRevurderingModal'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { HendelseBeskrivelse } from '~components/person/uhaandtereHendelser/HendelseBeskrivelse'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
  disabled: boolean
  grunnlag: Result<PersonerISakResponse>
  revurderinger: Array<Revurderingsaarsak>
  sakId: number
}

const RelevanteHendelser = (props: Props) => {
  const { hendelser, disabled, grunnlag, revurderinger, sakId } = props

  if (hendelser.length === 0) {
    return revurderinger.length > 0 ? <OpprettNyBehandling revurderinger={revurderinger} sakId={sakId} /> : null
  }

  const [visOpprettRevurderingsmodal, setVisOpprettRevurderingsmodal] = useState<boolean>(false)
  const [valgtHendelse, setValgtHendelse] = useState<Grunnlagsendringshendelse | undefined>(undefined)
  const startRevurdering = (hendelse: Grunnlagsendringshendelse) => {
    setValgtHendelse(hendelse)
    setVisOpprettRevurderingsmodal(true)
  }

  const navneMap = useMemo(() => {
    if (isSuccess(grunnlag)) {
      return grunnlag.data.personer
    } else {
      return {}
    }
  }, [grunnlag])

  const relevanteHendelser = hendelser.filter((h) => h.status !== STATUS_IRRELEVANT)
  const lukkedeHendelser = hendelser.filter((h) => h.status === STATUS_IRRELEVANT)

  return (
    <>
      <BorderWidth>
        <HendelserBorder>
          <FnrTilNavnMapContext.Provider value={navneMap}>
            {relevanteHendelser && relevanteHendelser.length > 0 && (
              <StyledAlert>
                Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.
              </StyledAlert>
            )}
            <Heading size="medium">Nye hendelser</Heading>
            {relevanteHendelser.map((hendelse) => (
              <UhaandtertHendelse
                key={hendelse.id}
                hendelse={hendelse}
                disabled={disabled}
                startRevurdering={startRevurdering}
                revurderinger={revurderinger}
              />
            ))}
          </FnrTilNavnMapContext.Provider>
        </HendelserBorder>
      </BorderWidth>
      {valgtHendelse && (
        <OpprettRevurderingModal
          sakId={sakId}
          valgtHendelse={valgtHendelse}
          open={visOpprettRevurderingsmodal}
          setOpen={setVisOpprettRevurderingsmodal}
          revurderinger={revurderinger}
        />
      )}
      <OpprettNyBehandling revurderinger={revurderinger} sakId={sakId} />
      <HistoriskeHendelser hendelser={lukkedeHendelser} />
    </>
  )
}

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
                    <Button onClick={() => lukkGrunnlagshendelseWrapper()}>Lagre</Button>
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

const HendelserBorder = styled.div`
  outline: solid;
  outline-offset: 25px;
`

const BorderWidth = styled.div`
  margin-top: 55px;
  margin-right: 10px;
  margin-left: 2px;
  max-width: 1000px;
`

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

const StyledAlert = styled(Alert).attrs({ variant: 'warning' })`
  display: inline-flex;
  margin: 1em 0;
`
const BodySmall = styled(BodyShort).attrs({ size: 'small' })`
  max-width: 25em;
`

export default RelevanteHendelser
