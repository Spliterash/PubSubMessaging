rootProject.name = "pub-sub-messaging"

include(
    "publish",

    "core",
    "types:binary",
    "types:binary:mapper:serializable",
    "types:binary:mapper:kryo",

    "types:multipaper",
    "types:multipaper:base",

    "types:redisson",
    "types:jedis"
)