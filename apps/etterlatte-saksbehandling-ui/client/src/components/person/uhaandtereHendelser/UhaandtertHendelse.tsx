import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/uhaandtereHendelser/utils'
import { Alert, BodyShort, Button, Heading, Link, Loader, Modal, Table, Textarea } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { HendelseBeskrivelse } from '~components/person/uhaandtereHendelser/HendelseBeskrivelse'
import styled from 'styled-components'
import InstitusjonsoppholdVurderingBegrunnelse from '~components/person/uhaandtereHendelser/InstitusjonsoppholdVurderingBegrunnelse'
import { ArrowsCirclepathIcon, XMarkIcon } from '@navikt/aksel-icons'
import { ButtonGroup } from '~components/person/VurderHendelseModal'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const UhaandtertHendelse = (props: {
  hendelse: Grunnlagsendringshendelse
  harAapenRevurdering: boolean
  startRevurdering: (hendelse: Grunnlagsendringshendelse) => void
  revurderinger: Array<Revurderingaarsak>
}) => {
  const { hendelse, harAapenRevurdering, startRevurdering, revurderinger } = props
  const { samsvarMellomKildeOgGrunnlag, opprettet } = hendelse
  const [open, setOpen] = useState(false)
  const [hendelsekommentar, oppdaterKommentar] = useState<string>('')
  const [res, lukkGrunnlagshendelseFunc, resetApiCall] = useApiCall(lukkGrunnlagshendelse)
  const stoetterRevurdering = stoetterRevurderingAvHendelse(hendelse, revurderinger)
  const { type: samsvarType } = samsvarMellomKildeOgGrunnlag

  const tattMedIBehandling = hendelse.status === 'TATT_MED_I_BEHANDLING'
  const lukkGrunnlagshendelseWrapper = () => {
    lukkGrunnlagshendelseFunc(
      { ...hendelse, kommentar: hendelsekommentar },
      () => {
        setOpen(false)
        location.reload()
      },
      (err) => {
        console.error(`Feil status: ${err.status} error: ${err.detail}`)
      }
    )
  }

  return (
    <Table.ExpandableRow
      content={
        <>
          <HendelseBeskrivelse hendelse={hendelse} />

          <BodyShort spacing>
            {tattMedIBehandling ? (
              <Alert variant="info" inline>
                Denne hendelsen har en revurdering knyttet til seg.{' '}
                <BlueLink href={`/behandling/${hendelse.behandlingId}/revurderingsoversikt`}>
                  Gå til revurdering
                </BlueLink>
              </Alert>
            ) : harAapenRevurdering ? (
              <Alert variant="info" inline>
                Denne saken har en åpen revurdering, denne må behandles før en ny kan startes.
              </Alert>
            ) : (
              !stoetterRevurdering && (
                <Alert variant="info" inline>
                  Automatisk revurdering støttes ikke for denne hendelsen
                </Alert>
              )
            )}
          </BodyShort>

          <div style={{ minHeight: '3rem', marginTop: '1rem' }}>
            <div>
              {!tattMedIBehandling && stoetterRevurdering && (
                <Button
                  disabled={harAapenRevurdering}
                  onClick={() => startRevurdering(hendelse)}
                  icon={<ArrowsCirclepathIcon />}
                >
                  Start revurdering
                </Button>
              )}
              <Button variant="tertiary" onClick={() => setOpen(true)} icon={<XMarkIcon />} style={{ float: 'right' }}>
                Lukk hendelse
              </Button>
            </div>

            <Modal
              open={open}
              onClose={() => setOpen(false)}
              aria-labelledby="modal-heading"
              style={{ maxWidth: '60rem' }}
            >
              <Modal.Body>
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
                    <BodyShort spacing>
                      I noen tilfeller krever ikke ny informasjon eller hendelser noen revurdering. Beskriv hvorfor en
                      revurdering ikke er nødvendig.
                    </BodyShort>
                    <Textarea
                      label="Begrunnelse"
                      value={hendelsekommentar}
                      onChange={(e) => oppdaterKommentar(e.target.value)}
                    />
                    <ButtonGroup>
                      <Button
                        variant="secondary"
                        onClick={() => {
                          oppdaterKommentar('')
                          resetApiCall()
                          setOpen(false)
                        }}
                      >
                        Avbryt
                      </Button>
                      <Button onClick={lukkGrunnlagshendelseWrapper}>Lagre</Button>
                    </ButtonGroup>
                  </>
                )}
                {isPending(res) && <Loader />}
                {isFailureHandler({
                  apiResult: res,
                  errorMessage: 'Vi kunne ikke lukke hendelsen',
                })}
              </Modal.Body>
            </Modal>
          </div>
        </>
      }
    >
      <Table.DataCell>{grunnlagsendringsTittel[samsvarType]}</Table.DataCell>
      <Table.DataCell>{formaterStringDato(opprettet)}</Table.DataCell>
      <Table.DataCell></Table.DataCell>
    </Table.ExpandableRow>
  )
}

const BlueLink = styled(Link)`
  color: #0067c5 !important;
`

export default UhaandtertHendelse
