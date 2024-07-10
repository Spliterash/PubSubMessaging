rootProject.name = "pub-sub-messaging"

include(
    "publish",

    "core",
    "types:binary",
    "types:binary:mapper:serializable",
    "types:binary:mapper:kryo",
    "types:binary:mapper:json",

    "types:multipaper",
    "types:multipaper:base",

    "types:redisson",
    "types:jedis",
    "types:lettuce",
)