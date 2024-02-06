import React, { ReactNode, useState } from 'react'
import { Button, Dropdown, Label, UNSAFE_Combobox } from '@navikt/ds-react'
import { PersonCrossIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { byttSaksbehandlerApi, fjernSaksbehandlerApi, Oppgavetype, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'

interface Props {
  saksbehandlere: Array<string>
  saksbehandler: string | null
  oppgaveId: string
  sakId: number
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erRedigerbar: boolean
  versjon: number | null
  type: Oppgavetype
}

export const VelgSaksbehandler = ({
  saksbehandlere,
  saksbehandler,
  erRedigerbar,
  oppgaveId,
  sakId,
  type,
  versjon,
  oppdaterTildeling,
}: Props): ReactNode => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<string | null>(saksbehandler)

  const [, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)
  const [, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  const onSaksbehandlerSelect = (saksbehandler: string, erValgt: boolean) => {
    if (erValgt) {
      byttSaksbehandler(
        { oppgaveId, type, nysaksbehandler: { saksbehandler, versjon } },
        (result) => {
          oppdaterTildeling(oppgaveId, saksbehandler, result.versjon)
          setValgtSaksbehandler(saksbehandler)
          setOpenDropdown(false)
        },
        (error) => console.log(error)
      )
    }
  }

  const onTildelTilMeg = () => {
    tildelSaksbehandler(
      { oppgaveId, type, nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon } },
      (result) => {
        oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident, result.versjon)
        setValgtSaksbehandler(innloggetSaksbehandler.ident)
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
        setValgtSaksbehandler('')
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
              ? valgtSaksbehandler === innloggetSaksbehandler.ident
                ? `${valgtSaksbehandler} (meg)`
                : valgtSaksbehandler
              : 'Ikke tildelt'}
          </Button>
          <Dropdown.Menu onClose={() => setOpenDropdown(false)}>
            <MenyWrapper>
              <div>
                <UNSAFE_Combobox
                  label="Velg saksbehandler"
                  options={saksbehandlere}
                  onToggleSelected={onSaksbehandlerSelect}
                  selectedOptions={!!valgtSaksbehandler ? [valgtSaksbehandler] : []}
                />
                {!valgtSaksbehandler?.includes(innloggetSaksbehandler.ident) && (
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
        <SaksbehandlerWrapper>{saksbehandler}</SaksbehandlerWrapper>
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
