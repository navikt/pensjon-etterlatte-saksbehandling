package no.nav.etterlatte.metrics

import io.prometheus.client.Gauge
import org.slf4j.LoggerFactory

class BehandlingMetrics(private val behandlingerMetrikkerDao: BehandlingMetrikkerDao) : MetrikkUthenter() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val underBehandling by lazy {
        Gauge.build("antall_behandling_under_behandling", "Antall behandlinger").register()
    }
    val fattet by lazy {
        Gauge.build("antall_behandling_fattet", "Antall ").register()
    }
    val attestert by lazy {
        Gauge.build("antall_behandling_under_behandling", "Antall behandlinger").register()
    }
    val iverksatt by lazy {
        Gauge.build("antall_behandling_under_behandling", "Antall behandlinger").register()
    }

    val migrertManUnderBehandling by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_man", "Antall behandlinger").register()
    }
    val migrertManFattet by lazy {
        Gauge.build("antall_behandling_fattet_migr_man", "Antall behandlinger").register()
    }
    val migrertManAttestert by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_man", "Antall behandlinger").register()
    }
    val migrertManIverksatt by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_man", "Antall behandlinger").register()
    }

    val migrertAutoUnderBehandling by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_auto", "Antall behandlinger").register()
    }
    val migrertAutoFattet by lazy {
        Gauge.build("antall_behandling_fattet_migr_auto", "Antall behandlinger").register()
    }
    val migrertAutoAttestert by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_auto", "Antall behandlinger").register()
    }
    val migrertAutoIverksatt by lazy {
        Gauge.build("antall_behandling_under_behandling_migr_auto", "Antall behandlinger").register()
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")
        val behandlinger = behandlingerMetrikkerDao.hentBehandlingsstatusAntall()
        behandlinger[BehandlingMetrikkVariant.NY_GJENNY]?.let {
            underBehandling.set(it.underBehandling.toDouble())
            fattet.set(it.fattet.toDouble())
            attestert.set(it.attestert.toDouble())
            iverksatt.set(it.iverksatt.toDouble())
        }
        behandlinger[BehandlingMetrikkVariant.MANUELT_FRA_PESYS]?.let {
            migrertManUnderBehandling.set(it.underBehandling.toDouble())
            migrertManFattet.set(it.fattet.toDouble())
            migrertManAttestert.set(it.attestert.toDouble())
            migrertManIverksatt.set(it.iverksatt.toDouble())
        }
        behandlinger[BehandlingMetrikkVariant.AUTOMATISK_FRA_PESYS]?.let {
            migrertAutoUnderBehandling.set(it.underBehandling.toDouble())
            migrertAutoFattet.set(it.fattet.toDouble())
            migrertAutoAttestert.set(it.attestert.toDouble())
            migrertAutoIverksatt.set(it.iverksatt.toDouble())
        }
    }
}
