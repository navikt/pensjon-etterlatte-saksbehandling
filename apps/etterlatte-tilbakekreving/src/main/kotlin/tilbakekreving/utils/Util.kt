package no.nav.etterlatte.tilbakekreving.utils

import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.toLocalDate() = this.toGregorianCalendar().toZonedDateTime().toLocalDate()