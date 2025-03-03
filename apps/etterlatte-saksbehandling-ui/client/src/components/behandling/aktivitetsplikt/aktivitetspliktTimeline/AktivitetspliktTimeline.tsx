import React, { ReactNode, useEffect, useState } from 'react'
import { addMonths, addYears } from 'date-fns'
import { HStack, Timeline, ToggleGroup, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { AktivitetspliktType, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { AktivitetHendelseTimelinePinContent } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetHendelseTimelinePinContent'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AktivitetspliktRedigeringModus } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { AktivitetspliktPeriodeTimelinePeriodContent } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetspliktPeriodeTimelinePeriodContent'
import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktiviteterOgHendelser } from '~shared/api/aktivitetsplikt'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

interface Props {
  behandling?: IDetaljertBehandling
  sakId: number
  doedsdato: Date
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetspliktTimeline = ({ behandling, sakId, doedsdato, setAktivitetspliktRedigeringModus }: Props) => {
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

  const [aktivitetOgHendelserResult, aktiviteterOgHendelserFetch] = useApiCall(hentAktiviteterOgHendelser)

  const velgVisningAvAktivitetspliktPeriodeTimelineRow = (aktivitetPeriode: IAktivitetPeriode): ReactNode => {
    switch (aktivitetPeriode.type) {
      case AktivitetspliktType.ARBEIDSTAKER:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Arbeidstaker">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="success"
              statusLabel="Arbeidstaker"
              icon={<PersonIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Selvstendig næringsdrivende">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="info"
              statusLabel="Selvstendig næringsdrivende"
              icon={<RulerIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.ETABLERER_VIRKSOMHET:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Etablert virksomhet">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              icon={<Buildings2Icon aria-hidden />}
              status="danger"
              statusLabel="Etablert virksomhet"
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.ARBEIDSSOEKER:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Arbeidssøker">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="warning"
              statusLabel="Arbeidssøker"
              icon={<PencilIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.UTDANNING:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Utdanning">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="neutral"
              statusLabel="Utdanning"
              icon={<HatSchoolIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.OPPFOELGING_LOKALKONTOR:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Oppfølging lokalkontor">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="warning"
              statusLabel="Oppfølging lokalkontor"
              icon={<ReceptionIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
      case AktivitetspliktType.INGEN_AKTIVITET:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Ingen aktivitet">
            <Timeline.Period
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="neutral"
              statusLabel="Ingen aktivitet"
              icon={<WaitingRoomIcon aria-hidden />}
            >
              <AktivitetspliktPeriodeTimelinePeriodContent
                behandlingId={aktivitetPeriode.behandlingId}
                sakId={aktivitetPeriode.sakId}
                aktivitetPeriode={aktivitetPeriode}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
    }
  }

  useEffect(() => {
    aktiviteterOgHendelserFetch({ sakId, behandlingId: behandling?.id })
  }, [])

  return (
    <VStack gap="4">
      <HStack justify="end">
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
      </HStack>
      {mapResult(aktivitetOgHendelserResult, {
        pending: <Spinner label="Henter aktivitet og hendelser for aktivitetsplikt" />,
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente aktiviteter og hendelser'}</ApiErrorAlert>,
        success: ({ perioder: aktivitetPerioder, hendelser: aktivitetHendelser }) => (
          <Timeline startDate={doedsdato} endDate={sluttdato}>
            <Timeline.Pin date={doedsdato}>Dødsdato: {formaterDato(doedsdato)}</Timeline.Pin>
            <Timeline.Pin date={new Date()}>Dagens dato: {formaterDato(new Date())}</Timeline.Pin>
            <Timeline.Pin date={addMonths(doedsdato, 6)}>
              6 måneder etter dødsfall: {formaterDato(addMonths(doedsdato, 6))}
            </Timeline.Pin>
            <Timeline.Pin date={addMonths(doedsdato, 12)}>
              12 måneder etter dødsfall: {formaterDato(addMonths(doedsdato, 12))}
            </Timeline.Pin>
            {aktivitetHendelser.map((aktivitetHendelse, index) => (
              <Timeline.Pin key={index} date={new Date(aktivitetHendelse.dato)}>
                <AktivitetHendelseTimelinePinContent
                  sakId={aktivitetHendelse.sakId}
                  behandlingId={aktivitetHendelse.behandlingId}
                  aktivitetHendelse={aktivitetHendelse}
                  setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
                />
              </Timeline.Pin>
            ))}
            {!!aktivitetPerioder?.length ? (
              aktivitetPerioder.map(velgVisningAvAktivitetspliktPeriodeTimelineRow)
            ) : (
              <Timeline.Row label="Ingen aktiviteter">
                <Timeline.Period start={addYears(doedsdato, -1)} end={addYears(doedsdato, -1)}></Timeline.Period>
              </Timeline.Row>
            )}
          </Timeline>
        ),
      })}
    </VStack>
  )
}
