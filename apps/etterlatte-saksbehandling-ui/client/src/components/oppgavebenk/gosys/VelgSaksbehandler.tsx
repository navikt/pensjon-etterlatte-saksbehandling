import React, { ReactNode, useState } from 'react'
import { Button, Dropdown, Label, UNSAFE_Combobox } from '@navikt/ds-react'
import { PersonCrossIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { isPending } from '~shared/api/apiUtils'
import { OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { GosysOppgave } from '~shared/types/Gosys'
import { OppdatertOppgaveversjonResponseDto, tildelSaksbehandlerApi } from '~shared/api/gosys'

interface Props {
  oppgave: GosysOppgave
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (versjon: OppdatertOppgaveversjonResponseDto, saksbehandler?: OppgaveSaksbehandler) => void
}

export const VelgSaksbehandler = ({ saksbehandlereIEnhet, oppgave, oppdaterTildeling }: Props): ReactNode => {
  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const { id: oppgaveId, versjon, saksbehandler, enhet } = oppgave
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erRedigerbar = enhetErSkrivbar(enhet, innloggetSaksbehandler.skriveEnheter)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<OppgaveSaksbehandler | undefined>(saksbehandler)
  const [tildelResult, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const vedValgtSaksbehandler = (saksbehandlerNavn: string, erValgt: boolean) => {
    if (erValgt) {
      const selectedSaksbehandler: Saksbehandler | undefined = saksbehandlereIEnhet.find(
        (behandler) => behandler.navn === saksbehandlerNavn
      )

      if (selectedSaksbehandler) {
        tildelOppgave(selectedSaksbehandler)
      }
    }
  }

  const tildelOppgave = (saksbehandler?: OppgaveSaksbehandler) => {
    tildelSaksbehandler(
      { oppgaveId, nysaksbehandler: { saksbehandler: saksbehandler?.ident || '', versjon }, enhetsnr: enhet },
      (oppgaveVersjon) => {
        setValgtSaksbehandler(saksbehandler)
        oppdaterTildeling(oppgaveVersjon, saksbehandler)
        setOpenDropdown(false)
      },
      (error) => {
        console.log(error)
      }
    )
  }

  return (
    <div>
      {erRedigerbar ? (
        <Dropdown open={openDropdown}>
          <Button
            as={Dropdown.Toggle}
            icon={valgtSaksbehandler?.ident ? <PersonPencilIcon aria-hidden /> : <PersonPlusIcon aria-hidden />}
            iconPosition="left"
            size="small"
            variant="tertiary"
            onClick={() => setOpenDropdown(true)}
            loading={isPending(tildelResult)}
          >
            {valgtSaksbehandler?.ident === innloggetSaksbehandler.ident
              ? `${innloggetSaksbehandler.navn} (meg)`
              : valgtSaksbehandler?.ident || 'Ikke tildelt'}
          </Button>
          <DropdownMeny onClose={() => setOpenDropdown(false)}>
            <div>
              <VelgSaksbehandlerCombobox
                label="Velg saksbehandler"
                options={saksbehandlereIEnhet.map((behandler) => behandler.navn!)}
                onToggleSelected={vedValgtSaksbehandler}
                selectedOptions={!!valgtSaksbehandler ? [valgtSaksbehandler.navn!] : []}
                isLoading={isPending(tildelResult)}
              />
              {valgtSaksbehandler?.ident !== innloggetSaksbehandler.ident && (
                <ValgButton
                  variant="tertiary"
                  size="xsmall"
                  onClick={() => tildelOppgave(innloggetSaksbehandler)}
                  loading={isPending(tildelResult)}
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
                  onClick={() => tildelOppgave(undefined)}
                  icon={<PersonCrossIcon aria-hidden />}
                  iconPosition="right"
                  loading={isPending(tildelResult)}
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
