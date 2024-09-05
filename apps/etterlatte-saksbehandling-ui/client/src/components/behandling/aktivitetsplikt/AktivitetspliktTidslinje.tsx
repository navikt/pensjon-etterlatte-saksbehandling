import { Buildings2Icon, HatSchoolIcon, PencilIcon, PersonIcon, RulerIcon } from '@navikt/aksel-icons'
import { Alert, HStack, Timeline, ToggleGroup, VStack } from '@navikt/ds-react'
import {
  hentAktiviteterForBehandling,
  hentAktiviteterForSak,
  slettAktivitet,
  slettAktivitetForSak,
} from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears } from 'date-fns'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitet } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { NyAktivitet } from '~components/behandling/aktivitetsplikt/NyAktivitet'
import styled from 'styled-components'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

export const AktivitetspliktTidslinje = (props: {
  behandling?: IDetaljertBehandling
  doedsdato: Date
  sakId?: number
}) => {
  const { behandling, doedsdato, sakId } = props
  const [hentet, hent] = useApiCall(hentAktiviteterForBehandling)
  const [hentetForSak, hentForSak] = useApiCall(hentAktiviteterForSak)
  const [slettet, slett] = useApiCall(slettAktivitet)
  const [slettetForSak, slettForSak] = useApiCall(slettAktivitetForSak)
  const seksMndEtterDoedsfall = addMonths(doedsdato, 6)
  const tolvMndEtterDoedsfall = addMonths(doedsdato, 12)

  const [aktiviteter, setAktiviteter] = useState<IAktivitet[]>([])
  const [rediger, setRediger] = useState<IAktivitet | undefined>(undefined)
  const [aktivitetsTypeProps, setAktivitetsTypeProps] = useState<AktivitetstypeProps[]>([])
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

  useEffect(() => {
    if (behandling) {
      hent({ behandlingId: behandling.id }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    } else if (sakId) {
      hentForSak({ sakId: sakId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    }
  }, [])

  const oppdaterAktiviteter = (aktiviteter: IAktivitet[]) => {
    setAktivitetsTypeProps([...new Set(aktiviteter.map((a) => a.type))].map(mapAktivitetstypeProps))
    setAktiviteter(aktiviteter)
  }

  const fjernAktivitet = (aktivitetId: string) => {
    if (behandling) {
      slett({ behandlingId: behandling.id, aktivitetId: aktivitetId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    } else if (sakId) {
      slettForSak({ sakId: sakId, aktivitetId: aktivitetId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    }
  }

  return (
    <VStack gap="8" className="min-w-[800px]">
      {aktiviteter.length === 0 ? (
        <Alert variant="info" inline>
          Ingen aktiviteter er registrert.
        </Alert>
      ) : (
        <Timeline startDate={doedsdato} endDate={sluttdato}>
          <Timeline.Pin date={doedsdato}>
            <p>Dødsdato: {formaterDato(doedsdato)}</p>
          </Timeline.Pin>
          <Timeline.Pin date={new Date()}>
            <p>Dagens dato: {formaterDato(new Date())}</p>
          </Timeline.Pin>
          <Timeline.Pin date={seksMndEtterDoedsfall}>
            <p>6 måneder etter dødsfall: {formaterDato(seksMndEtterDoedsfall)}</p>
          </Timeline.Pin>
          <Timeline.Pin date={tolvMndEtterDoedsfall}>
            <p>12 måneder etter dødsfall: {formaterDato(tolvMndEtterDoedsfall)}</p>
          </Timeline.Pin>

          {aktivitetsTypeProps.map((props) => (
            <Timeline.Row key={props.type} label={props.beskrivelse}>
              {aktiviteter
                .filter((aktivitet) => aktivitet.type === props.type)
                .map((aktivitet, i) => (
                  <Timeline.Period
                    key={props.type + i}
                    start={new Date(aktivitet.fom)}
                    end={(aktivitet.tom && new Date(aktivitet.tom)) || addYears(doedsdato, 3)}
                    status={props.status}
                    icon={props.ikon}
                    statusLabel={props.beskrivelse}
                  >
                    <p>
                      <b>
                        Fra {formaterDato(new Date(aktivitet.fom))}{' '}
                        {aktivitet.tom && `til ${formaterDato(new Date(aktivitet.tom))}`}
                      </b>
                    </p>
                    <p>{aktivitet.beskrivelse}</p>
                    <p>
                      <i>
                        Lagt til {formaterDatoMedTidspunkt(new Date(aktivitet.opprettet.tidspunkt))} av{' '}
                        {aktivitet.opprettet.ident}
                      </i>
                      <br />
                      <i>
                        Sist endret {formaterDatoMedTidspunkt(new Date(aktivitet.endret.tidspunkt))} av{' '}
                        {aktivitet.endret.ident}
                      </i>
                    </p>
                    {isPending(slettet) || isPending(slettetForSak) ? (
                      <Spinner variant="neutral" label="Sletter" margin="1em" />
                    ) : (
                      <>
                        <SlettEndreWrapper onClick={() => fjernAktivitet(aktivitet.id)}>Slett</SlettEndreWrapper>
                        <SlettEndreWrapper onClick={() => setRediger(aktivitet)}>Endre</SlettEndreWrapper>
                      </>
                    )}
                    {isFailureHandler({
                      apiResult: slettet,
                      errorMessage: 'En feil har oppstått',
                    })}
                  </Timeline.Period>
                ))}
            </Timeline.Row>
          ))}
        </Timeline>
      )}

      <HStack align="center" justify="space-between">
        <NyAktivitet
          key={rediger?.id}
          behandling={behandling}
          oppdaterAktiviteter={oppdaterAktiviteter}
          redigerAktivitet={rediger}
          sakId={sakId}
        />

        {aktiviteter.length > 0 && (
          <ToggleGroup
            defaultValue="3"
            onChange={(value) => setSluttdato(addYears(doedsdato, Number(value)))}
            size="small"
            variant="neutral"
          >
            <ToggleGroup.Item value="1">1 år</ToggleGroup.Item>
            <ToggleGroup.Item value="2">2 år</ToggleGroup.Item>
            <ToggleGroup.Item value="3">3 år</ToggleGroup.Item>
          </ToggleGroup>
        )}
      </HStack>

      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av aktiviteter',
        apiResult: hentet || hentetForSak,
      })}
    </VStack>
  )
}

interface AktivitetstypeProps {
  type: AktivitetspliktType
  beskrivelse: string
  ikon: JSX.Element
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

export const mapAktivitetstypeProps = (type: AktivitetspliktType): AktivitetstypeProps => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return {
        type: AktivitetspliktType.ARBEIDSTAKER,
        beskrivelse: 'Arbeidstaker',
        ikon: <PersonIcon aria-hidden />,
        status: 'success',
      }
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return {
        type: AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE,
        beskrivelse: 'Selvstendig næringsdrivende',
        ikon: <RulerIcon aria-hidden />,
        status: 'info',
      }
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return {
        type: AktivitetspliktType.ETABLERER_VIRKSOMHET,
        beskrivelse: 'Etablerer virksomhet',
        ikon: <Buildings2Icon aria-hidden />,
        status: 'danger',
      }
    case AktivitetspliktType.ARBEIDSSOEKER:
      return {
        type: AktivitetspliktType.ARBEIDSSOEKER,
        beskrivelse: 'Arbeidssøker',
        ikon: <PencilIcon aria-hidden />,
        status: 'warning',
      }
    case AktivitetspliktType.UTDANNING:
      return {
        type: AktivitetspliktType.UTDANNING,
        beskrivelse: 'Utdanning',
        ikon: <HatSchoolIcon aria-hidden />,
        status: 'neutral',
      }
  }
}

const SlettEndreWrapper = styled.div`
  padding-right: 1em;
  display: inline-flex;
  float: left;
  cursor: pointer;
  color: #0067c5;

  &:hover {
    text-decoration-line: underline;
  }
`
