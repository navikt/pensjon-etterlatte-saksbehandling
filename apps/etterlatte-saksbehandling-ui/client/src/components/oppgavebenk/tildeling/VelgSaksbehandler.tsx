import React, { ReactNode, useEffect, useState } from 'react'
import {
  Alert,
  BodyShort,
  Box,
  Button,
  Checkbox,
  Dropdown,
  HStack,
  Label,
  Modal,
  Table,
  UNSAFE_Combobox,
  VStack,
} from '@navikt/ds-react'
import { PersonCrossIcon, PersonIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { fjernSaksbehandlerApi, hentOppgaverMedGruppeId, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { isPending, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { formaterDato } from '~utils/formatering/dato'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { OppgavetypeTag } from '~shared/tags/OppgavetypeTag'

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
  const { id: oppgaveId, status } = oppgave
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erRedigerbar =
    erOppgaveRedigerbar(status) && enhetErSkrivbar(oppgave.enhet, innloggetSaksbehandler.skriveEnheter)
  const saksbehandler = mapSaksbehandler(oppgave)

  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [valgteOppgaver, setValgteOppgaver] = useState<OppgaveDTO[]>([])

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<Saksbehandler | undefined>(saksbehandler)

  const [oppgaverResult, apiHentOppgaver] = useApiCall(hentOppgaverMedGruppeId)
  const [fjernSaksbehandlerResult, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [byttSaksbehandlerResult, byttSaksbehandler, resetByttSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const tildel = (saksbehandler: Saksbehandler) => {
    const oppgaverSomSkalTildeles = [...valgteOppgaver, oppgave]

    oppgaverSomSkalTildeles.forEach((valgtOppgave) =>
      byttSaksbehandler(
        { oppgaveId: valgtOppgave.id, saksbehandlerIdent: saksbehandler.ident },
        () => {
          oppdaterTildeling(valgtOppgave, saksbehandler)
          setValgtSaksbehandler(saksbehandler)
          setOpenDropdown(false)
          resetByttSaksbehandler()
        },
        (error) => console.log(error)
      )
    )

    console.log('ferdig')
  }

  const startTildeling = (saksbehandler: Saksbehandler, tildelOverstyr?: boolean) => {
    if (valgteOppgaver.length && !tildelOverstyr) {
      setIsModalOpen(true)
      setOpenDropdown(false)
      setValgtSaksbehandler(saksbehandler)

      /*apiHentOppgaver({ gruppeId: oppgave.gruppeId, type: oppgave.type }, (oppgaver) => {
        if (oppgaver) {
        } else {
          tildel(saksbehandler)
        }
      })*/
    } else {
      tildel(saksbehandler)
    }
  }

  const onSaksbehandlerSelect = (saksbehandlerNavn: string, erValgt: boolean) => {
    if (erValgt) {
      const selectedSaksbehandler: Saksbehandler | undefined = saksbehandlereIEnhet.find(
        (behandler) => behandler.navn === saksbehandlerNavn
      )

      if (selectedSaksbehandler) {
        startTildeling(selectedSaksbehandler)
      }
    }
  }

  const onTildelTilMeg = () => startTildeling(innloggetSaksbehandler)

  const onFjernTildeling = () => {
    fjernSaksbehandler(
      { oppgaveId },
      () => {
        oppdaterTildeling(oppgave, null)
        setValgtSaksbehandler(undefined)
        setOpenDropdown(false)
      },
      (error) => console.log(error)
    )
  }

  const lukkGrupperteOppgaver = () => {
    setIsModalOpen(false)
    setValgtSaksbehandler(undefined)
    setOpenDropdown(false)
  }

  useEffect(() => {
    if (openDropdown && oppgave.gruppeId) {
      apiHentOppgaver({ gruppeId: oppgave.gruppeId, type: oppgave.type }, (oppgaver) => {
        if (oppgaver.length > 1) {
          setValgteOppgaver(oppgaver)
          // setIsModalOpen(true)
          // setOpenDropdown(false)
          // setValgtSaksbehandler(saksbehandler)
        }
      })
    }
  }, [openDropdown])

  if (isModalOpen) {
    return (
      <Modal
        open={isModalOpen}
        aria-labelledby="modal-heading"
        onClose={lukkGrupperteOppgaver}
        style={{ minWidth: '60rem' }}
        header={{ heading: 'Tildel oppgaver' }}
      >
        <Modal.Body>
          {mapResult(oppgaverResult, {
            success: (oppgaver) =>
              oppgaver.length > 1 && (
                <Box width="">
                  <VStack gap="4">
                    <Alert variant="info" inline>
                      <BodyShort spacing>Det finnes andre oppgaver tilknyttet samme avdød.</BodyShort>

                      <BodyShort>
                        Ønsker du å tildele{' '}
                        {!!valgtSaksbehandler && valgtSaksbehandler?.ident !== innloggetSaksbehandler.ident
                          ? valgtSaksbehandler.navn
                          : 'deg'}{' '}
                        alle oppgavene?
                      </BodyShort>
                    </Alert>

                    <Table size="small">
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell />
                          <Table.HeaderCell>SaksID</Table.HeaderCell>
                          <Table.HeaderCell>Reg.dato</Table.HeaderCell>
                          <Table.HeaderCell>Frist</Table.HeaderCell>
                          <Table.HeaderCell>Fnr.</Table.HeaderCell>
                          <Table.HeaderCell>Ytelse</Table.HeaderCell>
                          <Table.HeaderCell>Oppgavetype</Table.HeaderCell>
                          <Table.HeaderCell>Tildelt</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {oppgaver.map((oppgave: OppgaveDTO) => (
                          <Table.Row key={`${oppgave.id}-${oppgave.gruppeId}`}>
                            <Table.DataCell>
                              <Checkbox
                                checked={valgteOppgaver.map((o) => o.id).includes(oppgave.id)}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    setValgteOppgaver([...valgteOppgaver, oppgave])
                                  } else {
                                    setValgteOppgaver([...valgteOppgaver.filter((o) => o.id !== oppgave.id)])
                                  }
                                }}
                              >
                                {undefined} {/* Checkbox-komponenten krever innhold */}
                              </Checkbox>
                            </Table.DataCell>
                            <Table.DataCell>{oppgave.sakId}</Table.DataCell>
                            <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
                            <Table.DataCell>{formaterDato(oppgave.frist)}</Table.DataCell>
                            <Table.DataCell>
                              {oppgave.fnr ? <PersonLink fnr={oppgave.fnr}>{oppgave.fnr}</PersonLink> : 'Mangler'}
                            </Table.DataCell>
                            <Table.DataCell>
                              <SakTypeTag sakType={oppgave.sakType} kort />
                            </Table.DataCell>
                            <Table.DataCell>
                              <OppgavetypeTag oppgavetype={oppgave.type} />
                            </Table.DataCell>
                            <Table.DataCell>
                              {oppgave.saksbehandler ? oppgave.saksbehandler.navn : 'Ikke tildelt'}
                            </Table.DataCell>
                          </Table.Row>
                        ))}
                      </Table.Body>
                    </Table>

                    <HStack gap="4" justify="end">
                      <Button
                        variant="secondary"
                        onClick={lukkGrupperteOppgaver}
                        disabled={isPending(byttSaksbehandlerResult)}
                      >
                        Avbryt
                      </Button>
                      <Button
                        variant="primary"
                        onClick={() => tildel(valgtSaksbehandler!!)}
                        loading={isPending(byttSaksbehandlerResult)}
                      >
                        {oppgaver.length === valgteOppgaver.length ? 'Ja, tildel alle' : 'Tildel valgte'}
                      </Button>
                    </HStack>
                  </VStack>
                </Box>
              ),
          })}
        </Modal.Body>
      </Modal>
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
                      Tildel meg
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

              {mapSuccess(
                oppgaverResult,
                (oppgaver) =>
                  oppgaver.length > 1 && (
                    <Alert variant="info" inline>
                      {oppgaver.length} oppgaver på samme avdød
                    </Alert>
                  )
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
