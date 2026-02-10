import React, { ReactNode, useState } from 'react'
import { addMonths, addYears } from 'date-fns'
import { HStack, Timeline, ToggleGroup, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { AktivitetspliktType, IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { AktivitetHendelseTimelinePinContent } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetHendelseTimelinePinContent'
import { AktivitetspliktRedigeringModus } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { AktivitetspliktPeriodeTimelinePeriodContent } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktPeriodeTimelinePeriodContent'
import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'

interface Props {
  doedsdato: Date
  aktivitetHendelser: IAktivitetHendelse[]
  setAktivitetHendelser: (aktivitetHendelser: IAktivitetHendelse[]) => void
  aktivitetPerioder: IAktivitetPeriode[]
  setAktivitetPerioder: (aktivitetPerioder: IAktivitetPeriode[]) => void
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetspliktTimeline = ({
  doedsdato,
  aktivitetHendelser,
  setAktivitetHendelser,
  aktivitetPerioder,
  setAktivitetPerioder,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
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
                setAktivitetPerioder={setAktivitetPerioder}
                setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
              />
            </Timeline.Period>
          </Timeline.Row>
        )
    }
  }

  return (
    <VStack gap="space-4">
      <HStack justify="end">
        <ToggleGroup
          data-color="neutral"
          defaultValue="3"
          onChange={(value) => setSluttdato(addYears(doedsdato, Number(value)))}
          size="small"
        >
          <ToggleGroup.Item value="1">1 år</ToggleGroup.Item>
          <ToggleGroup.Item value="2">2 år</ToggleGroup.Item>
          <ToggleGroup.Item value="3">3 år</ToggleGroup.Item>
        </ToggleGroup>
      </HStack>
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
              setAktivitetHendelser={setAktivitetHendelser}
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
    </VStack>
  )
}
