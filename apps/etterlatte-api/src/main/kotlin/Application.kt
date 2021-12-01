package no.nav.etterlatte


class ApplicationContext() {

}


fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
