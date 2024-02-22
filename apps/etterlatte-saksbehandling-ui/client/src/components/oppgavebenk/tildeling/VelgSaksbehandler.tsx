import React, { ReactNode, useEffect, useState } from 'react'
import { Button, Dropdown, Label, UNSAFE_Combobox } from '@navikt/ds-react'
import { PersonCrossIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import {
  byttSaksbehandlerApi,
  erOppgaveRedigerbar,
  fjernSaksbehandlerApi,
  OppgaveDTO,
  OppgaveSaksbehandler,
  tildelSaksbehandlerApi,
} from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Saksbehandler } from '~shared/types/saksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  oppgave: OppgaveDTO
}

const mapSaksbehandler = (oppgave: OppgaveDTO): Saksbehandler | undefined =>
  oppgave.saksbehandler
    ? {
        ident: oppgave.saksbehandler.ident,
        navn: oppgave.saksbehandler.navn || oppgave.saksbehandler.ident,
      }
    : undefined

export const VelgSaksbehandler = ({ saksbehandlereIEnhet, oppdaterTildeling, oppgave }: Props): ReactNode => {
  const { sakId, id: oppgaveId, type, versjon, status } = oppgave
  const erRedigerbar = erOppgaveRedigerbar(status)
  const saksbehandler = mapSaksbehandler(oppgave)

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<Saksbehandler | undefined>(saksbehandler)

  useEffect(() => {
    if (valgtSaksbehandler?.ident !== oppgave.saksbehandler?.ident) {
      setValgtSaksbehandler(saksbehandler)
    }
  }, [oppgave.saksbehandler])

  const [tildelSaksbehandlerResult, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)
  const [fjernSaksbehandlerResult, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [byttSaksbehandlerResult, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  const onSaksbehandlerSelect = (saksbehandlerNavn: string, erValgt: boolean) => {
    if (erValgt) {
      const selectedSaksbehandler: Saksbehandler | undefined = saksbehandlereIEnhet.find(
        (behandler) => behandler.navn === saksbehandlerNavn
      )

      if (selectedSaksbehandler) {
        byttSaksbehandler(
          { oppgaveId, type, nysaksbehandler: { saksbehandler: selectedSaksbehandler.ident!, versjon } },
          (result) => {
            oppdaterTildeling(oppgave, selectedSaksbehandler, result.versjon)
            setValgtSaksbehandler(saksbehandler)
            setOpenDropdown(false)
          },
          (error) => console.log(error)
        )
      }
    }
  }

  const onTildelTilMeg = () => {
    tildelSaksbehandler(
      { oppgaveId, type, nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon } },
      (result) => {
        oppdaterTildeling(oppgave, innloggetSaksbehandler, result.versjon)
        setValgtSaksbehandler({
          ident: innloggetSaksbehandler.ident,
          navn: innloggetSaksbehandler.navn,
        })
        setOpenDropdown(false)
      },
      (error) => {
        console.log(error)
      }
    )
  }

  const onFjernTildeling = () => {
    fjernSaksbehandler(
      { oppgaveId, sakId, type, versjon },
      (result) => {
        oppdaterTildeling(oppgave, null, result.versjon)
        setValgtSaksbehandler(undefined)
        setOpenDropdown(false)
      },
      (error) => console.log(error)
    )
  }

  return (
    <div>
      {erRedigerbar ? (
        <Dropdown open={openDropdown}>
          <Button
            as={Dropdown.Toggle}
            icon={valgtSaksbehandler?.ident ? <PersonPencilIcon /> : <PersonPlusIcon />}
            iconPosition="left"
            size="small"
            variant="tertiary"
            onClick={() => setOpenDropdown(true)}
            loading={byttSaksbehandlerResult.status === 'pending'}
          >
            {valgtSaksbehandler?.navn
              ? `${valgtSaksbehandler.navn} ${valgtSaksbehandler.ident === innloggetSaksbehandler.ident ? '(meg)' : ''}`
              : 'Ikke tildelt'}
          </Button>
          <DropdownMeny onClose={() => setOpenDropdown(false)}>
            <div>
              <VelgSaksbehandlerCombobox
                label="Velg saksbehandler"
                options={saksbehandlereIEnhet.map((behandler) => behandler.navn!)}
                onToggleSelected={onSaksbehandlerSelect}
                selectedOptions={!!valgtSaksbehandler ? [valgtSaksbehandler.navn!] : []}
                isLoading={byttSaksbehandlerResult.status === 'pending'}
              />
              {!valgtSaksbehandler?.ident?.includes(innloggetSaksbehandler.ident) && (
                <ValgButton
                  variant="tertiary"
                  size="xsmall"
                  onClick={onTildelTilMeg}
                  loading={tildelSaksbehandlerResult.status === 'pending'}
                >
                  Tildel til meg
                </ValgButton>
              )}
            </div>
            {valgtSaksbehandler?.ident && (
              <div>
                <ValgButton
                  variant="secondary"
                  size="small"
                  onClick={onFjernTildeling}
                  icon={<PersonCrossIcon />}
                  iconPosition="right"
                  loading={fjernSaksbehandlerResult.status === 'pending'}
                >
                  Fjern tildeling
                </ValgButton>
              </div>
            )}
          </DropdownMeny>
        </Dropdown>
      ) : (
        <SaksbehandlerWrapper>{saksbehandler ? saksbehandler.navn : 'Navn mangler'}</SaksbehandlerWrapper>
      )}
    </div>
  )
}

const DropdownMeny = styled(Dropdown.Menu)`
  position: absolute;
  overflow: visible;
  min-width: fit-content;
  max-width: fit-content;
`

const ValgButton = styled(Button)`
  margin-top: 0.75rem;
`

const VelgSaksbehandlerCombobox = styled(UNSAFE_Combobox)`
  width: 20rem;
`

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`
