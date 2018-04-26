task = dict(
    travel_counter = dict(
        LEASE_TASK_COUNT = 1000,
        LEASE_TASK_TIME_SEC = 20,
    ),
    billing_accumulator = dict(
        LEASE_TASK_COUNT = 1000,
        LEASE_TASK_TIME_SEC = 100,
    ),
)

store = dict(
    MEMCACHE_BILLING_EXPIRATION_SEC = 120,
    MEMCACHE_STATS_EXPIRATION_SEC = 60*60
    DEFAULT_PAGE_SIZE = 10
)