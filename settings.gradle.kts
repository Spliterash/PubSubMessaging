rootProject.name = "pub-sub-service-messaging"

include(
    "publish",

    "core",
    "types:binary",
    "types:binary:mapper:serializable",

    "types:multipaper",
    "types:multipaper:base",

    "types:redisson"
)