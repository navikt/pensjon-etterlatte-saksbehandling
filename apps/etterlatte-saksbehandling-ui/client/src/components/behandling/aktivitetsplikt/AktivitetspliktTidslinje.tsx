import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  TrashIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'
import { BodyShort, Button, HStack, Timeline, ToggleGroup, VStack } from '@navikt/ds-react'
import {
  hentAktiviteterOgHendelserForBehandling,
  hentAktiviteterOgHendelserForSak,
  slettAktivitet,
  slettAktivitetForSak,
  slettAktivitetHendelse,
  slettAktivitetHendelseForSak,
} from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears } from 'date-fns'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { ReactNode, useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { NyAktivitetHendelse } from '~components/behandling/aktivitetsplikt/AktivitetOgHendelse'

interface Props {
  behandling?: IDetaljertBehandling
  doedsdato: Date
  sakId: number
}

export const AktivitetspliktTidslinje = ({ behandling, doedsdato, sakId }: Props) => {
  const [hentet, hent] = useApiCall(hentAktiviteterOgHendelserForBehandling)
  const [hentetForSak, hentForSak] = useApiCall(hentAktiviteterOgHendelserForSak)
  const [slettet, slett] = useApiCall(slettAktivitet)
  const [slettetHendelse, slettHendelse] = useApiCall(slettAktivitetHendelse)
  const [slettetForSak, slettForSak] = useApiCall(slettAktivitetForSak)
  const [slettetHendelseForSak, slettHendelseForSak] = useApiCall(slettAktivitetHendelseForSak)
  const seksMndEtterDoedsfall = addMonths(doedsdato, 6)
  const tolvMndEtterDoedsfall = addMonths(doedsdato, 12)
  const [aktiviteter, setAktiviteter] = useState<IAktivitetPeriode[]>([])
  const [hendelser, setHendelser] = useState<IAktivitetHendelse[]>([])
  const [rediger, setRediger] = useState<IAktivitetPeriode | undefined>(undefined)
  const [redigerHendelse, setRedigerHendelse] = useState<IAktivitetHendelse | undefined>(undefined)
  const [aktivitetsTypeProps, setAktivitetsTypeProps] = useState<AktivitetstypeProps[]>([])
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

  useEffect(() => {
    if (behandling) {
      hent({ behandlingId: behandling.id }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter.perioder)
        setHendelser(aktiviteter.hendelser)
      })
    } else {
      hentForSak({ sakId: sakId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter.perioder)
        setHendelser(aktiviteter.hendelser)
      })
    }
  }, [behandling, sakId])

  const oppdaterAktiviteter = (aktiviteter: IAktivitetPeriode[]) => {
    setAktivitetsTypeProps([...new Set(aktiviteter.map((a) => a.type))].map(mapAktivitetstypeProps))
    setAktiviteter(aktiviteter)
  }

  const fjernAktivitet = (aktivitetId: string) => {
    if (behandling) {
      slett({ behandlingId: behandling.id, aktivitetId: aktivitetId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    } else {
      slettForSak({ sakId: sakId, aktivitetId: aktivitetId }, (aktiviteter) => {
        oppdaterAktiviteter(aktiviteter)
      })
    }
  }

  const fjernHendelse = (hendelseId: string) => {
    if (behandling) {
      slettHendelse({ behandlingId: behandling.id, hendelseId: hendelseId }, (hendelser) => {
        setHendelser(hendelser)
      })
    } else {
      slettHendelseForSak({ sakId: sakId, hendelseId: hendelseId }, (hendelser) => {
        setHendelser(hendelser)
      })
    }
  }

  function avbrytRedigering() {
    setRediger(undefined)
    setRedigerHendelse(undefined)
  }

  return (
    <VStack gap="8" className="min-w-[800px]">
      <Timeline startDate={doedsdato} endDate={sluttdato}>
        <Timeline.Pin date={doedsdato}>
          <BodyShort>Dødsdato: {formaterDato(doedsdato)}</BodyShort>
        </Timeline.Pin>
        <Timeline.Pin date={new Date()}>
          <BodyShort>Dagens dato: {formaterDato(new Date())}</BodyShort>
        </Timeline.Pin>
        <Timeline.Pin date={seksMndEtterDoedsfall}>
          <BodyShort>6 måneder etter dødsfall: {formaterDato(seksMndEtterDoedsfall)}</BodyShort>
        </Timeline.Pin>
        <Timeline.Pin date={tolvMndEtterDoedsfall}>
          <BodyShort>12 måneder etter dødsfall: {formaterDato(tolvMndEtterDoedsfall)}</BodyShort>
        </Timeline.Pin>
        {hendelser.map((hendelse) => (
          <Timeline.Pin key={hendelse.id} date={new Date(hendelse.dato)}>
            <BodyShort>{hendelse.beskrivelse}</BodyShort>
            <VStack>
              <BodyShort>
                <i>
                  Lagt til {formaterDatoMedTidspunkt(new Date(hendelse.opprettet.tidspunkt))} av{' '}
                  {hendelse.opprettet.ident}
                </i>
              </BodyShort>
              <BodyShort>
                <i>
                  Sist endret {formaterDatoMedTidspunkt(new Date(hendelse.endret.tidspunkt))} av {hendelse.endret.ident}
                </i>
              </BodyShort>
            </VStack>
            {isPending(slettetHendelse) || isPending(slettetHendelseForSak) ? (
              <Spinner variant="neutral" label="Sletter" margin="1em" />
            ) : (
              <HStack gap="2">
                <Button
                  variant="secondary"
                  size="xsmall"
                  icon={<PencilIcon aria-hidden />}
                  onClick={() => setRedigerHendelse(hendelse)}
                >
                  Rediger
                </Button>
                <Button
                  variant="secondary"
                  size="xsmall"
                  icon={<TrashIcon aria-hidden />}
                  onClick={() => fjernHendelse(hendelse.id)}
                >
                  Slett
                </Button>
              </HStack>
            )}
            {isFailureHandler({
              apiResult: slettetHendelse,
              errorMessage: 'En feil har oppstått',
            })}
          </Timeline.Pin>
        ))}
        {aktiviteter.length === 0 && (
          <Timeline.Row label="Ingen aktiviteter">
            <Timeline.Period start={addYears(doedsdato, -1)} end={addYears(doedsdato, -1)}></Timeline.Period>
          </Timeline.Row>
        )}

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
                  <BodyShort weight="semibold">
                    Fra {formaterDato(new Date(aktivitet.fom))}{' '}
                    {aktivitet.tom && `til ${formaterDato(new Date(aktivitet.tom))}`}
                  </BodyShort>
                  <BodyShort>{aktivitet.beskrivelse}</BodyShort>
                  <VStack>
                    <BodyShort>
                      <i>
                        Lagt til {formaterDatoMedTidspunkt(new Date(aktivitet.opprettet.tidspunkt))} av{' '}
                        {aktivitet.opprettet.ident}
                      </i>
                    </BodyShort>
                    <BodyShort>
                      <i>
                        Sist endret {formaterDatoMedTidspunkt(new Date(aktivitet.endret.tidspunkt))} av{' '}
                        {aktivitet.endret.ident}
                      </i>
                    </BodyShort>
                  </VStack>
                  {isPending(slettet) || isPending(slettetForSak) ? (
                    <Spinner variant="neutral" label="Sletter" margin="1em" />
                  ) : (
                    <HStack gap="2">
                      <Button
                        variant="secondary"
                        size="xsmall"
                        icon={<PencilIcon aria-hidden />}
                        onClick={() => setRediger(aktivitet)}
                      >
                        Rediger
                      </Button>
                      <Button
                        variant="secondary"
                        size="xsmall"
                        icon={<TrashIcon aria-hidden />}
                        onClick={() => fjernAktivitet(aktivitet.id)}
                      >
                        Slett
                      </Button>
                    </HStack>
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

      <HStack align="center" justify="space-between">
        <NyAktivitetHendelse
          key={rediger?.id}
          behandling={behandling}
          oppdaterAktiviteter={oppdaterAktiviteter}
          redigerAktivitet={rediger}
          sakId={sakId}
          setHendelser={setHendelser}
          redigerHendelse={redigerHendelse}
          avbrytRedigering={avbrytRedigering}
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
        errorMessage: 'En feil oppsto ved henting av tidslinje',
        apiResult: hentet,
      })}
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av tidslinje',
        apiResult: hentetForSak,
      })}
    </VStack>
  )
}

interface AktivitetstypeProps {
  type: AktivitetspliktType
  beskrivelse: string
  ikon: ReactNode
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
    case AktivitetspliktType.INGEN_AKTIVITET:
      return {
        type: AktivitetspliktType.INGEN_AKTIVITET,
        beskrivelse: 'Ingen Aktivitet',
        ikon: <WaitingRoomIcon aria-hidden />,
        status: 'neutral',
      }
    case AktivitetspliktType.OPPFOELGING_LOKALKONTOR:
      return {
        type: AktivitetspliktType.OPPFOELGING_LOKALKONTOR,
        beskrivelse: 'Oppfølging lokalkontor',
        ikon: <ReceptionIcon aria-hidden />,
        status: 'warning',
      }
  }
}
