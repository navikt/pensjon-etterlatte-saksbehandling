import React, { useEffect } from 'react'
import { Alert, BodyShort, Button, Heading, Label, Panel, Radio, RadioGroup, Select, Tag } from '@navikt/ds-react'
import { useNyBehandling } from '~components/person/oppgavebehandling/useNyBehandling'
import { useAppDispatch } from '~store/Store'
import { useNavigate, useParams } from 'react-router-dom'
import { JaNei } from '~shared/types/ISvar'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import AvbrytOppgavebehandling from '~components/person/oppgavebehandling/AvbrytOppgavebehandling'
import { formaterFnr, formaterSakstype, formaterStringDato } from '~utils/formattering'
import { settBehandlingBehov, settBruker, settOppgave, settSamsvar } from '~store/reducers/NyBehandlingReducer'
import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { GYLDIG_FNR } from '~utils/fnr'
import Spinner from '~shared/Spinner'
import { hentGosysOppgave } from '~shared/api/oppgaverny'
import { DatoVelger } from '~shared/DatoVelger'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { hentSakForPerson } from '~shared/api/behandling'
import { FormWrapper } from '../styled'

export default function KontrollerOppgave() {
  const { bruker, oppgave, samsvar, journalpost, behandlingBehov } = useNyBehandling()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  const [oppgaveStatus, hentOppgave] = useApiCall(hentGosysOppgave)
  const [sakStatus, hentSak] = useApiCall(hentSakForPerson)

  const { id: oppgaveId } = useParams()

  const neste = () => navigate('../nybehandling', { relative: 'path' })

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

  const kanGaaVidere = samsvar === JaNei.JA && !!journalpost

  if (isPending(oppgaveStatus)) {
    return <Spinner visible label={'Henter oppgave'} />
  }

  return (
    <FormWrapper column>
      <h1>Kontroll</h1>

      {oppgave && (
        <>
          <Panel border>
            <Heading size={'medium'} spacing>
              Oppgave
              <br />
              <Tag variant={'success'} size={'small'}>
                {formaterSakstype(oppgave.sakType)}
              </Tag>
            </Heading>

            <InfoWrapper>
              <Info label={'ID'} tekst={oppgave.id} />
              <Info label={'Status'} tekst={oppgave.status} />
              <Info label={'Type'} tekst={oppgave.type} />
              <Info label={'Bruker'} tekst={formaterFnr(oppgave.fnr)} />
              <Info label={'Opprettet'} tekst={formaterStringDato(oppgave.opprettet)} />
              <Info label={'Frist'} tekst={formaterStringDato(oppgave.frist)} />
            </InfoWrapper>
            <hr />
            <div>
              <Label size="small" as={'p'} spacing>
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
              <Alert variant={'info'} inline>
                Bruker har allerede en eksisterende sak {formaterSakstype(oppgave.sakType).toLowerCase()} (id=
                {sakStatus.data.id}). Det vil dermed bli opprettet en ny behandling tilknyttet denne saken.
              </Alert>
            ) : (
              <Alert variant={'warning'} inline>
                Bruker har ingen sak av typen {formaterSakstype(oppgave.sakType).toLowerCase()}. Det vil bli opprettet
                en ny sak med behandling.
              </Alert>
            ))}

          <RadioGroup
            legend={'Samsvarer oppgaven med journalposten?'}
            size="small"
            onChange={(value) => dispatch(settSamsvar(value))}
            value={samsvar || null}
            error={undefined}
          >
            <div className="flex">
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </div>
          </RadioGroup>

          {samsvar === JaNei.NEI && (
            <Alert variant={'error'}>Oppgave og/eller journalpost må korrigeres i Gosys før du går videre</Alert>
          )}

          {samsvar === JaNei.JA && (
            <div>
              <Select
                label={'Hvilket språk/målform er søknaden?'}
                value={behandlingBehov?.spraak || ''}
                onChange={(e) => dispatch(settBehandlingBehov({ ...behandlingBehov, spraak: e.target.value }))}
              >
                <option>Velg ...</option>
                <option value={'nb'}>Bokmål</option>
                <option value={'nn'}>Nynorsk</option>
                <option value={'en'}>Engelsk</option>
              </Select>

              <DatoVelger
                label={'Mottatt dato'}
                description={'Datoen søknaden ble mottatt'}
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
            </div>
          )}
        </>
      )}

      <KnapperWrapper>
        <div>
          <Button variant="primary" className="button" onClick={neste} disabled={!kanGaaVidere}>
            Neste
          </Button>
        </div>
        <AvbrytOppgavebehandling />
      </KnapperWrapper>
    </FormWrapper>
  )
}
