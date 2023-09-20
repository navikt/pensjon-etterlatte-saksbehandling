package rapidsandrivers

fun getRapidEnv(): Map<String, String> {
    return System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
}
