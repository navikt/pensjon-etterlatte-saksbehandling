import React, { ReactNode, useState } from 'react'
import { Button, Dropdown, HStack, Label, UNSAFE_Combobox, VStack } from '@navikt/ds-react'
import { PersonCrossIcon, PersonIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { tildelSaksbehandlerApi, fjernSaksbehandlerApi } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { isPending } from '~shared/api/apiUtils'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
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
  const versjon = null
  const { sakId, id: oppgaveId, status } = oppgave
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erRedigerbar =
    erOppgaveRedigerbar(status) && enhetErSkrivbar(oppgave.enhet, innloggetSaksbehandler.skriveEnheter)
  const saksbehandler = mapSaksbehandler(oppgave)

  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<Saksbehandler | undefined>(saksbehandler)

  const [fjernSaksbehandlerResult, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [byttSaksbehandlerResult, byttSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const onSaksbehandlerSelect = (saksbehandlerNavn: string, erValgt: boolean) => {
    if (erValgt) {
      const selectedSaksbehandler: Saksbehandler | undefined = saksbehandlereIEnhet.find(
        (behandler) => behandler.navn === saksbehandlerNavn
      )

      if (selectedSaksbehandler) {
        byttSaksbehandler(
          { oppgaveId, nysaksbehandler: { saksbehandler: selectedSaksbehandler.ident!, versjon } },
          () => {
            oppdaterTildeling(oppgave, selectedSaksbehandler)
            setValgtSaksbehandler(selectedSaksbehandler)
            setOpenDropdown(false)
          },
          (error) => console.log(error)
        )
      }
    }
  }

  const onTildelTilMeg = () => {
    byttSaksbehandler(
      { oppgaveId, nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon } },
      () => {
        oppdaterTildeling(oppgave, innloggetSaksbehandler)
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
      { oppgaveId, sakId },
      () => {
        oppdaterTildeling(oppgave, null)
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
            loading={isPending(byttSaksbehandlerResult)}
          >
            {valgtSaksbehandler?.navn
              ? `${valgtSaksbehandler.navn} ${valgtSaksbehandler.ident === innloggetSaksbehandler.ident ? '(meg)' : ''}`
              : 'Ikke tildelt'}
          </Button>
          <DropdownMeny onClose={() => setOpenDropdown(false)}>
            <VStack gap="3">
              <>
                <VelgSaksbehandlerCombobox
                  label="Velg saksbehandler"
                  options={saksbehandlereIEnhet.map((behandler) => behandler.navn!)}
                  onToggleSelected={onSaksbehandlerSelect}
                  selectedOptions={!!valgtSaksbehandler ? [valgtSaksbehandler.navn!] : []}
                  isLoading={isPending(byttSaksbehandlerResult)}
                />
                {!valgtSaksbehandler?.ident?.includes(innloggetSaksbehandler.ident) && (
                  <div>
                    <Button
                      variant="tertiary"
                      size="xsmall"
                      onClick={onTildelTilMeg}
                      loading={isPending(byttSaksbehandlerResult)}
                    >
                      Tildel til meg
                    </Button>
                  </div>
                )}
              </>
              {valgtSaksbehandler?.ident && (
                <div>
                  <Button
                    variant="secondary"
                    size="small"
                    onClick={onFjernTildeling}
                    icon={<PersonCrossIcon />}
                    iconPosition="right"
                    loading={isPending(fjernSaksbehandlerResult)}
                  >
                    Fjern tildeling
                  </Button>
                </div>
              )}
            </VStack>
          </DropdownMeny>
        </Dropdown>
      ) : (
        <SaksbehandlerNavnHStack gap="2" align="center">
          <PersonIcon width="1.5rem" height="1.5rem" />
          <Label size="small" textColor="subtle">
            {saksbehandler ? saksbehandler.navn : 'Navn mangler'}
          </Label>
        </SaksbehandlerNavnHStack>
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

const VelgSaksbehandlerCombobox = styled(UNSAFE_Combobox)`
  width: 20rem;
`

const SaksbehandlerNavnHStack = styled(HStack)`
  padding-left: 0.6rem;
`
