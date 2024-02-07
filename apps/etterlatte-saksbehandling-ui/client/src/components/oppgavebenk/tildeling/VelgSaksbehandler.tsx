import React, { ReactNode, useState } from 'react'
import { Button, Dropdown, Label, UNSAFE_Combobox } from '@navikt/ds-react'
import { PersonCrossIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import {
  byttSaksbehandlerApi,
  fjernSaksbehandlerApi,
  Oppgavetype,
  Saksbehandler,
  tildelSaksbehandlerApi,
} from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'

interface Props {
  saksbehandler: Saksbehandler
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppgaveId: string
  sakId: number
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erRedigerbar: boolean
  versjon: number | null
  type: Oppgavetype
}

// TODO Ta inn følgende:
//  Saksbehandler objektet på det spesifike oppgaven
//  Fikse at det funker med resten av flyten :)
export const VelgSaksbehandler = ({
  saksbehandler,
  saksbehandlereIEnhet,
  erRedigerbar,
  oppgaveId,
  sakId,
  type,
  versjon,
  oppdaterTildeling,
}: Props): ReactNode => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<Saksbehandler | undefined>(saksbehandler)

  const [, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)
  const [, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  const onSaksbehandlerSelect = (saksbehandlerNavn: string, erValgt: boolean) => {
    if (erValgt) {
      const selectedSaksbehandler: Saksbehandler | undefined = saksbehandlereIEnhet.find(
        (behandler) => behandler.navn === saksbehandlerNavn
      )

      if (selectedSaksbehandler) {
        byttSaksbehandler(
          { oppgaveId, type, nysaksbehandler: { saksbehandler: selectedSaksbehandler.ident!, versjon } },
          (result) => {
            oppdaterTildeling(oppgaveId, selectedSaksbehandler.ident!, result.versjon)
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
        oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident, result.versjon)
        setValgtSaksbehandler({ navn: innloggetSaksbehandler.navn, ident: innloggetSaksbehandler.ident })
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
        oppdaterTildeling(oppgaveId, null, result.versjon)
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
            icon={valgtSaksbehandler ? <PersonPencilIcon /> : <PersonPlusIcon />}
            iconPosition="left"
            size="small"
            variant="tertiary"
            onClick={() => setOpenDropdown(true)}
          >
            {valgtSaksbehandler
              ? valgtSaksbehandler.navn === innloggetSaksbehandler.navn
                ? `${valgtSaksbehandler.navn} (meg)`
                : valgtSaksbehandler.navn
              : 'Ikke tildelt'}
          </Button>
          <Dropdown.Menu onClose={() => setOpenDropdown(false)}>
            <MenyWrapper>
              <div>
                <UNSAFE_Combobox
                  label="Velg saksbehandler"
                  options={saksbehandlereIEnhet.map((behandler) => behandler.navn!)}
                  onToggleSelected={onSaksbehandlerSelect}
                  selectedOptions={!!valgtSaksbehandler ? [valgtSaksbehandler.navn!] : []}
                />
                {!valgtSaksbehandler?.ident?.includes(innloggetSaksbehandler.ident) && (
                  <ValgButton variant="tertiary" size="xsmall" onClick={onTildelTilMeg}>
                    Tildel til meg
                  </ValgButton>
                )}
              </div>
              {valgtSaksbehandler && (
                <div>
                  <ValgButton
                    variant="secondary"
                    size="small"
                    onClick={onFjernTildeling}
                    icon={<PersonCrossIcon />}
                    iconPosition="right"
                  >
                    Fjern tildeling
                  </ValgButton>
                </div>
              )}
            </MenyWrapper>
          </Dropdown.Menu>
        </Dropdown>
      ) : (
        <SaksbehandlerWrapper>{saksbehandler.navn}</SaksbehandlerWrapper>
      )}
    </div>
  )
}

const MenyWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
`

const ValgButton = styled(Button)`
  margin-top: 0.75rem;
`

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`
