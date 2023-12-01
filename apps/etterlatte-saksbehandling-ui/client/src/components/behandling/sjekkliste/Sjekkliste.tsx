import styled from 'styled-components'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { oppdaterSjekkliste, oppdaterSjekklisteItem } from '~shared/api/sjekkliste'
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  Checkbox,
  ConfirmationPanel,
  Heading,
  Link,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import { ConfigContext } from '~clientConfig'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { ISjekklisteItem } from '~shared/types/Sjekkliste'
import { ApiErrorAlert } from '~ErrorBoundary'
import debounce from 'lodash/debounce'
import { useSjekkliste, useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { updateSjekkliste, updateSjekklisteItem } from '~store/reducers/SjekklisteReducer'
import { PencilIcon } from '@navikt/aksel-icons'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { useSelectorSaksbehandlerGjeldendeOppgaveBehandling } from '~store/selectors/useSelectorSaksbehandlerGjeldendeOppgaveBehandling'

import { isFailure } from '~shared/api/apiUtils'

export const Sjekkliste = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [redigerbar, setRedigerbar] = useState<boolean>(false)
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)
  const saksbehandlerGjeldendeOppgave = useSelectorSaksbehandlerGjeldendeOppgaveBehandling()

  const dispatch = useAppDispatch()

  const [oppdaterSjekklisteResult, oppdaterSjekklisteApi] = useApiCall(oppdaterSjekkliste)
  const sjekkliste = useSjekkliste()

  const sjekklisteValideringsfeil = useSjekklisteValideringsfeil().length > 0

  const configContext = useContext(ConfigContext)

  const rutinerBP =
    'https://navno.sharepoint.com/:w:/r/sites/Pilot-Doffen/Delte dokumenter/General/Rutiner for sjekklister i Gjenny/BP - Rutine sjekkliste førstegangsbehandling.docx?web=1'
  const rutinerOMS =
    'https://navno.sharepoint.com/:w:/r/sites/Pilot-Doffen/Delte dokumenter/General/Rutiner for sjekklister i Gjenny/OMS - Rutine sjekkliste førstegangsbehandling .docx?web=1'

  const dispatchUpdatedItem = (item: ISjekklisteItem) => {
    dispatch(updateSjekklisteItem(item))
  }

  const fireOpppdater = useMemo(() => debounce(oppdaterSjekklisteApi, 1500), [])

  useEffect(() => {
    const erSammeIdent = saksbehandlerGjeldendeOppgave === innloggetSaksbehandler.ident
    setOppgaveErTildeltInnloggetBruker(erSammeIdent)
    setRedigerbar(behandlingErRedigerbar(behandling.status) && erSammeIdent)
  }, [])

  return (
    <SidebarPanel border id="sjekklistePanel">
      {sjekklisteValideringsfeil && (
        <Alert variant="error">Før du kan sende til attestering må du bekrefte at alle punkter er gjennomgått</Alert>
      )}

      <Heading spacing size="small">
        Sjekkliste
      </Heading>

      {isFailure(oppdaterSjekklisteResult) && <ApiErrorAlert>Oppdateringen av sjekklista feilet</ApiErrorAlert>}

      {sjekkliste && (
        <>
          <BodyLong>
            Gjennomgå alle punktene og marker de som krever handling.
            <Link href={behandling.sakType == SakType.BARNEPENSJON ? rutinerBP : rutinerOMS} target="_blank">
              Her finner du rutine til punktene.
            </Link>
          </BodyLong>

          {sjekkliste?.sjekklisteItems.map((item) => (
            <SjekklisteItem
              key={item.id}
              item={item}
              behandlingId={behandling.id}
              redigerbar={redigerbar}
              onUpdated={dispatchUpdatedItem}
            />
          ))}

          <HMargin>
            <Link
              href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker!.foedselsnummer}`}
              target="_blank"
            >
              Personoversikt i Gosys
            </Link>
          </HMargin>

          <VStack gap="4">
            <Textarea
              label="Kommentar"
              name="Kommentar"
              description="Skriv ned annet som ikke kommer frem i sjekklisten."
              value={sjekkliste.kommentar || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  kommentar: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              rows={3}
              readOnly={!redigerbar}
            />

            <TextField
              label="Adresse brevet er sendt til"
              name="AdresseBrevforsendelse"
              value={sjekkliste.adresseForBrev || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  adresseForBrev: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              readOnly={!redigerbar}
            />

            <TextField
              label="Kontonummer registrert"
              name="KontonummerRegistrert"
              value={sjekkliste.kontonrRegistrert || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  kontonrRegistrert: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              readOnly={!redigerbar}
            />

            {behandling.sakType == SakType.BARNEPENSJON && (
              <TextField
                label="Ønsket skattetrekk"
                name="OnsketSkattetrekk"
                value={sjekkliste.onsketSkattetrekk || undefined}
                onChange={(e) => {
                  const isNumberOrEmpty = /^\d*$/.test(e.target.value)
                  if (isNumberOrEmpty) {
                    const oppdatert = {
                      ...sjekkliste,
                      onsketSkattetrekk: e.target.value === '' ? undefined : Number(e.target.value),
                    }
                    dispatch(updateSjekkliste(oppdatert))
                    fireOpppdater(oppdatert)
                  }
                }}
                readOnly={!redigerbar}
              />
            )}

            {redigerbar && (
              <ConfirmationPanel
                name="BekreftGjennomgang"
                checked={sjekkliste.bekreftet}
                label="Jeg bekrefter at alle punkter er gjennomgått"
                error={sjekklisteValideringsfeil && !sjekkliste.bekreftet && 'Feltet må hukes av for å ferdigstilles'}
                onChange={(e) => {
                  const oppdatert = {
                    ...sjekkliste,
                    bekreftet: e.currentTarget.checked,
                  }
                  dispatch(updateSjekkliste(oppdatert))
                  fireOpppdater(oppdatert)
                }}
              />
            )}

            {!redigerbar && (
              <Checkbox checked={sjekkliste.bekreftet} readOnly={true}>
                Jeg bekrefter at alle punkter er gjennomgått
              </Checkbox>
            )}
          </VStack>

          {!redigerbar && behandling.status == IBehandlingStatus.FATTET_VEDTAK && oppgaveErTildeltInnloggetBruker && (
            <Button variant="tertiary" icon={<PencilIcon />} onClick={() => setRedigerbar(true)}>
              Rediger
            </Button>
          )}
        </>
      )}
      {!sjekkliste && <BodyShort>Ikke registrert</BodyShort>}
    </SidebarPanel>
  )
}

const SjekklisteItem = ({
  item,
  behandlingId,
  redigerbar,
  onUpdated,
}: {
  item: ISjekklisteItem
  behandlingId: string
  redigerbar: boolean
  onUpdated: (item: ISjekklisteItem) => void
}) => {
  const [avkrysset, setAvkrysset] = useState<boolean>(item.avkrysset)
  const [itemUpdateResult, oppdaterItem] = useApiCall(oppdaterSjekklisteItem)

  return (
    <>
      {isFailure(itemUpdateResult) && <ApiErrorAlert>En feil oppsto ved oppdatering av sjekklista</ApiErrorAlert>}

      <Checkbox
        checked={avkrysset}
        readOnly={!redigerbar}
        onChange={(event) => {
          const checked = event.currentTarget.checked
          setAvkrysset(checked)
          oppdaterItem({ behandlingId, item, checked }, (oppdatert) => onUpdated(oppdatert))
        }}
      >
        {item.beskrivelse}
      </Checkbox>
    </>
  )
}

const HMargin = styled.div`
  margin-top: 1em;
  margin-bottom: 1em;
`
