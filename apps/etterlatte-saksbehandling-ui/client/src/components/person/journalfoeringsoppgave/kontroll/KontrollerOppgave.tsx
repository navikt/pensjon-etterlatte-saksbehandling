import React, { useEffect, useState } from 'react'
import {
  Alert,
  BodyShort,
  Button,
  ErrorMessage,
  Heading,
  Label,
  Panel,
  Radio,
  RadioGroup,
  Select,
  Tag,
} from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { useNavigate, useParams } from 'react-router-dom'
import { JaNei } from '~shared/types/ISvar'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterFnr, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { settBehandlingBehov, settBruker, settOppgave, settSamsvar } from '~store/reducers/JournalfoeringOppgaveReducer'
import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { GYLDIG_FNR } from '~utils/fnr'
import Spinner from '~shared/Spinner'
import { hentGosysOppgave } from '~shared/api/oppgaverny'
import { DatoVelger } from '~shared/DatoVelger'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { hentSakForPerson } from '~shared/api/behandling'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { FlexRow } from '~shared/styled'
import { FristWrapper } from '~components/nyoppgavebenk/Oppgavelista'

export default function KontrollerOppgave() {
  const state = useJournalfoeringOppgave()
  const { bruker, oppgave, samsvar, journalpost, behandlingBehov } = state
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  const [error, setError] = useState<string | undefined>()

  const [oppgaveStatus, hentOppgave] = useApiCall(hentGosysOppgave)
  const [sakStatus, hentSak] = useApiCall(hentSakForPerson)

  const { id: oppgaveId } = useParams()

  const neste = () => {
    if (!journalpost) {
      setError('Journalpost må være valgt')
    } else if (samsvar !== JaNei.JA) {
      setError('Samsvar er påkrevd')
    } else if (!behandlingBehov?.mottattDato) {
      setError('Mottatt dato må være satt')
    } else if (!behandlingBehov?.spraak) {
      setError('Språk for søknaden må være valgt')
    } else {
      navigate('../nybehandling', { relative: 'path' })
    }
  }

  useEffect(() => {
    if (!oppgave) {
      hentOppgave(Number(oppgaveId), (oppgave) => {
        dispatch(settOppgave(oppgave))
        dispatch(settBruker(oppgave.fnr))
        dispatch(
          settBehandlingBehov({
            ...behandlingBehov,
            sakType: oppgave.sakType,
            persongalleri: { ...behandlingBehov?.persongalleri, soeker: oppgave.fnr },
          })
        )
      })
    }
  }, [oppgaveId])

  useEffect(() => {
    if (GYLDIG_FNR(bruker) && !!oppgave) {
      hentSak({ fnr: bruker!!, type: oppgave!!.sakType })
    }
  }, [bruker, oppgave])

  if (isPending(oppgaveStatus)) {
    return <Spinner visible label="Henter oppgave" />
  }

  return (
    <FormWrapper column>
      <Heading size="large">Kontroll</Heading>

      {oppgave && (
        <>
          <Panel border>
            <Heading size="medium" spacing>
              Gosysoppgave
              <br />
              <Tag variant="success" size="small">
                {formaterSakstype(oppgave.sakType)}
              </Tag>
            </Heading>

            <InfoWrapper>
              <Info label="ID" tekst={oppgave.id} />
              <Info label="Status" tekst={oppgave.status} />
              <Info label="Type" tekst={oppgave.type} />
              <Info label="Bruker" tekst={formaterFnr(oppgave.fnr)} />
              <Info label="Opprettet" tekst={formaterStringDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={<FristWrapper dato={oppgave.frist} />} />
            </InfoWrapper>
            <hr />
            <div>
              <Label size="small" as="p" spacing>
                Beskrivelseshistorikk
              </Label>
              <BodyShort>{oppgave?.beskrivelse || 'Ingen beskrivelse'}</BodyShort>
            </div>
          </Panel>

          {isPending(sakStatus) && (
            <Spinner visible label={`Sjekker om bruker har sak for ${formaterSakstype(oppgave.sakType)}`} />
          )}
          {isSuccess(sakStatus) &&
            (sakStatus.data ? (
              <Alert variant="info" inline>
                Bruker har allerede en eksisterende sak {formaterSakstype(oppgave.sakType).toLowerCase()} (id=
                {sakStatus.data.id}). Det vil dermed bli opprettet en ny behandling tilknyttet denne saken.
              </Alert>
            ) : (
              <Alert variant="warning" inline>
                Bruker har ingen sak av typen {formaterSakstype(oppgave.sakType).toLowerCase()}. Det vil bli opprettet
                en ny sak med behandling.
              </Alert>
            ))}

          <RadioGroup
            legend="Samsvarer oppgaven med journalposten?"
            size="small"
            onChange={(value) => dispatch(settSamsvar(value))}
            value={samsvar || null}
          >
            <div className="flex">
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </div>
          </RadioGroup>

          {samsvar === JaNei.NEI && (
            <Alert variant="error">Oppgave og/eller journalpost må korrigeres i Gosys før du går videre</Alert>
          )}

          {samsvar === JaNei.JA && (
            <>
              <Select
                label="Hva skal språket/målform være?"
                value={behandlingBehov?.spraak || ''}
                onChange={(e) => dispatch(settBehandlingBehov({ ...behandlingBehov, spraak: e.target.value }))}
              >
                <option>Velg ...</option>
                <option value="nb">Bokmål</option>
                <option value="nn">Nynorsk</option>
                <option value="en">Engelsk</option>
              </Select>

              <DatoVelger
                label="Mottatt dato"
                description="Datoen søknaden ble mottatt"
                value={behandlingBehov?.mottattDato ? new Date(behandlingBehov?.mottattDato) : null}
                onChange={(mottattDato) =>
                  dispatch(
                    settBehandlingBehov({
                      ...behandlingBehov,
                      mottattDato: mottattDato?.toISOString(),
                    })
                  )
                }
              />
            </>
          )}
        </>
      )}

      {error && <ErrorMessage>{error}</ErrorMessage>}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="primary" onClick={neste} disabled={samsvar === JaNei.NEI}>
            Neste
          </Button>
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
