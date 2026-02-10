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
import debounce from 'lodash/debounce'
import { useSjekkliste, useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import { useAppDispatch } from '~store/Store'
import { updateSjekkliste, updateSjekklisteItem } from '~store/reducers/SjekklisteReducer'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

export const Sjekkliste = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [redigerbar, setRedigerbar] = useState<boolean>(false)
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)
  const oppgave = useSelectorOppgaveUnderBehandling()
  const soeker = usePersonopplysninger()?.soeker?.opplysning

  const dispatch = useAppDispatch()

  const [oppdaterSjekklisteResult, oppdaterSjekklisteApi] = useApiCall(oppdaterSjekkliste)
  const sjekkliste = useSjekkliste()

  const sjekklisteValideringsfeil = useSjekklisteValideringsfeil().length > 0

  const configContext = useContext(ConfigContext)

  const rutinerBP =
    'https://navno.sharepoint.com/:u:/r/sites/fag-og-ytelser-pensjon-gjenlevendepensjon/SitePages/Rutine-for-sjekkliste-p%C3%A5-f%C3%B8rstegangsbehandling-Barnepensjon.aspx?csf=1&web=1&e=CZOas4'
  const rutinerOMS =
    'https://navno.sharepoint.com/:u:/r/sites/fag-og-ytelser-pensjon-gjenlevendepensjon/SitePages/Rutine-for-sjekkliste-p%C3%A5-f%C3%B8rstegangsbehandling-Omstillingsst%C3%B8nad.aspx?csf=1&web=1&e=wqh9pu'

  const dispatchUpdatedItem = (item: ISjekklisteItem) => {
    dispatch(updateSjekklisteItem(item))
  }

  const fireOpppdater = useMemo(() => debounce(oppdaterSjekklisteApi, 1500), [])

  useEffect(() => {
    const erSammeIdent = oppgave?.saksbehandler?.ident === innloggetSaksbehandler.ident
    setOppgaveErTildeltInnloggetBruker(erSammeIdent)
    setRedigerbar(
      behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter) &&
        erSammeIdent
    )
  }, [])

  const erBarnepensjon = behandling.sakType == SakType.BARNEPENSJON
  return (
    <SidebarPanel $border id="sjekklistePanel">
      {sjekklisteValideringsfeil && (
        <Alert variant="error">Før du kan sende til attestering må du bekrefte at alle punkter er gjennomgått</Alert>
      )}

      <Heading spacing size="small">
        Sjekkliste
      </Heading>

      {isFailureHandler({
        apiResult: oppdaterSjekklisteResult,
        errorMessage: 'Oppdateringen av sjekklista feilet',
      })}

      {sjekkliste && (
        <>
          <BodyLong>
            Gjennomgå alle punktene og marker de som krever handling.
            {/* Rutiner for revurdering er ikke laget enda */}
            {behandling.behandlingType !== IBehandlingsType.REVURDERING && (
              <Link href={erBarnepensjon ? rutinerBP : rutinerOMS} target="_blank">
                Her finner du rutine til punktene.
              </Link>
            )}
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
            <VStack gap="space-4">
              <Heading size="small">Lenker</Heading>
              {soeker?.foedselsnummer ? (
                <Link href={`${configContext['gosysUrl']}/personoversikt/fnr=${soeker.foedselsnummer}`} target="_blank">
                  Personoversikt i Gosys
                </Link>
              ) : (
                <Alert variant="warning">Mangler fødselsnummer på søker</Alert>
              )}
              {erBarnepensjon && (
                <Link href={configContext['bisysUrl']} target="_blank">
                  Bisys <ExternalLinkIcon aria-hidden />
                </Link>
              )}
            </VStack>
          </HMargin>

          <VStack gap="space-4">
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

            {behandling.behandlingType !== IBehandlingsType.REVURDERING && (
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
            )}

            {behandling.sakType == SakType.BARNEPENSJON &&
              behandling.behandlingType !== IBehandlingsType.REVURDERING && (
                <TextField
                  label="Ønsket skattetrekk"
                  name="OnsketSkattetrekk"
                  value={sjekkliste.onsketSkattetrekk || ''}
                  onChange={(e) => {
                    const oppdatert = {
                      ...sjekkliste,
                      onsketSkattetrekk: e.target.value,
                    }
                    dispatch(updateSjekkliste(oppdatert))
                    fireOpppdater(oppdatert)
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
            <Button variant="tertiary" icon={<PencilIcon aria-hidden />} onClick={() => setRedigerbar(true)}>
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
      {isFailureHandler({
        apiResult: itemUpdateResult,
        errorMessage: 'En feil oppsto ved oppdatering av sjekklista',
      })}
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
  margin-top: 2em;
  margin-bottom: 2em;
`
